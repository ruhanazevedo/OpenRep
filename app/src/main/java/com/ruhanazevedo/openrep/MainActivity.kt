package com.ruhanazevedo.openrep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruhanazevedo.openrep.data.repository.PreferencesRepository
import com.ruhanazevedo.openrep.ui.theme.WorkoutGeneratorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read once synchronously to get the correct initial theme — avoids light flash
        val initialDarkMode = runBlocking {
            preferencesRepository.preferences.first().isDarkMode
        }

        setContent {
            val prefs by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = null
            )
            val isDarkMode = prefs?.isDarkMode ?: initialDarkMode
            WorkoutGeneratorTheme(darkTheme = isDarkMode) {
                WorkoutGeneratorNavHost()
            }
        }
    }
}
