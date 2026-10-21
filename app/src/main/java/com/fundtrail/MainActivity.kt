package com.fundtrail

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.fundtrail.ui.navigation.Route
import com.fundtrail.ui.theme.FundtrailAppTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                FundtrailNavHost(start, isOnline)
                            }
                            if (lock.locked) {
                                AppLockScreen(onUnlock = ::showUnlockPrompt)
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

    /** Fingerprint or face, with the phone's PIN, pattern or password as the fallback (#222). */
    private fun showUnlockPrompt() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockViewModel.onUnlocked()
                }
                // Cancel or error: stay locked; the lock screen's Unlock button opens the prompt again.
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_prompt_title))
                .setAllowedAuthenticators(APP_LOCK_AUTHENTICATORS)
                .build()
        )
    }
}
