package com.yusufteker.pulse.server.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object CloudinaryService {

    private val dotenv = Dotenv.load()

    private val cloudinary = Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", dotenv["CLOUDINARY_CLOUD_NAME"],
            "api_key", dotenv["CLOUDINARY_API_KEY"],
            "api_secret", dotenv["CLOUDINARY_API_SECRET"],
            "secure", true
        )
    )

    /**
     * Uploads an image byte array to Cloudinary and returns the secure URL.
     */
    suspend fun uploadProfileImage(imageBytes: ByteArray, userId: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
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
                uploadResult["secure_url"] as? String
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
