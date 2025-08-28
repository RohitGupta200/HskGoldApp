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

fun normalizeIndianPhone(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed

    // Already E.164
    if (trimmed.startsWith("+")) return trimmed

    // Strip non-digits
    val digits = trimmed.filter { it.isDigit() }
    if (digits.isEmpty()) return trimmed

    // Drop leading 0 for common local formats like 0XXXXXXXXXX
    val noLeadingZero = if (digits.length == 11 && digits.startsWith("0")) digits.drop(1) else digits

    return when {
        // 10-digit local Indian mobile number
        noLeadingZero.length == 10 -> "+91$noLeadingZero"
        // 12 digits starting with 91 (missing +)
        noLeadingZero.length == 12 && noLeadingZero.startsWith("91") -> "+$noLeadingZero"
        else -> "+$noLeadingZero" // Fallback: prefix + and pass through
    }
}
