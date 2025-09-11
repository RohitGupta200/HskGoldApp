package org.cap.gold.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.data
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import kotlinx.cinterop.refTo
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.convert
import platform.Foundation.NSError
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.posix.memcpy

private fun currentViewController(): UIViewController? {
    val keyWindow = UIApplication.sharedApplication.keyWindow
    var root = keyWindow?.rootViewController
    while (root?.presentedViewController != null) root = root?.presentedViewController
    return root
}

@OptIn(ExperimentalForeignApi::class)
private fun nsDataToByteArray(data: NSData): ByteArray {
    val length = data.length.toInt()
    val out = ByteArray(length)
    val src = data.bytes
    if (src != null && length > 0) {
        out.usePinned { dest ->
            memcpy(dest.addressOf(0), src, length.convert())
        }
    }
    return out
}

@OptIn(ExperimentalForeignApi::class)
private fun imageToJpegBytes(image: UIImage, quality: Double = 0.9): ByteArray? {
    val data: NSData? = UIImageJPEGRepresentation(image, quality)
    return data?.let { nsDataToByteArray(it) }
}

private fun defaultFileName(): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyyMMdd_HHmmss"
        // Use default locale
    }
    val stamp = formatter.stringFromDate(NSDate())
    return "image_$stamp.jpg"
}

private class PickerDelegate(
    private val onPicked: (ByteArray, String) -> Unit,
    private val onError: (Exception) -> Unit
) : NSObject(), UINavigationControllerDelegateProtocol {
    fun present() {
        val cfg = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration = cfg)
        picker.setDelegate(object : NSObject(), platform.PhotosUI.PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: kotlin.collections.List<*>) {
                // Dismiss the picker
                picker.dismissViewControllerAnimated(true, completion = null)
                val first = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                val provider = first.itemProvider
                // Load raw data for image type identifier, then convert to JPEG if needed
                provider.loadDataRepresentationForTypeIdentifier(
                    UTTypeImage.identifier,
                    completionHandler = { data: NSData?, error: NSError? ->
                        if (error != null) {
                            onError(Exception(error.localizedDescription ?: "Failed to load image data"))
                            return@loadDataRepresentationForTypeIdentifier
                        }
                        val bytes = data?.let { nsDataToByteArray(it) }
                        if (bytes == null) {
                            onError(Exception("No image data from provider"))
                            return@loadDataRepresentationForTypeIdentifier
                        }
                        val name = provider.suggestedName ?: defaultFileName()
                        onPicked(bytes, if (name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)) name else "$name.jpg")
                    }
                )
            }
        })
        currentViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray, String) -> Unit,
    onError: (Exception) -> Unit
): ImagePicker {
    return remember {
        object : ImagePicker {
            override fun pickImage() {
                try {
                    PickerDelegate(onImagePicked, onError).present()
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }
}
