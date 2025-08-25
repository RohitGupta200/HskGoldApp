package org.cap.gold.auth

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidDeviceTokenProvider : DeviceTokenProvider {
    override suspend fun getDeviceToken(): String? = suspendCancellableCoroutine { cont ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!cont.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        cont.resume(task.result)
                    } else {
                        cont.resume(null)
                    }
                }
        } catch (t: Throwable) {
            if (cont.isActive) cont.resume(null)
        }
    }
}
