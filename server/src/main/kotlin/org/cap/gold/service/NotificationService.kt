package org.cap.gold.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.cap.gold.models.AdminUsers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class NotificationService {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sendAdminBroadcastAsync(title: String, body: String) {
        scope.launch {
            try {
                val tokens = fetchAdminDeviceTokens()
                if (tokens.isEmpty()) {
                    logger.info { "No admin device tokens to send notification" }
                    return@launch
                }

                val message = MulticastMessage.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .addAllTokens(tokens)
                    .build()

                val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
                logger.info { "Sent admin broadcast: success=${response.successCount}, failure=${response.failureCount}" }
            } catch (t: Throwable) {
                logger.error(t) { "Failed to send admin broadcast notification" }
            }
        }
    }

    private suspend fun fetchAdminDeviceTokens(): List<String> = newSuspendedTransaction(Dispatchers.IO) {
        AdminUsers
            .slice(AdminUsers.fireDeviceToken)
            .select { AdminUsers.fireDeviceToken.isNotNull() }
            .mapNotNull { row -> rowToDeviceToken(row) }
            .distinct()
    }

    private fun rowToDeviceToken(row: ResultRow): String? = row[AdminUsers.fireDeviceToken]
}
