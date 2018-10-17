package com.guoqi.magnetplayer.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dueeeke.videoplayer.player.PlayerConfig
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.util.FullScreenController
import kotlinx.android.synthetic.main.activity_player.*

/**
 * 横屏播放的界面
 */
class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player)

        val controller = FullScreenController(this)
        //高级设置（可选，须在start()之前调用方可生效）
        val playerConfig = PlayerConfig.Builder()
                .enableCache() //启用边播边缓存功能
//                .autoRotate() //启用重力感应自动进入/退出全屏功能
//                .enableMediaCodec()//启动硬解码，启用后可能导致视频黑屏，音画不同步
                .usingSurfaceView() //启用SurfaceView显示视频，不调用默认使用TextureView
                .savingProgress() //保存播放进度
                .disableAudioFocus() //关闭AudioFocusChange监听
//                .setLooping() //循环播放当前正在播放的视频
                .build()
        video_view.setPlayerConfig(playerConfig)
        video_view.setUrl(intent.getStringExtra("url"))
        video_view.title = intent.getStringExtra("title")
        video_view.setVideoController(controller)
        video_view.startFullScreen()
        video_view.start()
    }

    override fun onPause() {
        super.onPause()
        video_view.pause()
    }

    override fun onResume() {
        super.onResume()
        video_view.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        video_view.release()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (!video_view.onBackPressed()) {
            super.onBackPressed()
        }
    }
}