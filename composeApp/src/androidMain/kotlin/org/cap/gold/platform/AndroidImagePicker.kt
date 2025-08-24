package org.cap.gold.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("images")
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    ).apply { parentFile?.mkdirs() }
}

@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray, String) -> Unit,
    onError: (Exception) -> Unit
): ImagePicker {
    val context = LocalContext.current
    var tempFile by remember { mutableStateOf<File?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data ?: tempFile?.let { Uri.fromFile(it) }
            if (imageUri != null) {
                try {
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        val bytes = input.readBytes()
                        val fileName = "img_${System.currentTimeMillis()}.jpg"
                        onImagePicked(bytes, fileName)
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            } else {
                onError(Exception("Failed to capture image"))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Relaunch after permission
            val file = createImageFile(context).also { tempFile = it }
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                )
            }
            imagePickerLauncher.launch(intent)
        } else {
            onError(Exception("Camera permission denied"))
        }
    }

    return remember {
        object : ImagePicker {
            override fun pickImage() {
                if (hasCameraPermission(context)) {
                    val file = createImageFile(context).also { tempFile = it }
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        )
                    }
                    imagePickerLauncher.launch(intent)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}
