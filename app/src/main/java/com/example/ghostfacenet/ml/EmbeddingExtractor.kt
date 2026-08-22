package com.example.ghostfacenet.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Corre ghostfacenet.tflite sobre un rostro ya alineado/recortado a 112x112
 * y devuelve un embedding de 512 dimensiones, L2-normalizado.
 */
class EmbeddingExtractor(context: Context) {

    private val interpreter: Interpreter = Interpreter(
        loadModelFile(context, MODEL_FILE),
        Interpreter.Options().apply { setNumThreads(4) }
    )

    fun extract(faceBitmap: Bitmap): FloatArray {
        require(faceBitmap.width == INPUT_SIZE && faceBitmap.height == INPUT_SIZE) {
            "El bitmap debe ser de ${INPUT_SIZE}x${INPUT_SIZE}, era ${faceBitmap.width}x${faceBitmap.height}"
        }
        val input = bitmapToInputBuffer(faceBitmap)
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Mismo preprocesamiento usado al entrenar/convertir: (x - 127.5) / 128
            buffer.putFloat((r - 127.5f) * 0.0078125f)
            buffer.putFloat((g - 127.5f) * 0.0078125f)
            buffer.putFloat((b - 127.5f) * 0.0078125f)
        }
        buffer.rewind()
        return buffer
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vector) sumSquares += v * v
        val norm = sqrt(sumSquares).takeIf { it > 1e-8f } ?: 1f
        return FloatArray(vector.size) { vector[it] / norm }
    }

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return inputStream.channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() = interpreter.close()

    companion object {
        private const val MODEL_FILE = "ghostfacenet.tflite"
        const val INPUT_SIZE = FaceAligner.TARGET_SIZE
        const val EMBEDDING_SIZE = 512
    }
}
