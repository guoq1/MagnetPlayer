package com.guoqi.magnetplayer

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
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
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        //source=种子搜&keyword=测试&page=1
        val filePath = Environment.getExternalStorageDirectory().absolutePath + File.separator + "MagnetPlayer"
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
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
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
}
