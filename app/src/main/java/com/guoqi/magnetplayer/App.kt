package com.guoqi.magnetplayer

import android.app.Application
import com.tencent.bugly.Bugly

class App : Application() {

    companion object {
        var instance: App? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Bugly.init(applicationContext, "19856250dd", BuildConfig.DEBUG);
    }
}