package com.example.ghostfacenet.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.ThresholdPreferences
import com.example.ghostfacenet.ui.people.PeopleListScreen
import com.example.ghostfacenet.ui.people.PersonDetailScreen
import com.example.ghostfacenet.ui.recognize.RecognizeScreen
import com.example.ghostfacenet.ui.settings.SettingsScreen

@Composable
fun GhostFaceNetNavHost(app: GhostFaceNetApp) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var threshold by remember { mutableStateOf(ThresholdPreferences.get(context)) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                Screen.items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) { launchSingleTop = true }
                        },
                        icon = { Text(screen.label.first().toString()) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recognize.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Recognize.route) {
                RecognizeScreen(
                    app = app,
                    threshold = threshold,
                    onPersonClick = { personId ->
                        navController.navigate(Screen.PersonDetail.route(personId))
                    }
                )
            }
            composable(Screen.People.route) {
                PeopleListScreen(app, onPersonClick = { personId ->
                    navController.navigate(Screen.PersonDetail.route(personId))
                })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(app) { newValue -> threshold = newValue }
            }
            composable(
                route = Screen.PersonDetail.route,
                arguments = listOf(navArgument("personId") { type = NavType.LongType })
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
                PersonDetailScreen(app, personId, onBack = { navController.popBackStack() })
            }
        }
    }
}
