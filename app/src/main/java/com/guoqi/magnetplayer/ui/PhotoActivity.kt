package com.guoqi.magnetplayer.ui

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.guoqi.magnetplayer.R
import kotlinx.android.synthetic.main.activity_photo.*


/**
 *  作者    GUOQI
 *  时间    2018/11/12 19:52
 *  描述
 */
class PhotoActivity : AppCompatActivity() {

    var mAttacher: PhotoViewAttacher? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        var url = intent.getStringExtra("url")

        mAttacher = PhotoViewAttacher(pv)
        pv.setImageURI(Uri.parse(url))
        mAttacher!!.update()
    }
}