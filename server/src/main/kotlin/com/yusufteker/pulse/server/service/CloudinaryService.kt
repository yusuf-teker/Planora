package com.yusufteker.pulse.server.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object CloudinaryService {

    private val dotenv = Dotenv.configure().ignoreIfMissing().load()

    private val cloudinary = Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME") ?: dotenv["CLOUDINARY_CLOUD_NAME"],
            "api_key", System.getenv("CLOUDINARY_API_KEY") ?: dotenv["CLOUDINARY_API_KEY"],
            "api_secret", System.getenv("CLOUDINARY_API_SECRET") ?: dotenv["CLOUDINARY_API_SECRET"],
            "secure", true
        )
    )

    /**
     * Uploads an image byte array to Cloudinary and returns the secure URL.
     */
    suspend fun uploadProfileImage(imageBytes: ByteArray, userId: Int): String {
        return withContext(Dispatchers.IO) {
            // Generate a unique filename
            val publicId = "pulse_user_${userId}_${UUID.randomUUID()}"
            
            // Upload to Cloudinary in the "profile_pictures" folder
            val uploadResult = cloudinary.uploader().upload(
                imageBytes,
                ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "profile_pictures",
                    "overwrite", true,
                    "resource_type", "image"
                )
            )

            // Return the secure URL
            val secureUrl = uploadResult["secure_url"] as? String
            secureUrl ?: throw Exception("Cloudinary did not return a secure_url")
        }
    }
    /**
     * Extracts public_id from secure URL and deletes it from Cloudinary.
     */
    suspend fun deleteImageByUrl(url: String) {
        withContext(Dispatchers.IO) {
            try {
                // Example URL: https://res.cloudinary.com/cloud_name/image/upload/v1234/profile_pictures/pulse_user_1_uuid.jpg
                val uploadIndex = url.indexOf("/upload/")
                if (uploadIndex != -1) {
                    val afterUpload = url.substring(uploadIndex + 8)
                    val firstSlashIndex = afterUpload.indexOf("/")
                    if (firstSlashIndex != -1) {
                        val pathWithExtension = afterUpload.substring(firstSlashIndex + 1)
                        val lastDotIndex = pathWithExtension.lastIndexOf(".")
                        val publicId = if (lastDotIndex != -1) pathWithExtension.substring(0, lastDotIndex) else pathWithExtension
                        
                        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to delete old image from Cloudinary: ${e.message}")
            }
        }
    }
}
