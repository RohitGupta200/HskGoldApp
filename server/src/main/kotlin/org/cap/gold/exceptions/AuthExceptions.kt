package org.cap.gold.exceptions

// Base exception for authentication errors
open class AuthException(message: String) : RuntimeException(message)

// Thrown when a user with the same phone number already exists
class UserAlreadyExistsException(phoneNumber: String) : AuthException("User with phone number $phoneNumber already exists")

// Thrown when a user is not found
class UserNotFoundException(message: String = "User not found") : AuthException(message)

// Thrown when invalid credentials are provided
class InvalidCredentialsException : AuthException("Invalid phone number or password")

// Thrown when a refresh token is invalid or expired
class InvalidRefreshTokenException : AuthException("Invalid or expired refresh token")

// Thrown when a request is missing required fields
class MissingFieldException(field: String) : AuthException("Missing required field: $field")

// Thrown when a resource already exists
class ResourceConflictException(message: String) : AuthException(message)

// Thrown when a request is invalid
class BadRequestException(message: String) : AuthException(message)

// Thrown when a resource is not found
class NotFoundException(message: String) : AuthException(message)

// Thrown when authentication/authorization fails (e.g., missing/invalid token)
class UnauthorizedException(message: String) : AuthException(message)
