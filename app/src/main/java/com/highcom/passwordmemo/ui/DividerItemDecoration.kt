package com.highcom.passwordmemo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * リサイクラービューのアイテムに対する分割線の描画クラス
 *
 * @constructor
 * コンストラクタ
 *
 * @param context コンテキスト
 * @param orientation リスト方向
 */
class DividerItemDecoration(context: Context, orientation: Int) : ItemDecoration() {
    /** 分割線描画 */
    private val mDivider: Drawable?
    /** リスト方向 */
    private var mOrientation = 0

    init {
        val a = context.obtainStyledAttributes(ATTRS)
        mDivider = a.getDrawable(0)
        a.recycle()
        setOrientation(orientation)
    }

    /**
     * リスト方向設定処理
     *
     * @param orientation リスト方向
     */
    private fun setOrientation(orientation: Int) {
        require(!(orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST)) { "invalid orientation" }
        mOrientation = orientation
    }

    /**
     * 描画処理
     *
     * @param c 描画用キャンバス
     * @param parent 親のビュー
     * @param state 操作状態
     */
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    /**
     * 垂直方向リストの分割線描画処理
     *
     * @param c 描画用キャンバス
     * @param parent 親ビュー
     */
    private fun drawVertical(c: Canvas?, parent: RecyclerView) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) { // 最後の行に対する線は描画しない
            val child = parent.getChildAt(i)
            val params = child
                .layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + mDivider!!.intrinsicHeight
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c!!)
        }
    }

    /**
     * 水平方向リストの分割線描画処理
     *
     * @param c 描画用キャンバス
     * @param parent 親ビュー
     */
    private fun drawHorizontal(c: Canvas?, parent: RecyclerView) {
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) { // 最後の行に対する線は描画しない
            val child = parent.getChildAt(i)
            val params = child
                .layoutParams as RecyclerView.LayoutParams
            val left = child.right + params.rightMargin
            val right = left + mDivider!!.intrinsicHeight
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c!!)
        }
    }

    /**
     * アイテムに対するオフセット位置取得処理
     *
     * @param outRect 外枠
     * @param itemPosition アイテム位置
     * @param parent 親ビュー
     */
    @Deprecated("Deprecated in Java")
    override fun getItemOffsets(outRect: Rect, itemPosition: Int, parent: RecyclerView) {
        if (mOrientation == VERTICAL_LIST) {
            outRect[0, 0, 0] = mDivider!!.intrinsicHeight
        } else {
            outRect[0, 0, mDivider!!.intrinsicWidth] = 0
        }
    }

    companion object {
        /** 分割線の属性 */
        private val ATTRS = intArrayOf(
            android.R.attr.listDivider
        )
        /** 水平方向リスト値 */
        const val HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL
        /** 垂直方向リスト値 */
        const val VERTICAL_LIST = LinearLayoutManager.VERTICAL
    }
}