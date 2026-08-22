package com.example.ghostfacenet.ml

import com.example.ghostfacenet.data.db.EmbeddingWithPerson

data class MatchResult(
    val personId: Long,
    val personName: String,
    val referenceImagePath: String,
    val similarity: Float
)

/**
 * Compara un embedding capturado contra todos los embeddings guardados
 * (identificacion 1:N) y devuelve la mejor coincidencia por persona.
 */
object FaceMatcher {

    fun findBestMatch(
        queryEmbedding: FloatArray,
        candidates: List<EmbeddingWithPerson>,
        threshold: Float
    ): MatchResult? {
        val bestPerPerson = HashMap<Long, MatchResult>()

        for (candidate in candidates) {
            val candidateEmbedding = EmbeddingCodec.decode(candidate.embedding)
            val similarity = cosineSimilarity(queryEmbedding, candidateEmbedding)
            val current = bestPerPerson[candidate.personId]
            if (current == null || similarity > current.similarity) {
                bestPerPerson[candidate.personId] = MatchResult(
                    personId = candidate.personId,
                    personName = candidate.personName,
                    referenceImagePath = candidate.referenceImagePath,
                    similarity = similarity
                )
            }
        }

        val best = bestPerPerson.values.maxByOrNull { it.similarity } ?: return null
        return if (best.similarity >= threshold) best else null
    }

    /** Ambos vectores ya vienen L2-normalizados desde EmbeddingExtractor, asi que el producto punto == similitud coseno. */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) dot += a[i] * b[i]
        return dot
    }
}
