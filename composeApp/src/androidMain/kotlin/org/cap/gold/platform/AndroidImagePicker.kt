package org.cap.gold.platform

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray, String) -> Unit,
    onError: (Exception) -> Unit
): ImagePicker {
    val context = LocalContext.current

    // System Photo Picker (Android 13+)
    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    val fileName = "img_${System.currentTimeMillis()}.jpg"
                    onImagePicked(bytes, fileName)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // Fallback for older Android: simple content picker
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    val fileName = "img_${System.currentTimeMillis()}.jpg"
                    onImagePicked(bytes, fileName)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    return remember {
        object : ImagePicker {
            override fun pickImage() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    getContentLauncher.launch("image/*")
                }
            }
        }
    }
}
