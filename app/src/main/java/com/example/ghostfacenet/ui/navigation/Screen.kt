package com.example.ghostfacenet.ui.navigation

sealed class Screen(val route: String, val label: String) {
    data object Recognize : Screen("recognize", "Reconocer")
    data object People : Screen("people", "Personas")
    data object Import : Screen("import", "Importar")
    data object Settings : Screen("settings", "Ajustes")
    data object PersonDetail : Screen("person/{personId}", "Persona") {
        fun route(personId: Long) = "person/$personId"
    }

    companion object {
        val items = listOf(Recognize, People, Import, Settings)
    }
}
