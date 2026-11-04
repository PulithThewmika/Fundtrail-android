package com.fundtrail

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fundtrail.security.APP_LOCK_AUTHENTICATORS
import com.fundtrail.ui.MainViewModel
import com.fundtrail.ui.lock.AppLockScreen
import com.fundtrail.ui.lock.AppLockViewModel
import com.fundtrail.ui.navigation.FundtrailNavHost
import com.fundtrail.ui.navigation.LaunchRequests
import com.fundtrail.ui.navigation.Route
import com.fundtrail.ui.theme.FundtrailAppTheme
import com.fundtrail.widget.QuickAddLink.wantsQuickAdd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** A [FragmentActivity] because `BiometricPrompt` hosts its dialog in a fragment (#222). */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val lockViewModel: AppLockViewModel by viewModels()

    /** Whole-app background/foreground, not this activity's: the prompt and system dialogs don't count as leaving. */
    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) = lockViewModel.onBackgrounded()
        override fun onStart(owner: LifecycleOwner) = lockViewModel.onForegrounded()
    }

    /** The invoice notification asked for a screen; cleared once Income has opened it (#256). */
    private var deepLinkInvoiceId by mutableStateOf<String?>(null)

    /** The widget's "+ Add" asked for the quick-add sheet; cleared once the dashboard has opened it (#228). */
    private var quickAddRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restored when there is saved state, so a request still waiting on sign-in or unlock survives a
        // recreation, and one already handled is not replayed (both were saved as cleared).
        // Saved state wins whenever it exists, even when it holds "cleared". Falling back to the launch intent there
        // would replay a request that was already handled, because that intent still carries it after a recreation.
        if (savedInstanceState != null) {
            deepLinkInvoiceId = savedInstanceState.getString(INVOICE_ID_KEY)
            quickAddRequested = savedInstanceState.getBoolean(QUICK_ADD_KEY)
        } else {
            deepLinkInvoiceId = intent.invoiceId()
            quickAddRequested = intent.wantsQuickAdd()
        }
        enableEdgeToEdge()
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)

        // While the lock is on, hide the app from screenshots and the recent-apps preview (#222).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                lockViewModel.uiState.collect { lock ->
                    if (lock.enabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            FundtrailAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
                    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                    val lock by lockViewModel.uiState.collectAsStateWithLifecycle()
                    // Wait for Firebase to restore the session and for the lock setting, so nothing flashes (FR-06).
                    val initial = signedIn
                    if (initial != null && lock.resolved) {
                        val start = remember { if (initial) Route.MainGraph else Route.AuthGraph }
                        Box(Modifier.fillMaxSize()) {
                            // The app stays composed under the lock so its back stack survives, but TalkBack can't
                            // reach it and the opaque cover hides it.
                            Box(if (lock.locked) Modifier.clearAndSetSemantics { } else Modifier) {
                                // A request that arrives while locked is held, not dropped: the navigation effects
                                // see nothing until the unlock flips `locked`, then this recomposes with it (#222).
                                val requests = LaunchRequests(deepLinkInvoiceId, quickAddRequested)
                                    .releasedWhen(lock.locked)
                                FundtrailNavHost(
                                    start,
                                    isOnline,
                                    requests.invoiceId,
                                    quickAddRequested = requests.quickAdd,
                                    onQuickAddHandled = { quickAddRequested = false },
                                    // Only the request that was handled: a newer link may have arrived meanwhile.
                                    onDeepLinkHandled = { handled ->
                                        if (deepLinkInvoiceId == handled) deepLinkInvoiceId = null
                                    },
                                )
                            }
                            if (lock.locked) {
                                AppLockScreen(onUnlock = ::showUnlockPrompt)
                                // Dialogs and sheets (the invoice form, quick add, ...) are their own windows above this
                                // one, so a cover drawn only here would leave them readable. A window created later sits
                                // above them all, so the lock is drawn in one as well. Declared before the prompt effect
                                // so the system prompt, created after it, stays on top.
                                Dialog(
                                    onDismissRequest = {},
                                    properties = DialogProperties(
                                        dismissOnBackPress = false,
                                        dismissOnClickOutside = false,
                                        usePlatformDefaultWidth = false,
                                        decorFitsSystemWindows = false,
                                    ),
                                ) {
                                    AppLockScreen(onUnlock = ::showUnlockPrompt)
                                }
                                LaunchedEffect(Unit) { showUnlockPrompt() }
                            }
                        }
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

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // The latest tap wins. Each intent is one request or the other, and releasing an invoice and a quick-add
        // together would send the user to Income and then pop the quick-add sheet when they return to Insights. A
        // plain relaunch carries neither, so it leaves a held request alone.
        val invoiceId = intent.invoiceId()
        when {
            invoiceId != null -> {
                deepLinkInvoiceId = invoiceId
                quickAddRequested = false
            }
            intent.wantsQuickAdd() -> {
                quickAddRequested = true
                deepLinkInvoiceId = null
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(INVOICE_ID_KEY, deepLinkInvoiceId)
        outState.putBoolean(QUICK_ADD_KEY, quickAddRequested)
        super.onSaveInstanceState(outState)
    }

    /** Fingerprint or face, with the phone's PIN, pattern or password as the fallback (#222). */
    private fun showUnlockPrompt() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockViewModel.onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Only these two mean the device can no longer authenticate anyone (the screen lock or the
                    // biometrics were removed after the lock was turned on), so the app would stay shut for good.
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL || errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                        lockViewModel.onAuthenticationUnavailable()
                    }
                    // Anything else (cancel, timeout, lockout, hardware busy) is recoverable: stay locked; the lock
                    // screen's Unlock button opens the prompt again.
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_prompt_title))
                .setAllowedAuthenticators(APP_LOCK_AUTHENTICATORS)
                .build()
        )
    }

    private fun Intent.invoiceId(): String? =
        data?.takeIf { it.scheme == "fundtrail" && it.host == "invoice" }?.lastPathSegment

    private companion object {
        const val INVOICE_ID_KEY = "deep_link_invoice_id"
        const val QUICK_ADD_KEY = "quick_add_requested"
    }
}
