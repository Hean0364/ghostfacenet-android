package com.example.ghostfacenet.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object Base64ImageCodec {

    fun decode(value: String): Bitmap? {
        val encoded = normalize(value)
        if (encoded.isEmpty()) return null

        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val orientation = ByteArrayInputStream(bytes).use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
            ImageOrientation.correct(bitmap, orientation)
        } catch (_: Exception) {
            null
        }
    }

    fun encode(bitmap: Bitmap, quality: Int = 90): String {
        val output = ByteArrayOutputStream()
        output.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
            return Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP)
        }
    }

    fun normalize(value: String): String {
        val trimmed = value.trim()
        return trimmed.substringAfter("base64,", trimmed)
    }
}
