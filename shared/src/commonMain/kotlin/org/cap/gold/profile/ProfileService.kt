package org.cap.gold.profile

import org.cap.gold.model.User

interface ProfileService {
    suspend fun changePassword(newPassword: String)
    suspend fun changePhone(newPhone: String): User
    suspend fun getMe(): User
}
