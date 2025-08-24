package org.cap.gold.model

/**
 * Represents the result of a validation operation.
 * @property success Whether the validation was successful
 * @property errorMessage The error message if validation failed, null if successful
 */
data class ValidationResult(
    val success: Boolean,
    val errorMessage: String? = null
)
