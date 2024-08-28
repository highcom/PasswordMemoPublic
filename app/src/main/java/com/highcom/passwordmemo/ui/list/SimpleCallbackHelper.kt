package com.highcom.passwordmemo.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.highcom.passwordmemo.R
import java.util.LinkedList
import java.util.Queue
import kotlin.properties.Delegates

/**
 * リサイクラービューに対するタッチ操作のヘルパークラス
 *
 * @property recyclerView 対象のビュー全体
 * @property simpleCallbackListener タッチされた対象アイテムのリスナー
 * @constructor
 * タッチ操作のヘルパーコンストラクタ
 *
 * @param context コンテキスト
 * @param scale スケール
 */
@SuppressLint("ClickableViewAccessibility")
abstract class SimpleCallbackHelper(
    context: Context?,
    private val recyclerView: RecyclerView?,
    scale: Float,
    private val simpleCallbackListener: SimpleCallbackListener
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    ItemTouchHelper.LEFT
) {
    /** スワイプボタン */
    private lateinit var buttons: MutableList<UnderlayButton>
    /** 操作検出 */
    private lateinit var gestureDetector: GestureDetector
    /** スワイプしたビューの位置 */
    private var swipedPos = -1
    /** スワイプの閾値 */
    private var swipeThreshold = 0.5f
    /** ボタン描画用バッファ */
    private val buttonsBuffer: MutableMap<Int, MutableList<UnderlayButton>>
    /** 以前にスワイプしたリカバリ用のビューの位置 */
    private lateinit var recoverQueue: Queue<Int>
    /** 移動したか */
    private var isMoved: Boolean

    /**
     * タッチされた対象アイテムのリスナー
     *
     */
    interface SimpleCallbackListener {
        /**
         * 移動中の位置に対する通知イベント
         *
         * @param viewHolder 移動元ビューホルダー
         * @param target 移動先ビューホルダー
         * @return 移動可能可否
         */
        fun onSimpleCallbackMove(
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean

        /**
         * 移動完了後の通知イベント
         *
         * @param recyclerView ビュー全体
         * @param viewHolder 移動元ビューホルダー
         */
        fun clearSimpleCallbackView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder)
    }

    /** 操作イベントリスナー */
    private val gestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        /**
         * シングルタップによる確定イベント
         *
         * @param e 操作イベント
         * @return 確定可否
         */
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            for (button in buttons) {
                if (button.onClick(e.x, e.y)) break
            }
            return true
        }
    }

    /** タッチ操作に対するリスナー */
    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = OnTouchListener { _, e ->
        if (swipedPos < 0) return@OnTouchListener false
        val point = Point(e.rawX.toInt(), e.rawY.toInt())
        val swipedViewHolder = recyclerView!!.findViewHolderForAdapterPosition(swipedPos)
            ?: return@OnTouchListener false
        val swipedItem = swipedViewHolder.itemView
        val rect = Rect()
        swipedItem.getGlobalVisibleRect(rect)
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_MOVE) {
            if (rect.top < point.y && rect.bottom > point.y) gestureDetector.onTouchEvent(e) else {
                recoverQueue.add(swipedPos)
                swipedPos = -1
                recoverSwipedItem()
            }
        }
        false
    }

    init {
        BUTTON_WIDTH_DP = (BUTTON_WIDTH * scale).toInt()
        FONT_SIZE_DP = (FONT_SIZE * scale).toInt()
        buttons = ArrayList()
        gestureDetector = GestureDetector(context, gestureListener)
        recyclerView!!.setOnTouchListener(onTouchListener)
        isMoved = false
        buttonsBuffer = HashMap()
        recoverQueue = object : LinkedList<Int>() {
            override fun add(element: Int): Boolean {
                return if (contains(element)) false else super.add(element)
            }
        }
        attachSwipe()
    }

    /**
     * スワイプしたビューの位置のリセット処理
     *
     */
    fun resetSwipePos() {
        swipedPos = -1
    }

    /**
     * 移動中の位置に対する通知処理
     *
     * @param recyclerView ビュー全体
     * @param viewHolder 移動元ビューホルダー
     * @param target 移動先ビューホルダー
     * @return 移動可能可否
     */
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return if (target.itemView.id == R.id.row_footer) false else simpleCallbackListener.onSimpleCallbackMove(
            viewHolder,
            target
        )
    }

    /**
     * 移動完了後の通知処理
     *
     * @param recyclerView ビュー全体
     * @param viewHolder 移動元ビューホルダー
     * @param fromPos 移動元位置
     * @param target 移動先ビューホルダー
     * @param toPos 移動先位置
     * @param x 横軸座標
     * @param y 縦軸座標
     */
    override fun onMoved(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        fromPos: Int,
        target: RecyclerView.ViewHolder,
        toPos: Int,
        x: Int,
        y: Int
    ) {
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
        isMoved = true
    }

    /**
     * スワイプ操作通知処理
     *
     * @param viewHolder 対象のビューホルダー
     * @param direction スワイプ方向
     */
    @Suppress("DEPRECATION")
    @SuppressLint("ResourceType")
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (viewHolder.itemView.id == R.id.row_footer) return
        val pos = viewHolder.adapterPosition
        if (swipedPos != pos) recoverQueue.add(swipedPos)
        swipedPos = pos
        if (buttonsBuffer.containsKey(swipedPos)) buttons =
            buttonsBuffer[swipedPos]!! else buttons.clear()
        buttonsBuffer.clear()
        swipeThreshold = 0.5f * buttons.size * BUTTON_WIDTH_DP
        recoverSwipedItem()
    }

    /**
     * 移動完了後の通知処理
     *
     * @param recyclerView ビュー全体
     * @param viewHolder 移動元ビューホルダー
     */
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (isMoved) {
            simpleCallbackListener.clearSimpleCallbackView(recyclerView, viewHolder)
        }
        isMoved = false
    }

    /**
     * スワイプの閾値取得処理
     *
     * @param viewHolder スワイプ対象のビューホルダー
     * @return スワイプの閾値
     */
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    /**
     * スワイプ速度取得処理
     *
     * @param defaultValue 標準の速度値
     * @return 変換後の速度値
     */
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.1f * defaultValue
    }

    /**
     * スワイプ速度判定閾値取得処理
     *
     * @param defaultValue 標準の閾値
     * @return 変換後の閾値
     */
    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 5.0f * defaultValue
    }

    /**
     * 対象ビューの操作中の表示イベント
     * スワイプすると表示されるボタンの描画イベントとして処理する
     *
     * @param c 描画用のキャンバス
     * @param recyclerView ビュー全体
     * @param viewHolder 操作対象のビューホルダー
     * @param dX 横方向移動量
     * @param dY 縦方向移動量
     * @param actionState 操作イベント種別
     * @param isCurrentlyActive アクティブな操作かどうか
     */
    @Suppress("DEPRECATION")
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.adapterPosition
        var translationX = dX
        val itemView = viewHolder.itemView
        if (pos < 0) {
            swipedPos = pos
            return
        }
        // 左方向へのスワイプ操作の場合はボタンを描画する
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                var buffer: MutableList<UnderlayButton> = ArrayList()
                if (!buttonsBuffer.containsKey(pos)) {
                    instantiateUnderlayButton(viewHolder, buffer)
                    buttonsBuffer[pos] = buffer
                } else {
                    buffer = buttonsBuffer[pos]!!
                }
                translationX = dX * buffer.size * BUTTON_WIDTH_DP / itemView.width
                drawButtons(c, itemView, buffer, pos, translationX)
            }
        }
        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            translationX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    /**
     * スワイプ状態を元に戻すアイテムへの通知処理
     *
     */
    @Synchronized
    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val pos = recoverQueue.poll()
            if (pos != null) {
                if (pos > -1) {
                    recyclerView!!.adapter!!.notifyItemChanged(pos)
                }
            }
        }
    }

    /**
     * スワイプ時に表示するボタンの描画処理
     *
     * @param c 描画用のキャンバス
     * @param itemView 描画対象のビュー
     * @param buffer 描画用バッファ
     * @param pos 描画位置
     * @param dX 横方向移動量
     */
    private fun drawButtons(
        c: Canvas,
        itemView: View,
        buffer: List<UnderlayButton>,
        pos: Int,
        dX: Float
    ) {
        var right = itemView.right.toFloat()
        val dButtonWidth = -1 * dX / buffer.size
        for (button in buffer) {
            val left = right - dButtonWidth
            button.onDraw(
                c,
                RectF(
                    left,
                    itemView.top.toFloat(),
                    right,
                    itemView.bottom
                        .toFloat()
                ),
                pos
            )
            right = left
        }
    }

    /**
     * スワイプ操作のアタッチ処理
     *
     */
    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    /**
     * スワイプ操作時のボタン描画初期化処理
     *
     * @param viewHolder 対象のビューホルダー
     * @param underlayButtons 描画ボタンのリスト
     */
    abstract fun instantiateUnderlayButton(
        viewHolder: RecyclerView.ViewHolder,
        underlayButtons: MutableList<UnderlayButton>
    )

    /**
     * 描画ボタンクラス
     *
     * @property text 表示テキスト
     * @property imageRes 表示イメージ
     * @property color 表示色
     * @property viewHolder 対象のビューホルダー
     * @property clickListener クリックイベントリスナー
     */
    class UnderlayButton(
        private val text: String,
        private val imageRes: Bitmap,
        private val color: Int,
        private val viewHolder: RecyclerView.ViewHolder,
        private val clickListener: UnderlayButtonClickListener
    ) {
        /** 描画対象の位置 */
        private var pos = 0
        /** クリック位置情報 */
        private var clickRegion: RectF? = null

        /**
         * クリック操作イベント処理
         *
         * @param x 横軸位置
         * @param y 縦軸位置
         * @return クリック可否
         */
        fun onClick(x: Float, y: Float): Boolean {
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                clickListener.onClick(viewHolder, pos)
                return true
            }
            return false
        }

        /**
         * ボタン描画処理
         *
         * @param c 描画用キャンバス
         * @param rect 描画対象の矩形
         * @param pos 描画対象の位置
         */
        fun onDraw(c: Canvas, rect: RectF, pos: Int) {
            val p = Paint()

            // Draw background
            p.color = color
            c.drawRect(rect, p)

            // Draw Text
            p.color = Color.WHITE
            p.textSize = FONT_SIZE_DP.toFloat()
            val r = Rect()
//            val cHeight = rect.height()
//            val cWidth = rect.width()
            p.textAlign = Paint.Align.LEFT
            p.getTextBounds(text, 0, text.length, r)
            // テキストを表示する場合はここを有効にする
//            float x = cWidth / 2f - r.width() / 2f - r.left;
//            float y = cHeight / 2f + r.height() / 2f - r.bottom;
//            c.drawText(text, rect.left + x, rect.top + y, p);
            // 画像を表示する
            val imgRect: RectF = if (rect.right - rect.left > rect.bottom - rect.top) {
                // 横が長くなった場合には高さに合わせてクリップする
                val center = rect.left + (rect.right - rect.left) / 2
                val span = (rect.bottom - rect.top) / 2
                RectF(center - span, rect.top, center + span, rect.bottom)
            } else {
                RectF(rect.left, rect.top, rect.right, rect.bottom)
            }
            c.drawBitmap(imageRes, null, imgRect, p)
            clickRegion = rect
            this.pos = pos
        }
    }

    /**
     * 描画ボタンクリック用リスナークラス
     *
     */
    interface UnderlayButtonClickListener {
        /**
         * クリックイベント通知
         *
         * @param holder 対象のビューホルダー
         * @param pos 対象の位置
         */
        fun onClick(holder: RecyclerView.ViewHolder, pos: Int)
    }

    companion object {
        /** 描画ボタンの横幅 */
        private const val BUTTON_WIDTH = 60
        /** 描画ボタンに表示するテキストサイズ */
        private const val FONT_SIZE = 12
        /** 描画ボタンの横幅のDP変換値 */
        private var BUTTON_WIDTH_DP by Delegates.notNull<Int>()
        /** 描画ボタンに表示するテキストサイズのDP変換値 */
        private var FONT_SIZE_DP by Delegates.notNull<Int>()
    }
}