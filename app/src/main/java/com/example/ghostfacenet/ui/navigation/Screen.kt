package com.example.ghostfacenet.ui.navigation

sealed class Screen(val route: String, val label: String) {
    data object Recognize : Screen("recognize", "Reconocer")
    data object People : Screen("people", "Personas")
    data object Import : Screen("import", "Importar")
    data object Settings : Screen("settings", "Ajustes")

    companion object {
        val items = listOf(Recognize, People, Import, Settings)
    }
}
