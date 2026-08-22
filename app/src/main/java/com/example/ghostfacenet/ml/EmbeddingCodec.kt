package com.example.ghostfacenet.ml

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Serializa/deserializa un embedding (FloatArray) para guardarlo como BLOB en Room. */
object EmbeddingCodec {

    fun encode(values: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.nativeOrder())
        for (v in values) buffer.putFloat(v)
        return buffer.array()
    }

    fun decode(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) floats[i] = buffer.getFloat()
        return floats
    }
}
