package com.guoqi.magnetplayer.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.bean.RecordBean
import org.jetbrains.anko.toast


class RecordAdapter(var context: Context, var datas: ArrayList<RecordBean.Results>) : BaseAdapter() {

    private var keyWords: String = ""

    override fun getCount(): Int {
        return if (datas.isEmpty()) 0 else datas.size
    }

    override fun getItem(position: Int): RecordBean.Results? {
        return if (datas.isEmpty()) null else datas[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /**
     * 更新数据
     */
    fun resetData(datas: ArrayList<RecordBean.Results>, keyWords: String) {
        this.datas = datas
        this.keyWords = keyWords
        this.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView != null) {
            viewHolder = convertView.tag as ViewHolder
        } else {
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_item, null)
            viewHolder = ViewHolder()
            viewHolder.ll_item = convertView.findViewById<View>(R.id.ll_item) as LinearLayout
            viewHolder.tv_title = convertView.findViewById<TextView>(R.id.tv_title) as TextView
            viewHolder.tv_date = convertView.findViewById<TextView>(R.id.tv_date) as TextView
            viewHolder.tv_size = convertView.findViewById<TextView>(R.id.tv_size) as TextView
            viewHolder.tv_resolution = convertView.findViewById<TextView>(R.id.tv_resolution) as TextView
            viewHolder.tv_copy = convertView.findViewById<TextView>(R.id.tv_copy) as TextView
            convertView.tag = viewHolder
        }
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
            viewHolder.tv_resolution?.setBackgroundResource(R.drawable.bg_tag_gray_trans)
        } else {
            viewHolder.tv_resolution?.text = ""
            viewHolder.tv_resolution?.setBackgroundResource(0)
        }

        viewHolder.ll_item?.setOnClickListener {
            var cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm?.let {
                cm.primaryClip = ClipData.newPlainText(null, bean?.magnet)
                context.toast("磁链已复制")
            }
            listener?.onClick(it)
        }
        return convertView!!
    }

    internal class ViewHolder {
        var ll_item: LinearLayout? = null
        var tv_title: TextView? = null
        var tv_date: TextView? = null
        var tv_size: TextView? = null
        var tv_resolution: TextView? = null
        var tv_copy: TextView? = null
    }

    lateinit var listener: View.OnClickListener
    fun setMagnetCopyClickListener(listener: View.OnClickListener) {
        this.listener = listener
    }

}

