package com.fundtrail.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.fundtrail.domain.model.UserProfile
import com.fundtrail.domain.usecase.auth.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(observeUserProfile: ObserveUserProfileUseCase) : ViewModel() {
    val profile: StateFlow<UserProfile?> =
        observeUserProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * The user's photo in a circle. The initial (or a person icon) sits underneath, so it shows while the photo
 * loads, when there is none (email/password accounts), and when it can't be fetched offline.
 */
@Composable
fun Avatar(profile: UserProfile?, size: Dp, modifier: Modifier = Modifier) {
    val initial = (profile?.name ?: profile?.email)?.trim()?.firstOrNull()?.uppercase()
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                initial,
                style = if (size >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        profile?.photoUrl?.let {
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}
