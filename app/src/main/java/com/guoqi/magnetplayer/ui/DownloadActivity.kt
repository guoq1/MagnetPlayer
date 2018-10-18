package com.guoqi.magnetplayer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentStatus
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.adapter.TorrentPieceAdapter
import com.guoqi.magnetplayer.core.TorrentSession
import com.guoqi.magnetplayer.core.TorrentSessionOptions
import com.guoqi.magnetplayer.core.contracts.TorrentSessionListener
import com.guoqi.magnetplayer.core.models.TorrentSessionStatus
import com.guoqi.magnetplayer.util.MagnetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_download.*
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class DownloadActivity : AppCompatActivity() {

    private val TAG = DownloadActivity::class.java.simpleName
    private lateinit var torrentSession: TorrentSession
    private var startDownloadTask: DownloadTask? = null
    private var torrentPieceAdapter: TorrentPieceAdapter = TorrentPieceAdapter()


    companion object {
        val TAG_URI = "uri"
        var isDownloading = false
        var hasTitle = false
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


        if (uri?.scheme == MagnetUtils.MAGNET_PREFIX) {
            Snackbar.make(pd, "正在获取磁链信息", Snackbar.LENGTH_LONG).show()
            startDecodeTask()
        } else {
            Snackbar.make(pd, "Uri不正确", Snackbar.LENGTH_LONG).show()
        }



        btn_option.setOnClickListener {
            if (btn_option.text == "重试") {
                setContinueClick("重试")
            } else {
                setContinueClick(null)
            }
        }

        btn_play.setOnClickListener {
            val intent = Intent(this@DownloadActivity, PlayerActivity::class.java)
            var path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/${tv_title.text}"
            Log.e(TAG, "path = $path")
            intent.putExtra("url", Uri.parse(path))
            intent.putExtra("title", tv_title.text.toString())
        }
    }

    private fun setContinueClick(retry: String?) {
        if (torrentSession.isPaused) {
            torrentSession.resume()
            btn_option?.text = "暂停"
            retry?.let { pd.visibility = View.VISIBLE;countDown(300) }
        } else {
            torrentSession.pause()
            btn_option?.text = retry ?: "继续"
            retry?.let { tv_progress.text = "获取元数据超时, 请重试或使用迅雷下载" }
            pd.visibility = View.GONE
        }
    }


    private fun initRecycleView() {
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

    private fun startDecodeTask() {
        pd.visibility = View.VISIBLE
        val torrentSessionOptions = TorrentSessionOptions(downloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), onlyDownloadLargestFile = true, enableLogging = false, shouldStream = true)
        torrentSession = TorrentSession(torrentSessionOptions)
        torrentSession.listener = object : TorrentSessionListener {
            override fun onAlertException(err: String) {
                Log.e(TAG, "onAlertException:$err")
            }

            override fun onAddTorrent(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
                //第一步,将磁链转为种子
                Log.e(TAG, "onAddTorrent" + torrentSessionStatus.toString())
                isDownloading = true
                showLog(torrentSessionStatus)
                countDown(300)
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
                        hasTitle = true
                        btn_play.visibility = View.VISIBLE
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
            TorrentStatus.State.DOWNLOADING -> {
                //正在下载,返回下载量
                tv_progress.text = "下载中 " + BigDecimal((torrentSessionStatus.progress).toDouble() * 100).setScale(2, RoundingMode.HALF_UP).toString() + "%"
            }
            TorrentStatus.State.SEEDING -> {
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
                        tv_title.text = "${it}秒"
                        tv_progress.text = "正在获取元数据..."
                    }
                    if (it == 0 && !hasTitle) {
                        tv_title.text = "未知"
                        setContinueClick("重试")
                    }
                }
    }

}
