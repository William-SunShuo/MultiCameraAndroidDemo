package com.blink.monitor.composeable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blink.monitor.R
import com.blink.monitor.extention.JoystickDirection
import kotlin.math.abs

@Composable
fun JoystickView(outerRadius: Float, onDirectionChange: (JoystickDirection) -> Unit) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var currentDirection by remember { mutableStateOf(JoystickDirection.Release) }

    Box(
        Modifier
            .width(outerRadius.dp)
            .height(outerRadius.dp), contentAlignment = Alignment.Center
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(onDrag = { change, dragAmount ->
                        change.consume() // 消耗手势事件
                        val newOffset = dragOffset + dragAmount

                        if (newOffset.getDistance() <= size.width / 2 - size.width / 6) {
                            dragOffset = newOffset

                            // 判断滑动方向
                            val xAbs = abs(dragOffset.x)
                            val yAbs = abs(dragOffset.y)

                            val direction = when {
                                xAbs > yAbs && dragOffset.x > 0 -> JoystickDirection.Right
                                xAbs > yAbs && dragOffset.x < 0 -> JoystickDirection.Left
                                yAbs > xAbs && dragOffset.y > 0 -> JoystickDirection.Down
                                yAbs > xAbs && dragOffset.y < 0 -> JoystickDirection.Up
                                else -> null
                            }

                            direction?.let {
                                currentDirection = it
                                onDirectionChange(it)
                            }
                        }
                    }, onDragEnd = {
                        // 手势结束时将圆点恢复到中心
                        dragOffset = Offset.Zero
                        currentDirection = JoystickDirection.Release
                        onDirectionChange(JoystickDirection.Release)
                    }, onDragCancel = {
                        // 手势结束时将圆点恢复到中心
                        dragOffset = Offset.Zero
                        currentDirection = JoystickDirection.Release
                        onDirectionChange(JoystickDirection.Release)
                    })
                }) {
            drawJoystick(dragOffset)
        }
        Image(//上
            painter = painterResource(
                if (currentDirection == JoystickDirection.Up) R.drawable.ic_direction_arrow_solid else R.drawable.ic_direction_arrow_opacity
            ), "",
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20f.dp)
                .size(24f.dp, 12f.dp)
        )
        Image(//左
            painter = painterResource(
                if (currentDirection == JoystickDirection.Left) R.drawable.ic_direction_arrow_solid else R.drawable.ic_direction_arrow_opacity
            ),
            "",
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20f.dp)
                .size(24f.dp, 12f.dp)
                .rotate(270f)
        )
        Image(//右
            painter = painterResource(
                if (currentDirection == JoystickDirection.Right) R.drawable.ic_direction_arrow_solid else R.drawable.ic_direction_arrow_opacity
            ),
            "",
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20f.dp)
                .size(24f.dp, 12f.dp)
                .rotate(90f)

        )
        Image(//下
            painter = painterResource(
                if (currentDirection == JoystickDirection.Down) R.drawable.ic_direction_arrow_solid else R.drawable.ic_direction_arrow_opacity
            ),
            "",
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20f.dp)
                .size(24f.dp, 12f.dp)
                .rotate(180f)
        )
    }
}

private fun DrawScope.drawJoystick(
    dragOffset: Offset,
) {
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = size.width / 2 - 4f.dp.toPx(),
        center = Offset(size.width / 2, size.height / 2),
        style = Stroke(width = 4f.dp.toPx())
    )

    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = size.width / 2 - (4f.dp + 4f.dp).toPx(),
        center = Offset(size.width / 2, size.height / 2)
    )

    drawCircle(
        color = Color.White,
        radius = 44f.dp.toPx() / 2,
        center = Offset(size.width / 2, size.height / 2) + dragOffset
    )
}

@Preview
@Composable
private fun preview() {
    JoystickView(160f, {})
}