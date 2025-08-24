package org.cap.gold.platform

import android.content.Context

// This object will hold the Android application context
internal object AndroidContextHolder {
    var context: Context? = null
}

/**
 * Sets the Android context for the platform module
 */
fun setAndroidContext(context: Context?) {
    AndroidContextHolder.context = context?.applicationContext
}

/**
 * Gets the Android application context
 * @throws IllegalStateException if the context is not set
 */
internal val androidContext: Context
    get() = AndroidContextHolder.context ?: throw IllegalStateException(
        "Android context not set. Call setAndroidContext() in your Application class."
    )
