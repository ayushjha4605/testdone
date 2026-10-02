package com.testdone.app

import android.app.Application
import com.testdone.app.di.AppContainer

class TestDoneApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
