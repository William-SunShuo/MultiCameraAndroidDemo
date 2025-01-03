package com.blink.monitor.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.blink.monitor.R

/**
 * 取色器容器，
 */
class ColorPlateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var HSV = floatArrayOf(1f, 1f, 1f)

    var colorAction: ((Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.color_plate_container, this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onFinishInflate() {
        super.onFinishInflate()

//        val cursorView = findViewById<ImageView>(R.id.plate_cursor)
        val groupView = findViewById<View>(R.id.cursor_group)
        val plateView: PlateView = findViewById(R.id.plate)
        val cursor = findViewById<ImageView>(R.id.plate_cursor)

        plateView.apply {
            moveAction = { _, eventX, eventY, sv ->
                /**
                 * 1. x,y点位手势落点，相对于当前控件的顶点;
                 * 2. xml中对x-y点到控件顶点做了映射：
                 */
                val cursorLayoutParams: MarginLayoutParams =
                    groupView.layoutParams as MarginLayoutParams
                cursorLayoutParams.leftMargin = eventX.toInt()
                cursorLayoutParams.topMargin = eventY.toInt()
                groupView.layoutParams = cursorLayoutParams

                updateHSV(sv)
                groupView.visibility = View.VISIBLE
                Log.d("color", "hsv:${HSV.toList()}")
                cursor.imageTintList = ColorStateList.valueOf(Color.HSVToColor(HSV))
                colorAction?.invoke(Color.HSVToColor(HSV))
            }
            groupView.visibility = View.GONE

        }

        val barView = findViewById<ColorSeekBar>(R.id.seek_bar)
        barView.setOnColorChangeListener(object :ColorSeekBar.SeekBarColorChangeListener {
            override fun colorChange(color: Int) {
                //update h;同时更新到面板;
                val barHsv = floatArrayOf(0f, 1f, 1f)
                Color.colorToHSV(color, barHsv)
                //清除原来的饱和度和明暗度;
                HSV = barHsv
                plateView.setHue(barHsv[0])
                groupView.visibility = View.GONE
                Log.d("color", "hsv:${HSV.toList()}")
                colorAction?.invoke(Color.HSVToColor(barHsv))
            }
        })
    }


    private fun updateHSV(hsv: FloatArray) {
        HSV[1] = hsv[0]
        HSV[2] = hsv[1]
    }


    interface OnColorChangeListener {

        fun onColorChange(colorInt: Int)
    }

}