package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.databinding.RowDrawerGroupBinding
import com.highcom.passwordmemo.domain.TextSizeUtil

/**
 * ドロワーに表示するグループ一覧表示用アダプタ
 *
 * @property context コンテキスト
 * @property items グループ名称一覧
 */
class DrawerGroupListAdapter(
    private val context: Context,
    private val items: List<GroupEntity>
) : BaseAdapter() {
    /** レイアウト高さ設定マップデータ */
    private val layoutHeightMap: Map<Int, Float>
    /** 表示テキストサイズ */
    var textSize = 15f

    init {
        layoutHeightMap = object : HashMap<Int, Float>() {
            init {
                put(
                    TextSizeUtil.TEXT_SIZE_SMALL,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_SMALL)
                )
                put(
                    TextSizeUtil.TEXT_SIZE_MEDIUM,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_MEDIUM)
                )
                put(
                    TextSizeUtil.TEXT_SIZE_LARGE,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_LARGE)
                )
                put(
                    TextSizeUtil.TEXT_SIZE_EXTRA_LARGE,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_EXTRA_LARGE)
                )
            }
        }
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: RowDrawerGroupBinding = if (convertView == null) {
            // 新しいビューの作成
            RowDrawerGroupBinding.inflate(LayoutInflater.from(context), parent, false)
        } else {
            // 再利用
            RowDrawerGroupBinding.bind(convertView)
        }
        // 設定されたカラーにする
        val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_folder)?.mutate()
        if (items[position].color == 0) {
            drawable?.setTintList(null)
        } else {
            items[position].color.let { drawable?.setTint(it) }
        }
        binding.drawerImageIcon.setImageDrawable(drawable)
        // グループ名称を設定
        binding.drawerTitle.text = items[position].name
        // リストの高さと文字サイズを設定
        val layoutHeight = layoutHeightMap[textSize.toInt()]
        if (layoutHeight != null) {
            val params = binding.rowDrawerGroup.layoutParams
            params?.height = layoutHeight.toInt()
            binding.rowDrawerGroup.layoutParams = params
        }
        binding.drawerTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)

        return binding.root
    }

    /**
     * dpからpxへの変換処理
     *
     * @param context コンテキスト
     * @param dp 変換元dp値
     * @return 変換後px値
     */
    private fun convertFromDpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    companion object {
        /** テキストサイズが小の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_SMALL = 40f
        /** テキストサイズが中の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_MEDIUM = 45f
        /** テキストサイズが大の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_LARGE = 53f
        /** テキストサイズが特大の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_EXTRA_LARGE = 60f
    }
}