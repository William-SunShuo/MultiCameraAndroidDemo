package com.blink.monitor.view

import android.annotation.SuppressLint
import android.content.Context

import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ArrayRes
import com.blink.monitor.R

/**
 * 取色进度条;
 */
class ColorSeekBar(context: Context, attributeSet: AttributeSet): View(context, attributeSet){

    private val minThumbRadius = 20f
    private var colorSeeds = intArrayOf(
        Color.parseColor("#FF0000"),
        Color.parseColor("#FF3C00"),
        Color.parseColor("#FF8C00"),
        Color.parseColor("#FFCC00"),
        Color.parseColor("#DDFF00"),
        Color.parseColor("#77FF00"),
        Color.parseColor("#00FF1E"),
        Color.parseColor("#00FFBF"),

        Color.parseColor("#00DDFF"),
        Color.parseColor("#00AEFF"),
        Color.parseColor("#0090FF"),
        Color.parseColor("#0000FF"),

        Color.parseColor("#5E00FF"),
        Color.parseColor("#9000FF"),
        Color.parseColor("#E100FF"),
        Color.parseColor("#FF0077"),
        Color.parseColor("#FF0004")

    )
    private var canvasHeight: Int = 60
    private var barHeight: Int = 20
    private var rectf: RectF = RectF()
    private var rectPaint: Paint = Paint()
    private var thumbBorderPaint: Paint = Paint()
    private var thumbPaint: Paint = Paint()
    private lateinit var colorGradient: LinearGradient
    private var thumbX: Float = 10f
    private var thumbY: Float = (canvasHeight/2).toFloat()

    private var thumbRadius: Float = 16f

    private var thumbBorderColor = Color.BLACK
    private var paddingStart = 0f
    private var paddingEnd = 0f
    private var barCornerRadius: Float = 8f

    private var colorChangeListener: SeekBarColorChangeListener? = null

    init {
        attributeSet.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ColorSeekBar)
            val colorsId = typedArray.getResourceId(R.styleable.ColorSeekBar_colorSeeds, 0)
            if (colorsId != 0) colorSeeds = getColorsById(colorsId)
            barCornerRadius = typedArray.getDimension(R.styleable.ColorSeekBar_cornerRadius, 8f)
            barHeight = typedArray.getDimension(R.styleable.ColorSeekBar_barHeight, 20f).toInt()
            typedArray.recycle()
        }
        rectPaint.isAntiAlias = true

        thumbBorderPaint.isAntiAlias = true
        thumbBorderPaint.color = thumbBorderColor

        thumbPaint.isAntiAlias = true

        thumbRadius = (barHeight / 2).toFloat().let {
            if (it < minThumbRadius) minThumbRadius else it
        }
        canvasHeight = (thumbRadius * 3).toInt()
        thumbY = (canvasHeight/2).toFloat()


    }

    private fun getColorsById(@ArrayRes id: Int): IntArray {
        if (isInEditMode) {
            val s = context.resources.getStringArray(id)
            val colors = IntArray(s.size)
            for (j in s.indices) {
                colors[j] = Color.parseColor(s[j])
            }
            return colors
        } else {
            val typedArray = context.resources.obtainTypedArray(id)
            val colors = IntArray(typedArray.length())
            for (j in 0 until typedArray.length()) {
                colors[j] = typedArray.getColor(j, Color.BLACK)
            }
            typedArray.recycle()
            return colors
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //color bar position
        val barLeft: Float = paddingStart
        val barRight: Float = width.toFloat() - paddingEnd
        val barTop: Float = ((canvasHeight / 2) - (barHeight / 2)).toFloat()
        val barBottom: Float = ((canvasHeight / 2) + (barHeight / 2)).toFloat()

        //画bar
        rectf.set(barLeft, barTop,barRight,barBottom)
        canvas.drawRoundRect(rectf, barCornerRadius, barCornerRadius, rectPaint)

        if (thumbX < barLeft){
            thumbX = barLeft
        }else if (thumbX > barRight){
            thumbX = barRight
        }
        val color = pickColor(thumbX, width)
        thumbPaint.color = color

        // draw color bar thumb
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint)
    }

    private fun pickColor(position: Float, canvasWidth: Int): Int {
        val value = (position - paddingStart) / (canvasWidth - (paddingStart + paddingEnd))
        when {
            value <= 0.0 -> return colorSeeds[0]
            value >= 1 -> return colorSeeds[colorSeeds.size - 1]
            else -> {
                var colorPosition = value * (colorSeeds.size - 1)
                val i = colorPosition.toInt()
                colorPosition -= i
                val c0 = colorSeeds[i]
                val c1 = colorSeeds[i + 1]

                val red = mix(Color.red(c0), Color.red(c1), colorPosition)
                val green = mix(Color.green(c0), Color.green(c1), colorPosition)
                val blue = mix(Color.blue(c0), Color.blue(c1), colorPosition)
                return Color.rgb( red, green, blue)
            }
        }
    }

    private fun mix(start: Int, end: Int, position: Float): Int {
        return start + Math.round(position * (end - start))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        colorGradient = LinearGradient(0f, 0f, w.toFloat(), 0f, colorSeeds, null, Shader.TileMode.CLAMP)
        rectPaint.shader = colorGradient
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, canvasHeight)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action){

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)
                event.x.let {
                    thumbX = it
                    invalidate()
                }
                post {
                    colorChangeListener?.colorChange(getColor())
                }
            }

        }
        return true
    }

    fun getColor()  = thumbPaint.color


    fun setOnColorChangeListener(onColorChangeListener: SeekBarColorChangeListener) {
        this.colorChangeListener = onColorChangeListener
    }

    interface SeekBarColorChangeListener {

        fun colorChange(color: Int)
    }
}