package org.cap.gold.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

interface ImageUploader {
    suspend fun uploadImage(
        imageBytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<String> // Returns the URL of the uploaded image
    
    suspend fun uploadImageMultipart(
        partData: PartData,
        onProgress: (Float) -> Unit
    ): Result<String>
}

class ImageUploaderImpl(private val client: HttpClient) : ImageUploader {
    override suspend fun uploadImage(
        imageBytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        return try {
            val response = client.post("/api/upload") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("image", imageBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/*")
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            })
                        }
                    )
                )
            }
            
            if (response.status.isSuccess()) {
                val imageUrl = response.body<String>()
                Result.success(imageUrl)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImageMultipart(
        partData: PartData,
        onProgress: (Float) -> Unit
    ): Result<String> {
        return try {
            val response = client.post("/api/upload") {
                setBody(partData)
            }
            
            if (response.status.isSuccess()) {
                val imageUrl = response.body<String>()
                Result.success(imageUrl)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// For preview and testing
class MockImageUploader : ImageUploader {
    override suspend fun uploadImage(
        imageBytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        // Simulate upload progress
        repeat(5) { progress ->
            onProgress(progress * 0.2f)
            kotlinx.coroutines.delay(200)
        }
        return Result.success("https://example.com/images/$fileName")
    }

    override suspend fun uploadImageMultipart(
        partData: PartData,
        onProgress: (Float) -> Unit
    ): Result<String> {
        // Simulate upload progress
        repeat(5) { progress ->
            onProgress(progress * 0.2f)
            kotlinx.coroutines.delay(200)
        }
        return Result.success("https://example.com/images/uploaded_${Clock.System.now().toEpochMilliseconds()}.jpg")
    }
}
