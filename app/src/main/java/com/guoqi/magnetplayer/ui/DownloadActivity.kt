package com.guoqi.magnetplayer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentStatus
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.adapter.TorrentPieceAdapter
import com.guoqi.magnetplayer.core.TorrentSession
import com.guoqi.magnetplayer.core.TorrentSession.Companion.NO_ROUTER_FOUND
import com.guoqi.magnetplayer.core.TorrentSessionOptions
import com.guoqi.magnetplayer.core.contracts.TorrentSessionListener
import com.guoqi.magnetplayer.core.models.TorrentSessionStatus
import com.guoqi.magnetplayer.ui.MainActivity.Companion.rootPath
import com.guoqi.magnetplayer.util.MagnetUtils
import com.leon.lfilepickerlibrary.LFilePicker
import com.leon.lfilepickerlibrary.utils.Constant
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_download.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import java.io.File
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class DownloadActivity : AppCompatActivity() {

    private val TAG = DownloadActivity::class.java.simpleName
    private var torrentSession: TorrentSession? = null
    private var startDownloadTask: DownloadTask? = null
    private var torrentPieceAdapter: TorrentPieceAdapter = TorrentPieceAdapter()

    //下载是否完成
    private var isFinish = false
    //是否正在下载
    private var isDownloading = false

    companion object {
        val TAG_URI = "uri"
        var hasTitle = false
        var REQUESTCODE_FROM_ACTIVITY = 1000
        val MOV_FORMAT = arrayOf(".3gpp", ".png", ".avi", ".asf", ".asx", ".fvi", ".flv", ".lsf", ".lsx", ".m4u", ".mng", ".movie", ".pvx", ".m4v", ".mov", ".mp4", ".mpe", ".mpeg", ".mpg", ".mpg4", ".qt", ".rv", ".wm", ".wmv", ".wmx", ".wv", ".wvx", ".vdo", ".viv", ".vivo")
        val IMG_FORMAT = arrayOf(".bmp",".gif",".jpeg",".jpg",".png", ".svf",".svg",".tif",".tiff",".wbmp")
    }

    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        toolbar.title = "磁力下载"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        initRecycleView()

        intent.getStringExtra(TAG_URI)?.let {
            uri = Uri.parse(it)
        }

        if (uri?.scheme == MagnetUtils.MAGNET_PREFIX) {
            pd.longSnackbar("等待时间根据当前P2P网络情况而定...")
            startDecodeTask()
        } else {
            pd.snackbar("Uri不正确")
        }



        btn_option.setOnClickListener {
            if (btn_option.text == "重试") {
                setContinueClick("重试")
            } else {
                setContinueClick(null)
            }
        }

        btn_open.setOnClickListener {
            LFilePicker()
                    .withActivity(this)
                    .withRequestCode(REQUESTCODE_FROM_ACTIVITY)
                    .withStartPath(rootPath)
                    .withIsGreater(true)//过滤文件大小 小于指定大小的文件
                    .withFileSize(500 * 1024)//指定文件大小为500K
                    .withTitle("下载目录")
                    .withTitleColor("#FFFFFF")
                    .withBackIcon(Constant.BACKICON_STYLETHREE)
                    .withBackgroundColor("#333333")
                    .withFileFilter(MOV_FORMAT + IMG_FORMAT)
                    .withMutilyMode(false)
                    .start()
        }
    }

    /**
     * 开始播放
     */
    private fun startPlay(path: String) {
        val intent = Intent(this@DownloadActivity, PlayerActivity::class.java)
        //val path = "$rootPath/${tv_title.text}"
        Log.e(TAG, "播放的path = $path")
        intent.putExtra("url", Uri.parse(path))
        intent.putExtra("title", tv_title.text.toString())
        startActivity(intent)
    }

    private fun setContinueClick(retry: String?) {
        if (torrentSession!!.isPaused) {
            torrentSession?.resume()
            btn_option?.text = "暂停"
            retry?.let { pd.visibility = View.VISIBLE;countDown(300) }
            btn_option.visibility = View.GONE
        } else {
            torrentSession?.pause()
            btn_option.visibility = View.VISIBLE
            btn_option?.text = retry ?: "继续"
            retry?.let { tv_progress.text = "下载失败" }
            tv_title.text = "获取元数据超时, 可尝试复制磁链到迅雷..."
            pd.visibility = View.GONE
        }
    }


    private fun initRecycleView() {
        tv_log.visibility = View.VISIBLE
        tv_log.movementMethod = ScrollingMovementMethod.getInstance()
        rv_download.visibility = View.GONE

        rv_download.apply {
            layoutManager = (GridLayoutManager(this@DownloadActivity, 16) as RecyclerView.LayoutManager?)!!
            adapter = torrentPieceAdapter
        }
    }

    private fun refreshData(data: TorrentSessionStatus) {
        rv_download.post {
            torrentPieceAdapter.refreshData(data)
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

    private fun startDecodeTask() {
        pd.visibility = View.VISIBLE
        val torrentSessionOptions = TorrentSessionOptions(downloadLocation = File(rootPath), onlyDownloadLargestFile = true, enableLogging = false, shouldStream = true)
        torrentSession = TorrentSession(torrentSessionOptions)
        torrentSession?.listener = object : TorrentSessionListener {
            override fun onLogMessage(log: String) {
                //打印日志
                if (tv_log.visibility == View.VISIBLE) {
                    runOnUiThread {
                        tv_log.text = """$log\n${tv_log.text}"""
                        if (tv_log.text.length > 2048) {
                            tv_log.text = "\n> 自动清除日志 "
                        }
                    }
                }
            }

            override fun onAlertException(err: String) {
                Log.e(TAG, "onAlertException:$err")
                if (err == NO_ROUTER_FOUND) {
                    torrentSession?.pause()
                    torrentSession?.resume()
                }
            }

            override fun onAddTorrent(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第一步,将磁链转为种子
                Log.e(TAG, "onAddTorrent" + torrentSessionStatus.toString())
                isDownloading = true
                showLog(torrentSessionStatus)
                countDown(300)
                runOnUiThread { pd.visibility = View.GONE }
            }

            override fun onTorrentResumed(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第二步,添加种子到下载队列 / 暂停后继续下载
                Log.e(TAG, "onTorrentResumed" + torrentSessionStatus.toString())
                isDownloading = true
                showLog(torrentSessionStatus)
            }

            override fun onMetadataReceived(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第三步, 获取到种子中的信息
                Log.e(TAG, "onMetadataReceived" + torrentSessionStatus.toString())
                runOnUiThread {
                    pd.visibility = View.GONE
                    var title = torrentSessionStatus.magnetUri.toString()
                    if (title.contains("&dn=")) {
                        tv_title.text = title.substring(title.indexOf("&dn=") + 4)
                        tv_title.gravity = Gravity.LEFT
                        hasTitle = true
                        btn_open.visibility = View.VISIBLE
                        tv_log.visibility = View.GONE
                        rv_download.visibility = View.VISIBLE
                    }
                    pd.longSnackbar("下载到: /DownLoad 目录下")
                }
                showLog(torrentSessionStatus)
            }

            override fun onBlockUploaded(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第四步,加载块
                Log.e(TAG, "onBlockUploaded" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }

            override fun onPieceFinished(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第五步,块下载完成状态
                Log.e(TAG, "onPieceFinished" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }

            override fun onTorrentFinished(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                isDownloading = false
                Log.e(TAG, "onTorrentFinished" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }


            override fun onMetadataFailed(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                isDownloading = false
                Log.e(TAG, "onMetadataFailed" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }


            override fun onTorrentDeleteFailed(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                isDownloading = false
                Log.e(TAG, "onTorrentDeleteFailed" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }

            override fun onTorrentDeleted(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                isDownloading = false
                Log.e(TAG, "onTorrentDeleted" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }

            override fun onTorrentError(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                isDownloading = false
                Log.e(TAG, "onTorrentError" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }

            override fun onTorrentPaused(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                isDownloading = false
                //暂停
                Log.e(TAG, "onTorrentPaused" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }

            override fun onTorrentRemoved(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                Log.e(TAG, "onTorrentRemoved" + torrentSessionStatus.toString())
                showLog(torrentSessionStatus)
            }


        }

        startDownloadTask = DownloadTask(this, torrentSession!!, uri!!)
        startDownloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun showLog(torrentSessionStatus: TorrentSessionStatus) {
        when (torrentSessionStatus.state) {
            TorrentStatus.State.DOWNLOADING -> {
                //正在下载,返回下载量
                refreshData(torrentSessionStatus)
                tv_progress.text = "下载中 " + BigDecimal((torrentSessionStatus.progress).toDouble() * 100).setScale(2, RoundingMode.HALF_UP).toString() + "%"
                isDownloading = true
            }
            TorrentStatus.State.SEEDING -> {
                //下载完成
                refreshData(torrentSessionStatus)
                tv_progress.text = "下载已完成"
                isDownloading = false
                isFinish = true
            }
        }

    }

    private class DownloadTask : AsyncTask<Void, Void, Unit> {

        private val context: WeakReference<Context>
        private val torrentSession: WeakReference<TorrentSession>
        val magnetUri: Uri

        @Suppress("ConvertSecondaryConstructorToPrimary")
        constructor(context: Context, torrentSession: TorrentSession, magnetUri: Uri) : super() {
            this.context = WeakReference(context)
            this.torrentSession = WeakReference(torrentSession)
            this.magnetUri = magnetUri
        }

        override fun doInBackground(vararg args: Void) {
            try {
                torrentSession.get()?.start(context.get()!!, magnetUri)
            } catch (ex: Exception) {
                Log.e("DownloadTask", "Failed to start torrent", ex)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        torrentSession?.listener = null
        torrentSession?.stop()
    }

    override fun onPause() {
        super.onPause()
        if (!isFinish)
            torrentSession?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinish)
            torrentSession?.resume()
    }

    override fun onBackPressed() {
        if (isDownloading) {
            alert {
                title = "提示"
                message = "正在下载中，是否要取消下载？"
                yesButton { super.onBackPressed() }
                noButton { }
            }.show()
        } else {
            super.onBackPressed()
        }
    }


    @SuppressLint("CheckResult")
    fun countDown(countTime: Int) {
        Observable.interval(0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { increaseTime -> countTime - increaseTime.toInt() }
                .take((countTime + 1).toLong())
                .subscribe {
                    if (!hasTitle) {
                        tv_title.text = "正在获取元数据..."
                        tv_progress.text = "${it}秒"
                    }
                    if (it == 0 && !hasTitle) {
                        setContinueClick("重试")
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUESTCODE_FROM_ACTIVITY) {
                var list = data.getStringArrayListExtra(Constant.RESULT_INFO) as ArrayList<String>
                Log.e(TAG, "选择的文件 = " + list.toString())
                startPlay(list[0])
            }
        }
    }

}
