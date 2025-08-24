package org.cap.gold.util

import org.cap.gold.model.ValidationResult

/**
 * Validates a phone number.
 * @param phone The phone number to validate
 * @return ValidationResult with success status and error message if any
 */
fun validatePhoneNumber(phone: String): ValidationResult {
    return when {
        phone.isBlank() -> ValidationResult(
            success = false,
            errorMessage = "Phone number cannot be empty"
        )
        !phone.matches(Regex("^[0-9]{10,15}$")) -> ValidationResult(
            success = false,
            errorMessage = "Please enter a valid phone number"
        )
        else -> ValidationResult(success = true)
    }
}

/**
 * Validates a password.
 * @param password The password to validate
 * @return ValidationResult with success status and error message if any
 */
fun validatePassword(password: String): ValidationResult {
    return when {
        password.isBlank() -> ValidationResult(
            success = false,
            errorMessage = "Password cannot be empty"
        )
        else -> ValidationResult(success = true)
    }
}
