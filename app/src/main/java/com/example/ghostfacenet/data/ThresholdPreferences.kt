package com.example.ghostfacenet.data

import android.content.Context

/**
 * Umbral de similitud coseno para aceptar una coincidencia de reconocimiento.
 * Valor por defecto calibrado empiricamente contra el benchmark LFW
 * (ver model_prep/evaluate_lfw.py): umbral optimo ~0.265, se deja margen a 0.30.
 */
object ThresholdPreferences {
    private const val PREFS_NAME = "ghostfacenet_prefs"
    private const val KEY_THRESHOLD = "recognition_threshold"
    const val DEFAULT_THRESHOLD = 0.30f

    fun get(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_THRESHOLD, DEFAULT_THRESHOLD)

    fun set(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_THRESHOLD, value)
            .apply()
    }
}
