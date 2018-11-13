package com.guoqi.magnetplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.util.SysUtil
import kotlinx.android.synthetic.main.activity_about.*


/**
 *  作者    GUOQI
 *  时间    2018/11/12 19:52
 *  描述
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        toolbar.title = getString(R.string.title_about)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        tv_appVersion.text = """v${SysUtil.getVersionName()} (${SysUtil.getVersionCode()})"""
        tv_developer.setOnClickListener {
            val uri = Uri.parse("https://madaigou.oschina.io/gqblog/about/")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}