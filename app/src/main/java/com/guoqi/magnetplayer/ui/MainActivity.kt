package com.guoqi.magnetplayer.ui

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import com.alibaba.fastjson.JSON
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.adapter.RecordAdapter
import com.guoqi.magnetplayer.bean.RecordBean
import com.guoqi.magnetplayer.ui.DownloadActivity.Companion.IMG_FORMAT
import com.guoqi.magnetplayer.ui.DownloadActivity.Companion.MOV_FORMAT
import com.guoqi.magnetplayer.ui.DownloadActivity.Companion.REQUESTCODE_FROM_ACTIVITY
import com.guoqi.magnetplayer.ui.DownloadActivity.Companion.TAG_URI
import com.guoqi.magnetplayer.util.MagnetUtils
import com.leon.lfilepickerlibrary.LFilePicker
import com.leon.lfilepickerlibrary.utils.Constant
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.scwang.smartrefresh.layout.footer.ClassicsFooter
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_text_input.view.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.selector
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.io.File
import java.util.*
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    companion object {
        //source=种子搜&keyword=测试&page=1
        val rootPath = Environment.getExternalStorageDirectory().absolutePath + File.separator + "MagnetPlayer"
        const val LOAD_REFRESH = 1001
        const val LOAD_MORE = 1002
    }

    private lateinit var pd: ProgressDialog

    private var sourceArr = arrayOf("种子搜", "磁力吧", "BT兔子", "idope", "BTDB", "BT4G", "屌丝搜", "AOYOSO")
    private var source = sourceArr[0]
    private var SOURCE_TEMPLATE = """当前搜索源为：<font color=#FFFFFF>"%s"</font> ，搜索结果来自DHT网络。"""

    private var page = 1
    private var recordList = ArrayList<RecordBean.Results>()
    private lateinit var recordAdapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        initProgressDialog()
        initData()

    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
        }
    }


    private fun initData() {
        tv_source.text = Html.fromHtml(String.format(SOURCE_TEMPLATE, source))
        et_key.setOnEditorActionListener { _, i, _ ->
            if (et_key.text.toString().isEmpty()) {
                toolbar.snackbar(getString(R.string.please_input_search_key))
                true
            }
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                goSearch()
            }
            true
        }
        btn_search.setOnClickListener {
            if (et_key.text.toString().isEmpty()) {
                toolbar.snackbar(getString(R.string.please_input_search_key))
                return@setOnClickListener
            }
            goSearch()
        }
        fab.setOnClickListener { _ ->
            showAddLinkDialog()
        }

        recordAdapter = RecordAdapter(this, recordList)
        rv_record.adapter = recordAdapter
        rv_record.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rv_record.setmEmptyView(empty_view)


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
        recordAdapter.setMagnetCopyClickListener(View.OnClickListener {
            showAddLinkDialog()
        })

    }

    private fun goSearch() {
        loadDialog()
        getSearchList(LOAD_REFRESH)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                selector(getString(R.string.change_search_pot), sourceArr.toList()) { _, i ->
                    source = sourceArr[i]
                    tv_source.text = Html.fromHtml(String.format(SOURCE_TEMPLATE, source))
                    toolbar.snackbar(getString(R.string.shift_search_pot) + source)
                }
                true
            }
            R.id.action_download -> {
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
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initProgressDialog() {
        pd = ProgressDialog(this)
        pd.setMessage(getString(R.string.tip_loading))
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        pd.setCanceledOnTouchOutside(false)
        pd.setCancelable(true)
    }

    private fun loadDialog() {
        this?.let {
            pd?.show()
        }
    }

    fun removeDialog() {
        this?.let {
            pd?.let {
                if (pd.isShowing) {
                    pd.dismiss()
                }
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
                        recordBean.results?.let {
                            recordList.addAll(it)
                            recordAdapter.resetData(recordList, et_key.text.toString().trim())
                        }
                        removeDialog()
                        hideKeyBoard()
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
            inputView.et.setText(MagnetUtils.getClipboard(this))
            inputView.et.let { it.setSelection(it.text.toString().length) }

            var addLinkDialog = AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_magnet_link))
                    .setView(inputView)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val link = inputView.et.text.toString()
                        var url: String = ""
                        when {
                            link.startsWith(MagnetUtils.MAGNET_PREFIX) -> url = link
                            MagnetUtils.isHash(link) -> url = MagnetUtils.normalizeMagnetHash(link)
                            else -> {
                                inputView.longSnackbar(getString(R.string.magnet_is_wrong))
                            }
                        }

                        if (!url.isEmpty()) {
                            startActivity<DownloadActivity>(TAG_URI to url)
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


    fun hideKeyBoard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, HIDE_NOT_ALWAYS)
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
                        var intent = Intent(this@MainActivity, PhotoActivity::class.java)
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
        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
        intent.putExtra("url", """file://$path""")
        intent.putExtra("title", File(path.trim()).name)
        startActivity(intent)
    }
}
