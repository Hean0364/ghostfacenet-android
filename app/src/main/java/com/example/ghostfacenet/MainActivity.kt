package com.example.ghostfacenet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ghostfacenet.ui.navigation.GhostFaceNetNavHost
import com.example.ghostfacenet.ui.theme.GhostFaceNetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GhostFaceNetTheme {
                GhostFaceNetNavHost(application as GhostFaceNetApp)
            }
        }
    }
}
