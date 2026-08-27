package com.example.ghostfacenet.ml

import com.example.ghostfacenet.data.db.EmbeddingWithPerson

data class MatchResult(
    val personId: Long,
    val personName: String,
    val referenceImageBase64: String,
    val similarity: Float
)

/**
 * Compara un embedding capturado contra todos los embeddings guardados
 * (identificacion 1:N) y devuelve la mejor coincidencia por persona.
 */
object FaceMatcher {

    /**
     * Devuelve, por cada persona enrolada, su mejor similitud contra el
     * embedding capturado, ordenadas de mayor a menor (top [topN]). Sirve
     * tanto para decidir el match principal (la primera del resultado, si
     * supera el umbral) como para mostrar candidatas cercanas cuando el
     * resultado es ambiguo o ninguna alcanza el umbral.
     */
    fun findTopMatches(
        queryEmbedding: FloatArray,
        candidates: List<EmbeddingWithPerson>,
        topN: Int = 5
    ): List<MatchResult> {
        val bestPerPerson = HashMap<Long, MatchResult>()

        for (candidate in candidates) {
            val candidateEmbedding = EmbeddingCodec.decode(candidate.embedding)
            val similarity = cosineSimilarity(queryEmbedding, candidateEmbedding)
            val current = bestPerPerson[candidate.perfilId]
            if (current == null || similarity > current.similarity) {
                bestPerPerson[candidate.perfilId] = MatchResult(
                    personId = candidate.perfilId,
                    personName = candidate.personName,
                    referenceImageBase64 = candidate.referenceImageBase64,
                    similarity = similarity
                )
            }
        }

        return bestPerPerson.values.sortedByDescending { it.similarity }.take(topN)
    }

    /** Ambos vectores ya vienen L2-normalizados desde EmbeddingExtractor, asi que el producto punto == similitud coseno. */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) dot += a[i] * b[i]
        return dot
    }
}
