package com.blink.monitor.composeable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import kotlin.coroutines.cancellation.CancellationException
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
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume() // 消耗手势事件
                            val newOffset = dragOffset + dragAmount

                            if (newOffset.getDistance() <= size.width / 2 - size.width / 6 - 24f.dp.toPx()) {
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
                        },
                        onDragEnd = {
                            // 手势结束时将圆点恢复到中心
                            dragOffset = Offset.Zero
                            currentDirection = JoystickDirection.Release
                            onDirectionChange(JoystickDirection.Release)
                        },
                        onDragCancel = {
                            // 手势结束时将圆点恢复到中心
                            dragOffset = Offset.Zero
                            currentDirection = JoystickDirection.Release
                            onDirectionChange(JoystickDirection.Release)
                        },
                    )
                }
        ) {
            drawJoystick(dragOffset)
        }
        //上
        ArrowClickable(
            onSelect = currentDirection == JoystickDirection.Up,
            direction = JoystickDirection.Up,
            {
                onDirectionChange(JoystickDirection.UpTap)
                currentDirection = JoystickDirection.Up
            }, {
                currentDirection = JoystickDirection.Release
            }
        )
        //下
        ArrowClickable(
            onSelect = currentDirection == JoystickDirection.Down,
            direction = JoystickDirection.Down,
            {
                onDirectionChange(JoystickDirection.DownTap)
                currentDirection = JoystickDirection.Down
            }, {
                currentDirection = JoystickDirection.Release
            }
        )
        //左
        ArrowClickable(
            onSelect = currentDirection == JoystickDirection.Left,
            direction = JoystickDirection.Left,
            {
                onDirectionChange(JoystickDirection.LeftTap)
                currentDirection = JoystickDirection.Left
            }, {
                currentDirection = JoystickDirection.Release
            }
        )
        //右
        ArrowClickable(
            onSelect = currentDirection == JoystickDirection.Right,
            direction = JoystickDirection.Right,
            {
                onDirectionChange(JoystickDirection.RightTap)
                currentDirection = JoystickDirection.Right
            }, {
                currentDirection = JoystickDirection.Release
            }
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
private fun Preview() {
    JoystickView(160f) {}
}

@Composable
private fun BoxScope.ArrowClickable(
    onSelect: Boolean,
    direction: JoystickDirection,
    onTap: () -> Unit,
    onTapUp: () -> Unit,
) {
    val (rotate, alignment) = when (direction) {
        JoystickDirection.Up -> {
            (0f to Alignment.TopCenter)
        }

        JoystickDirection.Down -> {
            (180f to Alignment.BottomCenter)
        }

        JoystickDirection.Left -> {
            (270f to Alignment.CenterStart)
        }

        JoystickDirection.Right -> {
            (90f to Alignment.CenterEnd)
        }

        else -> {
            (0f to Alignment.TopCenter)
        }
    }
    Box(
        Modifier
            .align(alignment)
            .size(40f.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            onTap()
                            awaitRelease()
                            onTapUp()
                        } catch (e: CancellationException) {
                            // 如果手势被取消
                            println("Gesture was cancelled")
                            onTapUp()

                        }

                    }
                )
            }
    ) {

        Image(
            painter = painterResource(
                if (onSelect) R.drawable.ic_direction_arrow_solid else R.drawable.ic_direction_arrow_opacity
            ),
            "",
            Modifier
                .align(alignment)
                .padding(top = if (direction == JoystickDirection.Up) 20f.dp else 0.dp)
                .padding(bottom = if (direction == JoystickDirection.Down) 20f.dp else 0.dp)
                .padding(start = if (direction == JoystickDirection.Left) 15f.dp else 0.dp)
                .padding(end = if (direction == JoystickDirection.Right) 15f.dp else 0.dp)
                .size(24f.dp, 12f.dp)
                .rotate(rotate)

        )
    }
}