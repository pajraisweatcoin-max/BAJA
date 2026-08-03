package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.barracloud.BarraCloudApplication
import com.example.barracloud.data.local.AppThemeMode
import com.example.barracloud.ui.MainContainer
import com.example.barracloud.ui.MainViewModel
import com.example.ui.theme.BarraCloudTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(BarraCloudApplication.instance.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            BarraCloudTheme(darkTheme = isDarkTheme) {
                MainContainer(viewModel = viewModel)
            }
        }
    }
}

