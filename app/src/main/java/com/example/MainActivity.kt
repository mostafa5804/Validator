package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.CityRepository
import com.example.data.history.AppDatabase
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val systemTheme = isSystemInDarkTheme()
      var isDarkTheme by remember { mutableStateOf(systemTheme) }
      
      MyApplicationTheme(darkTheme = isDarkTheme) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val context = LocalContext.current
          val cityRepository = remember { CityRepository(context) }
          val database = remember { AppDatabase.getDatabase(context) }
          
          androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
              MainScreen(
                  cityRepository = cityRepository, 
                  historyDao = database.historyDao(),
                  isDarkMode = isDarkTheme,
                  onThemeToggle = { isDarkTheme = !isDarkTheme }
              )
          }
        }
      }
    }
  }
}

