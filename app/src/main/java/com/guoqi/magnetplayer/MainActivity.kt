package com.guoqi.magnetplayer

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.alibaba.fastjson.JSON
import com.guoqi.magnetplayer.ttorrent.common.Torrent
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.scwang.smartrefresh.layout.footer.ClassicsFooter
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {

    companion object {
        //source=种子搜&keyword=测试&page=1
        val rootPath = Environment.getExternalStorageDirectory().absolutePath + File.separator + "MagnetPlayer"
        const val LOAD_REFRESH = 1001
        const val LOAD_MORE = 1002
    }

    private lateinit var pd: ProgressDialog
    private var page = 1
    private var sourceArr = arrayOf("种子搜", "磁力吧", "BT兔子", "idope", "BTDB", "BT4G", "屌丝搜", "AOYOSO")
    private var source = sourceArr[0]
    private var recordList = ArrayList<RecordBean.Results>()
    private lateinit var recordAdapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initProgressDialog()
        initData()

        fab.setOnClickListener { view ->

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
            } else {
                decodeTorrent()

            }
        }
    }

    private fun initData() {
        btn_search.setOnClickListener {
            loadDialog()
            getSearchList(LOAD_REFRESH)
        }

        recordAdapter = RecordAdapter(this, recordList)
        lv_record.adapter = recordAdapter

        refreshLayout.setRefreshHeader(ClassicsHeader(this))
        refreshLayout.setRefreshFooter(ClassicsFooter(this))
        refreshLayout.isEnableLoadMore = true
        refreshLayout.setOnRefreshListener {
            getSearchList(LOAD_REFRESH)
        }
        refreshLayout.setOnLoadMoreListener {
            page++
            getSearchList(LOAD_MORE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initProgressDialog() {
        pd = ProgressDialog(this)
        pd.setMessage("加载中，请稍后")
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        pd.setCanceledOnTouchOutside(false)
        pd.setCancelable(true)
    }

    fun loadDialog() {
        pd?.show()
    }

    fun removeDialog() {
        pd?.let {
            if (pd.isShowing) {
                pd.dismiss()
            }
        }
    }

    private fun getSearchList(type: Int) {
        if (this == null || this.isFinishing) return
        if (type == LOAD_REFRESH) {
            page = 1
            recordList.clear()
        }
        val url = "http://bt.xiandan.in/api/search"
        OkGo.get<String>(url)
                .params("source", source)
                .params("keyword", et_key.text.toString().trim())
                .params("page", page)
                .execute(object : StringCallback() {
                    override fun onSuccess(response: Response<String>) {
                        Log.e("JSON onSuccess", response?.body().toString())
                        var recordBean = JSON.parseObject(response?.body().toString(), RecordBean::class.java) as RecordBean
                        var list = recordBean.results
                        recordList.addAll(list)
                        recordAdapter.resetData(recordList)
                        removeDialog()
                        if (type == LOAD_REFRESH) {
                            refreshLayout.finishRefresh()
                        } else {
                            refreshLayout.finishLoadMore()
                        }
                    }

                    override fun onError(response: Response<String>) {
                        super.onError(response)
                    }
                })

    }

    private fun isMagnet(magnet: String): Boolean {
        return Pattern.matches("^(magnet:\\?xt=urn:btih:)[0-9a-fA-F]{40}.*$", magnet)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                decodeTorrent()
            } else {
                showNoPermissionDialog(this, "文件读写")
            }
        }
    }

    private fun showNoPermissionDialog(context: Context, str: String) {
        AlertDialog.Builder(context).setTitle("获取" + str + "权限被禁用")
                .setMessage("请在 设置-应用管理-" + context.getString(R.string.app_name) + "-权限管理 (将" + str + "权限打开)")
                .setNegativeButton("取消", null)
                .setPositiveButton("去设置", DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                }).show()
    }

    private fun decodeTorrent() {
        Snackbar.make(toolbar, "正在解析种子", Snackbar.LENGTH_LONG).show()
//            val address = InetAddress.getLocalHost()
//            var newFile = File(Environment.getExternalStorageDirectory().absolutePath + "/newFile")
//            Util.writeBytesToFile(assets.open("test.torrent"), newFile)
//            val torrent = SharedTorrent.fromFile(newFile, File(Environment.getExternalStorageDirectory().absolutePath))
//            val client = Client(address, torrent)
//            client.download()
        var `is` = assets.open("test.torrent")
        var file = File(rootPath + "/bt.torrent")
//        if (!file.exists()) {
//            file.mkdirs()
//        }
        Util.writeBytesToFile(`is`, file)
        var torrent = Torrent.load(file)
        Log.e("torrent: name:", torrent.name)
        Log.e("torrent: comment:", torrent.comment)
        Log.e("torrent: createdBy:", torrent.createdBy)
        Log.e("torrent: hexInfoHash:", torrent.hexInfoHash)
        Log.e("torrent: announceList:", torrent.announceList.toString())
        Log.e("torrent: size:", torrent.size.toString())
        Log.e("torrent: isSeeder:", torrent.isSeeder.toString())
        Log.e("torrent: trackerCount:", torrent.trackerCount.toString())

//        val tracker = Tracker(6969)
//        val filter = FilenameFilter { dir, name -> name.endsWith(".torrent") }
//        for (f in File("/path/to/torrent/files").listFiles(filter)) {
//            tracker.announce(TrackedTorrent.load(f))
//        }
//        tracker.setAcceptForeignTorrents(true)
//        tracker.start(true);
    }

}
