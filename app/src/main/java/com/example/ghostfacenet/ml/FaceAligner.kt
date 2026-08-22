package com.example.ghostfacenet.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2

/**
 * Alinea (nivela los ojos) y recorta un rostro detectado a 112x112, el
 * tamano de entrada esperado por GhostFaceNet.
 */
object FaceAligner {

    const val TARGET_SIZE = 112
    private const val MARGIN_RATIO = 0.35f

    fun alignAndCrop(source: Bitmap, face: Face): Bitmap {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        val rotationDegrees = if (leftEye != null && rightEye != null) {
            val dy = (rightEye.y - leftEye.y).toDouble()
            val dx = (rightEye.x - leftEye.x).toDouble()
            Math.toDegrees(atan2(dy, dx)).toFloat()
        } else {
            0f
        }

        val box = face.boundingBox
        val pivotX = box.centerX().toFloat()
        val pivotY = box.centerY().toFloat()

        val rotated = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { setRotate(rotationDegrees, pivotX, pivotY) }
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            output
        } else {
            source
        }

        val marginW = box.width() * MARGIN_RATIO
        val marginH = box.height() * MARGIN_RATIO
        val left = (box.left - marginW).toInt().coerceIn(0, rotated.width - 1)
        val top = (box.top - marginH).toInt().coerceIn(0, rotated.height - 1)
        val right = (box.right + marginW).toInt().coerceIn(left + 1, rotated.width)
        val bottom = (box.bottom + marginH).toInt().coerceIn(top + 1, rotated.height)

        val cropped = Bitmap.createBitmap(rotated, left, top, right - left, bottom - top)
        return Bitmap.createScaledBitmap(cropped, TARGET_SIZE, TARGET_SIZE, true)
    }
}
