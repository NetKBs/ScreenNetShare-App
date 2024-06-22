package com.example.screennetshare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.net.SocketException

class ForegroundService: Service() {

    private lateinit var  notificationManager: NotificationManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var imageReader: ImageReader
    private var isStarted = false

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "1001"

        fun startService(context: Context) {
            val intent = Intent(context, ForegroundService::class.java)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundService::class.java)
            context.stopService(intent)
        }

    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onDestroy() {
        super.onDestroy()
        isStarted = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isStarted) {
            makeForeground()
            isStarted = true
        }

        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")
        val ip = intent?.getStringExtra("ip") ?: ""
        val port = intent?.getIntExtra("port", 0) ?: 0

        if (resultCode != 0 && data != null) {
            startScreenCapture(resultCode, data, ip, port)
        }

        return START_NOT_STICKY
    }

    private var socket: Socket? = null

    private fun startScreenCapture(resultCode: Int, data: Intent, ip: String, port: Int) {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        // Configurar el virtualDisplay y el Sufrace
        val metrics = resources.displayMetrics
        val density = metrics.density
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        // Crear un Surface (Puede ser SurfaceTexture o ImageReader)
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket(ip, port)

                // Crear el virtualDisplay
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "ScreenCapture",
                    width,
                    height,
                    density.toInt(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null
                )

                val mainHandler = Handler(Looper.getMainLooper())

                // Registrar un listener para procesar los frames
                imageReader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        sendImage(image, ip)
                        image.close()
                    }
                }, mainHandler)

            } catch (e: SocketException) {
                e.printStackTrace()
            }
        }

    }

    private fun sendImage(image: Image, ip: String) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        // Comprime el Bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, outputStream)
        val compressedBitmapByteArray = outputStream.toByteArray()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val output = DataOutputStream(socket?.getOutputStream())
                output.writeInt(compressedBitmapByteArray.size)
                output.write(compressedBitmapByteArray)
                output.flush()
            } catch (e: SocketException) {
                Log.e("ForegroundService", "Error sending image: ${e.message}")
            } catch (e: IOException) {
                Log.e("ForegroundService", "Error writing to socket: ${e.message}")
            }

        }
    }

    private fun stopScreenCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun makeForeground() {
        createServiceNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service Title")
            .setContentText("Foreground Service Content Context")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException()
    }


}