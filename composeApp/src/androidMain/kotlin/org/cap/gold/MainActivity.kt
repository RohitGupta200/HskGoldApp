package org.cap.gold

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.cap.gold.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Log activity creation to Crashlytics

        // Force light mode regardless of system setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Ensure system bars use light appearance so content stays light
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        // Test Crashlytics integration (REMOVE IN PRODUCTION)
        // Uncomment the following lines to test crash reporting:
//         FirebaseCrashlytics.getInstance().log("Test crash button pressed")
//         throw RuntimeException("Test Crash for Firebase Crashlytics")

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Let framework save basic state, then clear to avoid serializing Voyager Screens
        super.onSaveInstanceState(outState)
        outState.clear()
    }
}