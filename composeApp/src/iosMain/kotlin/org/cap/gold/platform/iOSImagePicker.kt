package org.cap.gold.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
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
import platform.MobileCoreServices.kUTTypeImage
import platform.UniformTypeIdentifiers.UTTypeImage

private fun currentViewController(): UIViewController? {
    val keyWindow = UIApplication.sharedApplication.keyWindow
    var root = keyWindow?.rootViewController
    while (root?.presentedViewController != null) root = root?.presentedViewController
    return root
}

@OptIn(ExperimentalForeignApi::class)
private fun imageToJpegBytes(image: UIImage, quality: Double = 0.9): ByteArray? {
    val data: NSData? = UIImageJPEGRepresentation(image, quality)
    return data?.let { bytes ->
        val length = bytes.length.toInt()
        val buffer = ByteArray(length)
        memScoped {
            bytes.getBytes(buffer.refTo(0), length.toULong())
        }
        buffer
    }
}

private fun defaultFileName(): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyyMMdd_HHmmss"
        locale = NSLocale.currentLocale
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
                // Prefer UIImage type
                val typeId = if (#available(iOS 14.0, *)) UTTypeImage.identifier else kUTTypeImage
                provider.loadObjectOfClass(UIImage.`class`(), completionHandler = { obj, error ->
                    if (error != null) {
                        onError(Exception(error.localizedDescription ?: "Failed to load image"))
                        return@loadObjectOfClass
                    }
                    val uiImage = obj as? UIImage
                    if (uiImage == null) {
                        onError(Exception("No UIImage from provider"))
                        return@loadObjectOfClass
                    }
                    val bytes = imageToJpegBytes(uiImage)
                    if (bytes == null) {
                        onError(Exception("Failed to convert image to JPEG"))
                        return@loadObjectOfClass
                    }
                    val name = provider.suggestedName ?: defaultFileName()
                    onPicked(bytes, if (name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)) name else "$name.jpg")
                })
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
