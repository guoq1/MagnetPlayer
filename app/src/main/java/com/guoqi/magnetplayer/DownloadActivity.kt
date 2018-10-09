package com.guoqi.magnetplayer

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.View
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentStatus.State
import com.masterwok.simpletorrentandroid.TorrentSession
import com.masterwok.simpletorrentandroid.TorrentSessionOptions
import com.masterwok.simpletorrentandroid.contracts.TorrentSessionListener
import com.masterwok.simpletorrentandroid.models.TorrentSessionStatus
import kotlinx.android.synthetic.main.activity_download.*
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode

class DownloadActivity : AppCompatActivity() {

    private val TAG = DownloadActivity::class.java.simpleName
    private lateinit var torrentSession: TorrentSession
    private var startDownloadTask: DownloadTask? = null
    private val torrentPieceAdapter: TorrentPieceAdapter = TorrentPieceAdapter()

    companion object {
        val TAG_URI = "uri"
        var isDownloading = false
    }

    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        initRecycleView()

        uri = if (intent.data != null)
            intent.data
        else
            intent.getParcelableExtra(TAG_URI)


        if (uri?.scheme == Utils.MAGNET_PREFIX) {
            Snackbar.make(pd, "正在获取磁链信息", Snackbar.LENGTH_LONG).show()
            pd.visibility = View.VISIBLE
            runOnUiThread {
                startDecodeTask()
            }
        } else {
            Snackbar.make(pd, "Uri不正确", Snackbar.LENGTH_LONG).show()
        }



        btn.setOnClickListener {
            if (torrentSession.isPaused) {
                torrentSession.resume()
                btn?.text = "暂停"
            } else {
                torrentSession.pause()
                btn?.text = "继续"
            }
        }
    }

    private fun initRecycleView() {
        rv_download.apply {
            layoutManager = GridLayoutManager(this@DownloadActivity, 16)!!
            adapter = torrentPieceAdapter
        }
    }

    private fun refreshData(data: TorrentSessionStatus) {
        rv_download.post {
            torrentPieceAdapter.refreshData(data)
        }
    }

    private fun startDecodeTask() {
        val torrentSessionOptions = TorrentSessionOptions(downloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), onlyDownloadLargestFile = true, enableLogging = false, shouldStream = true)
        torrentSession = TorrentSession(torrentSessionOptions)
        torrentSession.listener = object : TorrentSessionListener {
            override fun onAddTorrent(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第一步,将磁链转为种子
                Log.e(TAG, "onAddTorrent" + torrentSessionStatus.toString())
                isDownloading = true
                showLog(torrentSessionStatus)
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
                    }
                    Snackbar.make(pd, "下载到: /DownLoad 目录下", Snackbar.LENGTH_LONG).show()
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

        startDownloadTask = DownloadTask(this, torrentSession, uri!!)
        startDownloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun showLog(torrentSessionStatus: TorrentSessionStatus) {
        refreshData(torrentSessionStatus)
        when (torrentSessionStatus.state) {
            State.CHECKING_FILES -> {
                //检查已有文件,还未开始下载
                tv_progress.text = "正在检查文件..."
            }
            State.DOWNLOADING_METADATA -> {
                //从peers获取元数据
                tv_progress.text = "正在获取元数据..."
            }
            State.DOWNLOADING -> {
                //正在下载,返回下载量
                tv_progress.text = "下载中 " + BigDecimal((torrentSessionStatus.progress).toDouble()).setScale(2, RoundingMode.HALF_UP).toString() + "%"
            }
            State.SEEDING -> {
                //下载完成
                tv_progress.text = "下载已完成"
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

        torrentSession.listener = null
        torrentSession.stop()
    }

    override fun onBackPressed() {
        if (isDownloading) {
            var dialog = AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("正在下载中,是否要取消下载")
                    .setPositiveButton("确定") { _, _ ->
                        super.onBackPressed()
                    }
                    .setNegativeButton("取消", null)
            dialog.show()
        }else {
            super.onBackPressed()
        }
    }

}
