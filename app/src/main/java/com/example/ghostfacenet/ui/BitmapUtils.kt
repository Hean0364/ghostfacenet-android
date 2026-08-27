package com.example.ghostfacenet.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.ghostfacenet.data.ImageOrientation

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = try {
    val orientation = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it)
    }?.let { bitmap -> ImageOrientation.correct(bitmap, orientation) }
} catch (e: Exception) {
    null
}
