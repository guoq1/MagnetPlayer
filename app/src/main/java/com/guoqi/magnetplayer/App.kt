package com.guoqi.magnetplayer

import android.app.Application
import com.tencent.bugly.Bugly

class App :Application(){

    override fun onCreate() {
        super.onCreate()

        Bugly.init(applicationContext, "19856250dd", BuildConfig.DEBUG);
    }
}