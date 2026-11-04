package com.sebas.musicplayer

import android.app.Application
import com.sebas.musicplayer.di.AppContainer

class MusicApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
