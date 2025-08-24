package org.cap.gold.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(
    onImagePicked: (ByteArray, String) -> Unit,
    onError: (Exception) -> Unit = {}
): ImagePicker

interface ImagePicker {
    fun pickImage()
}
