package com.fundtrail.data.remote.source

import android.util.Log
import com.fundtrail.data.remote.dto.CategoryDto
import com.fundtrail.data.remote.dto.toDto
import com.fundtrail.data.remote.dto.toDomain
import com.fundtrail.data.remote.dto.toMap
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Provider

interface CategoryRemoteDataSource {
    fun observeCategories(uid: String): Flow<List<Category>>
    fun addCategory(uid: String, category: Category): Result<String>
    fun updateCategory(uid: String, category: Category): Result<Unit>
    fun deleteCategory(uid: String, categoryId: String): Result<Unit>
}

class CategoryRemoteDataSourceImpl @Inject constructor(
    private val firestoreProvider: Provider<FirebaseFirestore>
) : CategoryRemoteDataSource {

    override fun observeCategories(uid: String): Flow<List<Category>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collectionRef = firestoreProvider.get()
            .collection("users").document(uid).collection("categories")

        val registration = collectionRef.addSnapshotListener(
            MetadataChanges.EXCLUDE
        ) { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "PERMISSION_DENIED for categories uid=$uid (user signed out)")
                    close()
                } else {
                    Log.e(TAG, "Snapshot listener error for categories uid=$uid", error)
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val categories = snapshot.documents.mapNotNull { doc ->
                    try {
                        val dto = doc.toObject(CategoryDto::class.java)
                        dto?.toDomain(doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing CategoryDto for doc ${doc.id}", e)
                        null
                    }
                }
                trySend(categories)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override fun addCategory(uid: String, category: Category): Result<String> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }

            val collection = firestoreProvider.get()
                .collection("users").document(uid).collection("categories")
            val docId = category.id.ifBlank { collection.document().id }
            val docRef = collection.document(docId)

            val validatedCategory = category.copy(id = docId)
            validatedCategory.validate()

            val dto = validatedCategory.toDto()
            val dataMap = dto.toMap(isUpdate = false)

            val task = docRef.set(dataMap)
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for addCategory id=$docId", e)
            }

            docId
        }
    }

    override fun updateCategory(uid: String, category: Category): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            category.validate()

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("categories").document(category.id)

            val dto = category.toDto()
            val dataMap = dto.toMap(isUpdate = true)

            val task = docRef.set(dataMap, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateCategory id=${category.id}", e)
            }
        }
    }

    override fun deleteCategory(uid: String, categoryId: String): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(categoryId.isNotBlank()) { "Category ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("categories").document(categoryId)

            val updates = mapOf(
                "deletedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val task = docRef.set(updates, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for deleteCategory id=$categoryId", e)
            }
        }
    }

    companion object {
        private const val TAG = "CategoryRemoteDS"
    }
}
