package com.hawkos.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class AppAdapter(
    context: Context,
    private var apps: MutableList<MainActivity.AppInfo>
) : ArrayAdapter<MainActivity.AppInfo>(context, android.R.layout.simple_list_item_1, apps) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val tv = (convertView as? TextView) ?: TextView(context)
        val app = apps[position]
        tv.text = "> ${app.label.uppercase()}"
        tv.setTextColor(Color.parseColor("#00FF9F"))
        tv.textSize = 13f
        tv.typeface = Typeface.MONOSPACE
        tv.setPadding(32, 18, 32, 18)
        tv.setBackgroundColor(Color.TRANSPARENT)
        return tv
    }

    fun updateList(newList: List<MainActivity.AppInfo>) {
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getCount() = apps.size
    override fun getItem(position: Int) = apps[position]
}

