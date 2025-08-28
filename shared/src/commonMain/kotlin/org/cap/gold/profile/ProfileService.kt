package org.cap.gold.profile

import org.cap.gold.model.User

interface ProfileService {
    suspend fun changePassword(currentPassword: String? = null, newPassword: String)
    suspend fun changePhone(newPhone: String,password: String): User
    suspend fun changeEmail(newEmail: String,password: String): User
    suspend fun changeName(newName: String,password: String): User
    suspend fun getMe(): User

}
