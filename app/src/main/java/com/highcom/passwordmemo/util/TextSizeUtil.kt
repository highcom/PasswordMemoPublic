package com.highcom.passwordmemo.util

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.list.TextSizeAdapter

class TextSizeUtil(context: Context, listener: TextSizeListener) {
    private val textNames: Array<String?>

    interface TextSizeListener {
        fun onTextSizeSelected(size: Float)
    }

    private val mTextSizeAdapter: TextSizeAdapter
    private val mTextSizeListener: TextSizeListener

    init {
        textNames = arrayOfNulls(4)
        textNames[0] = context.getString(R.string.size_small)
        textNames[1] = context.getString(R.string.size_medium)
        textNames[2] = context.getString(R.string.size_large)
        textNames[3] = context.getString(R.string.size_extra_large)
        mTextSizeAdapter = TextSizeAdapter(context, R.layout.row_text_size, textNames, textSizes)
        mTextSizeListener = listener
    }

    fun createTextSizeSpinner(spinner: Spinner) {
        spinner.adapter = mTextSizeAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                mTextSizeListener.onTextSizeSelected(textSizes[position].toFloat())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun getSpecifiedValuePosition(size: Float): Int {
        var position = 1 // デフォルト値である「中」の位置
        for (i in textSizes.indices) {
            if (textSizes[i].toFloat() == size) {
                position = i
                break
            }
        }
        return position
    }

    companion object {
        const val TEXT_SIZE_SMALL = 12
        const val TEXT_SIZE_MEDIUM = 15
        const val TEXT_SIZE_LARGE = 18
        const val TEXT_SIZE_EXTRA_LARGE = 21
        private val textSizes =
            intArrayOf(TEXT_SIZE_SMALL, TEXT_SIZE_MEDIUM, TEXT_SIZE_LARGE, TEXT_SIZE_EXTRA_LARGE)
    }
}