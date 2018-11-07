package com.guoqi.magnetplayer.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.bean.RecordBean
import org.jetbrains.anko.toast

class RecordAdapter(private var mContext: Context, private var datas: ArrayList<RecordBean.Results>) : RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    private var keyWords: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(mContext, R.layout.adapter_result_item, null)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (datas.isEmpty()) 0 else datas.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        var bean = datas[position]
        if (keyWords.isNotEmpty() && bean.name.isNotEmpty() && bean.name.contains(keyWords)) {
            val index = bean.name.indexOf(keyWords)
            val len = keyWords.length
            val temp = Html.fromHtml(bean.name.substring(0, index)
                    + "<font color=#FF0000>"
                    + bean.name.substring(index, index + len) + "</font>"
                    + bean.name.substring(index + len, bean.name.length))

            viewHolder.tv_title!!.text = temp
        } else {
            viewHolder.tv_title!!.text = bean.name
        }


        viewHolder.tv_date?.text = bean.count
        viewHolder.tv_size?.text = bean.formatSize
        if (bean.resolution.isNotEmpty()) {
            viewHolder.tv_resolution?.text = bean.resolution
            viewHolder.tv_resolution?.setBackgroundResource(com.guoqi.magnetplayer.R.drawable.bg_tag_gray_trans)
        } else {
            viewHolder.tv_resolution?.text = ""
            viewHolder.tv_resolution?.setBackgroundResource(0)
        }

        viewHolder.ll_item?.setOnClickListener {
            var cm = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm?.let {
                cm.primaryClip = ClipData.newPlainText(null, bean?.magnet)
                mContext.toast("磁链已复制")
            }
            listener?.onClick(it)
        }
    }

    /**
     * 更新数据
     */
    fun resetData(datas: ArrayList<RecordBean.Results>, keyWords: String) {
        this.datas = datas
        this.keyWords = keyWords
        this.notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ll_item: LinearLayout = itemView.findViewById(R.id.ll_item)
        var tv_title: TextView = itemView.findViewById(R.id.tv_title)
        var tv_date: TextView = itemView.findViewById(R.id.tv_date)
        var tv_size: TextView = itemView.findViewById(R.id.tv_size)
        var tv_resolution: TextView = itemView.findViewById(R.id.tv_resolution)
        var tv_copy: TextView = itemView.findViewById(R.id.tv_copy)
    }


    lateinit var listener: View.OnClickListener
    fun setMagnetCopyClickListener(listener: View.OnClickListener) {
        this.listener = listener
    }

}


