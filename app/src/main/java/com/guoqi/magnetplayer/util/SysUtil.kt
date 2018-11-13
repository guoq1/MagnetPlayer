package com.guoqi.magnetplayer.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import com.guoqi.magnetplayer.App

/**
 *  作者    GUOQI
 *  时间    2018/11/13 13:32
 *  描述
 */
object SysUtil {
    /**
     * 获取应用程序版本（versionName）
     *
     * @return 1.2.1
     */
    fun getVersionName(): String {
        val manager = App.instance?.packageManager
        var info: PackageInfo? = null
        try {
            info = manager?.getPackageInfo(App.instance?.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("SysUtil", "获取应用程序版本失败，原因：" + e.message)
            return ""
        }

        return info!!.versionName
    }

    /**
     * 获取版本号（versionCode）
     *
     * @return 11
     */
    fun getVersionCode(): Int {
        val versionCode: Int
        try {
            versionCode = App.instance?.packageManager?.getPackageInfo(App.instance?.packageName, 0)?.versionCode ?: 0
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("SysUtil", "获取版本号失败，原因：" + e.message)
            return 0
        }
        return versionCode
    }
}
