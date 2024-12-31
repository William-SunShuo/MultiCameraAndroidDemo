package com.blink.monitor.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.blink.monitor.extention.dp

/**
 * 取色面板控件，设置对应的饱和度和明暗数值;
 */
class PlateView @JvmOverloads constructor(context: Context,
                                          attrs: AttributeSet? = null,
                                          defStyleAttr: Int = 0):
    View(context, attrs, defStyleAttr) {

    //H- =代表色值，S -代表饱和度，V-明暗度
    private val HSV = floatArrayOf(1f, 1f, 1f)

    private var mPaint: Paint? = null
    private var mRectF: RectF? = null
    private var mShader: LinearGradient? = null

    var moveAction: ((View, Float, Float) -> Unit) ? = null



    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
       event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                    var x = it.x
                    var y = it.y

                    if(x < 0) x = 0f
                    if(x > measuredWidth) x = measuredWidth.toFloat()
                    //设置s向量值；颜色饱和
                    HSV[1] = 1.0f / measuredWidth * x

                    if(y > measuredHeight) y = measuredHeight.toFloat()
                    if(y < 0) y = 0f
                    //设置v向量值；代表颜色明暗
                    HSV[2] = 1.0f / measuredHeight * y

                    locationCursor(x, y)
                }
            }
       }

        return true
    }


    //初次进来落点在中间;
    fun middleState() {
        HSV[1] = 1.0f / 2
        HSV[2] = 1.0f / 2
        locationCursor(measuredWidth /2f, measuredHeight/2f)
    }

    //定位对应的图片控件位置.
    private fun locationCursor(x: Float, y: Float) {
        moveAction?.invoke(this, x, y)
    }


    fun getHSV(): FloatArray {
        return HSV
    }

    fun setHue(hue: Float) {
        HSV[0] = hue
        invalidate()
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mPaint == null) {
            mPaint = Paint()
            mPaint?.isAntiAlias = true
            mRectF = RectF()
            mShader = LinearGradient(
                0f, 0f, 0f,
                this.measuredHeight.toFloat(), -0x1, -0x1000000, Shader.TileMode.CLAMP
            ) //线性渐变
        }
        val rgb = Color.HSVToColor(HSV)
        val shaderHorizontal = LinearGradient(
            0f, 0f,
            this.measuredWidth.toFloat(), 0f, -0x1, rgb, Shader.TileMode.CLAMP
        )
        mShader?.also {
            val composeShader = ComposeShader(it, shaderHorizontal, PorterDuff.Mode.MULTIPLY) //混合渐变
            mPaint?.setShader(composeShader)
            mRectF?.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
            canvas.drawRoundRect(mRectF!!, 8.dp, 8.dp, mPaint!!)
        }
    }


}