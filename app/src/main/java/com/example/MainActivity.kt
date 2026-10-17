package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.LoginScreen
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.UploaderViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: UploaderViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val isLoggedIn by viewModel.isLoggedIn.collectAsState()

        Surface(modifier = Modifier.fillMaxSize()) {
          if (isLoggedIn) {
            MainScreen(
              viewModel = viewModel,
              onLogout = { viewModel.logout() }
            )
          } else {
            LoginScreen(
              onLoginSuccess = { viewModel.login() }
            )
          }
        }
      }
    }
  }
}
