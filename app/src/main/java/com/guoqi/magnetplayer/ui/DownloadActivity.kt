package com.guoqi.magnetplayer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
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
import kotlinx.android.synthetic.main.activity_download.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import java.io.File
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class DownloadActivity : AppCompatActivity() {

    companion object {
        val TAG = DownloadActivity::class.java.simpleName
        val TAG_URI = "uri"
        var hasTitle = false
        var REQUESTCODE_FROM_ACTIVITY = 1000
        val MOV_FORMAT = arrayOf(".3gpp", ".png", ".avi", ".asf", ".asx", ".fvi", ".flv", ".lsf", ".lsx", ".m4u", ".mng", ".movie", ".pvx", ".m4v", ".mov", ".mp4", ".mpe", ".mpeg", ".mpg", ".mpg4", ".qt", ".rv", ".wm", ".wmv", ".wmx", ".wv", ".wvx", ".vdo", ".viv", ".vivo")
        val IMG_FORMAT = arrayOf(".bmp", ".gif", ".jpeg", ".jpg", ".png", ".svf", ".svg", ".tif", ".tiff", ".wbmp")
        var countTime = 300 //超时时间
    }

    private var torrentSession: TorrentSession? = null
    private var startDownloadTask: DownloadTask? = null
    private var torrentPieceAdapter: TorrentPieceAdapter = TorrentPieceAdapter()

    //下载是否完成
    private var isFinish = false
    //是否正在下载
    private var isDownloading = false
    //下载uri
    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        toolbar.title = getString(R.string.title_magnet_download)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        initRecycleView()

        intent.getStringExtra(TAG_URI)?.let {
            uri = Uri.parse(it)
        }

        if (uri?.scheme == MagnetUtils.MAGNET_PREFIX) {
            pd.longSnackbar(getString(R.string.tip_waiting_for_p2p))
            startDecodeTask()
        } else {
            pd.snackbar(getString(R.string.uri_is_wrong))
        }

        btn_option.setOnClickListener {
            if (btn_option.text == getString(R.string.tip_retry)) {
                setOptionClick(getString(R.string.tip_retry))
            } else {
                setOptionClick(null)
            }
        }

        btn_open.setOnClickListener {
            LFilePicker()
                    .withActivity(this)
                    .withRequestCode(REQUESTCODE_FROM_ACTIVITY)
                    .withStartPath(rootPath)
                    .withIsGreater(true)//过滤文件大小 小于指定大小的文件
                    .withFileSize(500 * 1024)//指定文件大小为500K
                    .withTitle(getString(R.string.title_download_folder))
                    .withTitleColor("#FFFFFF")
                    .withBackIcon(Constant.BACKICON_STYLETHREE)
                    .withBackgroundColor("#333333")
                    .withFileFilter(MOV_FORMAT + IMG_FORMAT)
                    .withMutilyMode(false)
                    .start()
        }
    }


    /**
     * 暂停/继续
     */
    private fun setOptionClick(type: String?) {
        if (torrentSession!!.isPaused) {
            //继续
            torrentSession?.resume()
            btn_option?.text = getString(R.string.tip_pause)

            //改变标题提示
            if (!hasTitle) {
                resumeTimer()
                tv_title.text = getString(R.string.tip_fetching_data)
                type?.let { stopTimer();startTimer(300) }
            } else {
                tv_title.text = tv_title.text.toString().replace("""[${getString(R.string.tip_pause)}]""", "")
            }
        } else {
            //暂停
            torrentSession?.pause()
            btn_option.visibility = View.VISIBLE
            btn_option?.text = type ?: getString(R.string.tip_continue)

            //改变标题提示
            if (!hasTitle) {
                pauseTimer()
                tv_title.text = getString(R.string.tip_fetching_data_pause)
                type?.let {
                    tv_progress.text = getString(R.string.tip_download_fail)
                    tv_title.text = getString(R.string.tip_fetch_time_out)
                }
            } else {
                tv_title.text = tv_title.text.toString() + """[${getString(R.string.tip_pause)}]"""
            }
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
                        if (tv_log.text.length > 1280) {
                            tv_log.text = "\n> 自动清除日志 "
                        }
                    }
                }
            }

            override fun onAlertException(err: String) {
                Log.e(TAG, "onAlertException:$err")
                if (err == NO_ROUTER_FOUND && isDownloading) {
                    torrentSession?.pause()
                    torrentSession?.resume()
                }
            }

            override fun onAddTorrent(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第一步,将磁链转为种子
                Log.e(TAG, "onAddTorrent" + torrentSessionStatus.toString())
                isDownloading = true
                showLog(torrentSessionStatus)
                startTimer(300)
                runOnUiThread {
                    pd.visibility = View.GONE
                    btn_option.visibility = View.VISIBLE
                }
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
        stopTimer()
        torrentSession?.listener = null
        torrentSession?.stop()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinish && isDownloading && torrentSession!!.isPaused)
            torrentSession?.resume()
    }

    override fun onBackPressed() {
        if (isDownloading) {
            alert {
                title = getString(R.string.tip_tip)
                message = getString(R.string.tip_confirm_downloading)
                yesButton { super.onBackPressed() }
                noButton { }
            }.show()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * 定时器暂停/继续/重试
     */
    private var pause = false
    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: android.os.Message) {
            if (!pause) {
                updateCurrentTime()
            }
        }
    }

    private fun updateCurrentTime() {
        if (countTime-- > 0) {
            if (!hasTitle) {
                mHandler.sendEmptyMessageDelayed(0, 1000)
                tv_title.text = getString(R.string.tip_fetching_data)
                tv_progress.text = "${countTime}秒"
            }
        } else {
            if (!hasTitle) {
                setOptionClick(getString(R.string.tip_retry))
            }
        }
    }

    fun startTimer(seconds: Int) {
        pause = false
        countTime = seconds
        mHandler.sendEmptyMessage(0)
    }

    private fun pauseTimer() {
        pause = true
        mHandler.removeMessages(0)
    }

    private fun resumeTimer() {
        pause = false
        mHandler.sendEmptyMessage(0)
    }

    private fun stopTimer() {
        pause = true
        mHandler.removeMessages(0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUESTCODE_FROM_ACTIVITY) {
                var list = data?.getStringArrayListExtra(Constant.RESULT_INFO) as ArrayList<String>
                Log.e(TAG, "选择的文件 = " + list.toString())
                //获取文件后缀名
                var fileName = File(list[0].trim()).name
                var suffix = fileName.substring(fileName.lastIndexOf("."))
                when {
                    IMG_FORMAT.asList().any { it == suffix } -> {
                        var intent = Intent(this@DownloadActivity, PhotoActivity::class.java)
                        intent.putExtra("url", """file://${list[0].trim()}""")
                        startActivity(intent)
                    }
                    MOV_FORMAT.asList().any { it == suffix } -> startPlay(list[0])
                    else -> toast("不支持此文件格式")
                }
            }
        }
    }

    /**
     * 开始播放
     */
    private fun startPlay(path: String) {
        val intent = Intent(this@DownloadActivity, PlayerActivity::class.java)
        intent.putExtra("url", """file://$path""")
        intent.putExtra("title", File(path.trim()).name)
        startActivity(intent)
    }

}
