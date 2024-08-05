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
    private lateinit var buttons: MutableList<UnderlayButton>
    private lateinit var gestureDetector: GestureDetector
    private var swipedPos = -1
    private var swipeThreshold = 0.5f
    private val buttonsBuffer: MutableMap<Int, MutableList<UnderlayButton>>
    private lateinit var recoverQueue: Queue<Int>
    private var isMoved: Boolean

    interface SimpleCallbackListener {
        fun onSimpleCallbackMove(
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean

        fun clearSimpleCallbackView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder)
    }

    private val gestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            for (button in buttons) {
                if (button.onClick(e.x, e.y)) break
            }
            return true
        }
    }
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

    fun resetSwipePos() {
        swipedPos = -1
    }

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

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (isMoved) {
            simpleCallbackListener.clearSimpleCallbackView(recyclerView, viewHolder)
        }
        isMoved = false
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.1f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 5.0f * defaultValue
    }

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

    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    abstract fun instantiateUnderlayButton(
        viewHolder: RecyclerView.ViewHolder,
        underlayButtons: MutableList<UnderlayButton>
    )

    class UnderlayButton(
        private val text: String,
        private val imageRes: Bitmap,
        private val color: Int,
        private val viewHolder: RecyclerView.ViewHolder,
        private val clickListener: UnderlayButtonClickListener
    ) {
        private var pos = 0
        private var clickRegion: RectF? = null
        fun onClick(x: Float, y: Float): Boolean {
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                clickListener.onClick(viewHolder, pos)
                return true
            }
            return false
        }

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

    interface UnderlayButtonClickListener {
        fun onClick(holder: RecyclerView.ViewHolder, pos: Int)
    }

    companion object {
        private const val BUTTON_WIDTH = 60
        private const val FONT_SIZE = 12
        private var BUTTON_WIDTH_DP by Delegates.notNull<Int>()
        private var FONT_SIZE_DP by Delegates.notNull<Int>()
    }
}