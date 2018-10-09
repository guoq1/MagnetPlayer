package com.guoqi.magnetplayer

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
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
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.scwang.smartrefresh.layout.footer.ClassicsFooter
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.dialog_text_input.view.*
import java.io.File
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    companion object {
        //source=种子搜&keyword=测试&page=1
        val rootPath = Environment.getExternalStorageDirectory().absolutePath + File.separator + "MagnetPlayer"
        const val LOAD_REFRESH = 1001
        const val LOAD_MORE = 1002

        private val TAG_ADD_LINK_DIALOG = "add_link_dialog"
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

        fab.setOnClickListener { _ ->
            showAddLinkDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
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

    private fun loadDialog() {
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


    private fun showAddLinkDialog() {
        if (!this.isFinishing) {
            val inputView = layoutInflater.inflate(R.layout.dialog_text_input, null)
            var addLinkDialog = AlertDialog.Builder(this)
                    .setTitle("添加磁链")
                    .setView(inputView)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val link = inputView.et.text.toString()
                        var url: String = ""
                        when {
                            link.startsWith(Utils.MAGNET_PREFIX) -> url = link
                            Utils.isHash(link) -> url = Utils.normalizeMagnetHash(link)
                            else -> {
                                Snackbar.make(inputView, "磁链不正确, 请检查", Snackbar.LENGTH_LONG).show()
                            }
                        }

                        if (!url.isEmpty()) {
                            val i = Intent(this, DownloadActivity::class.java)
                            i.putExtra(DownloadActivity.TAG_URI, Uri.parse(url))
                            startActivity(i)
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                    }
            addLinkDialog.show()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                showNoPermissionDialog(this, "文件读写")
            }
        }
    }

    private fun showNoPermissionDialog(context: Context, str: String) {
        AlertDialog.Builder(context).setTitle("获取" + str + "权限被禁用")
                .setMessage("请在 设置-应用管理-" + context.getString(R.string.app_name) + "-权限管理 (将" + str + "权限打开)")
                .setNegativeButton("取消", null)
                .setPositiveButton("去设置") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                }.show()
    }


}
