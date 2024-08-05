package com.highcom.passwordmemo.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.annotation.ColorInt
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.list.BackgroundColorAdapter
import com.highcom.passwordmemo.ui.list.BackgroundColorItem

@Suppress("DEPRECATION")
class BackgroundColorUtil(context: Context, listener: BackgroundColorListener?) {
    private val colors: IntArray = IntArray(8)
    private var backgroundColorListener: BackgroundColorListener?

    interface BackgroundColorListener {
        fun onSelectColorClicked(color: Int)
    }

    init {
        colors[0] = context.resources.getColor(R.color.white)
        colors[1] = context.resources.getColor(R.color.lightgray)
        colors[2] = context.resources.getColor(R.color.lightcyan)
        colors[3] = context.resources.getColor(R.color.lavender)
        colors[4] = context.resources.getColor(R.color.bisque)
        colors[5] = context.resources.getColor(R.color.pink)
        colors[6] = context.resources.getColor(R.color.palegoldenrod)
        colors[7] = context.resources.getColor(R.color.palegreen)
        backgroundColorListener = listener
    }

    fun isColorExists(@ColorInt color: Int): Boolean {
        for (i in 0..7) {
            if (colors[i] == color) return true
        }
        return false
    }

    @SuppressLint("ResourceType", "InflateParams")
    fun createBackgroundColorDialog(activity: Activity): AlertDialog {
        val alertDialog = AlertDialog.Builder(activity)
            .setView(activity.layoutInflater.inflate(R.layout.alert_background_color, null))
            .create()
        alertDialog.show()
        val listView = alertDialog.findViewById<ListView>(R.id.backgroundColorListView)
        val colorItems: MutableList<BackgroundColorItem> = ArrayList()
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_white), colors[0]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_lightgray), colors[1]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_lightcyan), colors[2]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_lavender), colors[3]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_bisque), colors[4]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_pink), colors[5]))
        colorItems.add(
            BackgroundColorItem(
                activity.getString(R.string.color_palegoldenrod),
                colors[6]
            )
        )
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_palegreen), colors[7]))
        val adapter = BackgroundColorAdapter(activity, R.layout.row_background_color, colorItems)
        listView.adapter = adapter
        listView.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            backgroundColorListener!!.onSelectColorClicked(colors[i])
            alertDialog.dismiss()
        }
        return alertDialog
    }
}