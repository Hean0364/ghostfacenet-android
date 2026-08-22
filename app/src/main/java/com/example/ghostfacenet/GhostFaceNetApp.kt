package com.example.ghostfacenet

import android.app.Application
import com.example.ghostfacenet.data.FaceRepository

class GhostFaceNetApp : Application() {

    lateinit var repository: FaceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FaceRepository(this)
    }
}
