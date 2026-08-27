package com.example.ghostfacenet

import android.app.Application
import com.example.ghostfacenet.data.FaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GhostFaceNetApp : Application() {

    lateinit var repository: FaceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FaceRepository(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            repository.initializeEmbeddings()
        }
    }
}
