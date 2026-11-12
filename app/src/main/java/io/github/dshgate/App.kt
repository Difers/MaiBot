package io.github.dshgate

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Gate.init(this)
    }
}
