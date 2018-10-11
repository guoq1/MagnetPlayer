package com.guoqi.magnetplayer.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.guoqi.magnetplayer.R
import com.guoqi.magnetplayer.bean.RecordBean


class RecordAdapter(var context: Context, var datas: ArrayList<RecordBean.Results>) : BaseAdapter() {
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
    fun resetData(datas: ArrayList<RecordBean.Results>) {
        this.datas = datas
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
            viewHolder.ll_item = convertView!!.findViewById<View>(R.id.ll_item) as LinearLayout
            viewHolder.tv_title = convertView!!.findViewById<TextView>(R.id.tv_title) as TextView
            viewHolder.tv_date = convertView!!.findViewById<TextView>(R.id.tv_date) as TextView
            viewHolder.tv_size = convertView!!.findViewById<TextView>(R.id.tv_size) as TextView
            viewHolder.tv_resolution = convertView!!.findViewById<TextView>(R.id.tv_resolution) as TextView
            viewHolder.tv_copy = convertView!!.findViewById<TextView>(R.id.tv_copy) as TextView
            convertView.tag = viewHolder
        }
        var bean = datas[position]
        viewHolder.tv_title?.text = bean?.name
        viewHolder.tv_date?.text = bean?.count
        viewHolder.tv_size?.text = bean.formatSize
        viewHolder.tv_resolution?.text = bean.resolution
        viewHolder.tv_copy?.setOnClickListener {
            var cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm?.let {
                cm.primaryClip = ClipData.newPlainText(null, bean?.magnet)
                Toast.makeText(context, "磁链已复制", Toast.LENGTH_SHORT).show()
            }
        }
        return convertView
    }

    internal class ViewHolder {
        var ll_item: LinearLayout? = null
        var tv_title: TextView? = null
        var tv_date: TextView? = null
        var tv_size: TextView? = null
        var tv_resolution: TextView? = null
        var tv_copy: TextView? = null
    }


}

