package com.blink.monitor

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class VideoPlayActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        textureView = findViewById(R.id.textureView)
        startButton = findViewById(R.id.startButton)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            //    surface?.let { startDecoding(Surface(it)) }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {

                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

        }
        startButton.setOnClickListener{
// 确保解码时 textureView 可用
            textureView.surfaceTexture?.let { surfaceTexture ->
                val surface = Surface(surfaceTexture)
                startDecoding(surface)
            }
        }

    }

    private fun startDecoding(surface: Surface) {
        // 获取文件路径
//        val filePath = File(filesDir, "output.h264").absolutePath
        val filePath = externalCacheDir?.absolutePath + "/output.h264"
        Log.d("H264Encoder", "video size:${File(filePath).length()}")
        // 调用JNI开始解码
        decodeAndPlay(filePath,surface)
        //decodeH264(filePath)
    }
    private external fun decodeAndPlay(filePath: String, surface: Surface)
    // 声明一个本地方法用于接收解码后的数据
    external fun decodeH264(filePath: String)

    fun handleDecodedData(decodedData: ByteArray) {
        lifecycleScope.launch {
            // 直接使用解码后的数据，避免额外的内存复制
            val yuvImage = YuvImage(decodedData, ImageFormat.NV21, 1280, 720, null) // 根据你的 YUV 格式调整
            val byteArrayOutputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, 1280, 720), 100, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            // 转换为 Bitmap
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // 获取 TextureView 的 Canvas 并绘制 Bitmap
            textureView.post {
                val canvas = textureView.lockCanvas()
                canvas?.drawBitmap(bitmap, 0f, 0f, null)
                canvas?.let { textureView.unlockCanvasAndPost(it) }

                // 释放 Bitmap 占用的内存
                bitmap.recycle()
            }
        }
    }



    companion object {
        init {
            System.loadLibrary("mediacodec")
        }
    }
}