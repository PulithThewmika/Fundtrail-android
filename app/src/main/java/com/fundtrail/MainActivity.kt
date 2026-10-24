package com.fundtrail

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.ui.MainViewModel
import com.fundtrail.ui.navigation.FundtrailNavHost
import com.fundtrail.ui.navigation.Route
import com.fundtrail.ui.theme.FundtrailAppTheme
import com.fundtrail.widget.QuickAddLink.wantsQuickAdd
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var deepLinkInvoiceId by mutableStateOf<String?>(null)
    /** The widget's "+ Add" asked for the quick-add sheet; cleared once the dashboard has opened it (#228). */
    private var quickAddRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkInvoiceId = savedInstanceState?.getString(INVOICE_ID_KEY) ?: intent.invoiceId()
        // Only on a fresh launch: a rotation recreates the activity with the same intent and must not reopen it.
        quickAddRequested = savedInstanceState == null && intent.wantsQuickAdd()
        enableEdgeToEdge()
        setContent {
            FundtrailAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
                    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                    // Wait for Firebase to restore the session so neither graph flashes (FR-06).
                    val initial = signedIn
                    if (initial != null) {
                        val start = remember { if (initial) Route.MainGraph else Route.AuthGraph }
                        FundtrailNavHost(
                            start,
                            isOnline,
                            deepLinkInvoiceId,
                            quickAddRequested = quickAddRequested,
                            onQuickAddHandled = { quickAddRequested = false },
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkInvoiceId = intent.invoiceId()
        if (intent.wantsQuickAdd()) quickAddRequested = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(INVOICE_ID_KEY, deepLinkInvoiceId)
        super.onSaveInstanceState(outState)
    }

    private fun Intent.invoiceId(): String? =
        data?.takeIf { it.scheme == "fundtrail" && it.host == "invoice" }?.lastPathSegment

    private companion object {
        const val INVOICE_ID_KEY = "deep_link_invoice_id"
    }
}
