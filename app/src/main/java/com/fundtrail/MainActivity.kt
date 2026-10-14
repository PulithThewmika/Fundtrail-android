package com.fundtrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.ui.MainViewModel
import com.fundtrail.ui.navigation.FundtrailNavHost
import com.fundtrail.ui.navigation.Route
import com.fundtrail.ui.theme.FundtrailAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FundtrailAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
                    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                    // Wait for Firebase to restore the session so neither graph flashes (FR-06).
                    signedIn?.let { initial ->
                        val start = remember { if (initial) Route.MainGraph else Route.AuthGraph }
                        FundtrailNavHost(start, isOnline)
                    }
                }
            }
        }
    }
}
