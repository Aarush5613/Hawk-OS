package com.hawkos.launcher

import android.content.Context
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
        tv.text = "  ${app.label}"
        tv.setTextColor(android.graphics.Color.parseColor("#00FF9F"))
        tv.textSize = 14f
        tv.typeface = android.graphics.Typeface.MONOSPACE
        tv.setPadding(16, 20, 16, 20)
        tv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
