package com.example.screennetshare

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.IOException
import java.net.Socket

class Client(private val imageView: ImageView) {

    private lateinit var clientSocket: Socket
    private lateinit var clientJob: Job
    var isConnected = false

    fun startClient(ip: String, port: Int) {
        if (!isConnected) {
            clientJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    clientSocket = Socket(ip, port)
                    val input = DataInputStream(clientSocket.getInputStream())
                    val options = BitmapFactory.Options()
                    isConnected = true

                    while (true) {
                        val imageLength = input.readInt()
                        if (imageLength > 0) {
                            val byteArray = ByteArray(imageLength)
                            input.readFully(byteArray)

                            // Downsample la imagen
                            options.inJustDecodeBounds = true
                            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
                            options.inSampleSize = calculateInSampleSize(options, imageView.width, imageView.height)
                            options.inJustDecodeBounds = false

                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)

                            launch(Dispatchers.Main) {
                                imageView.setImageBitmap(bitmap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Client", "Error: ${e.message}")
                } finally {
                    try {
                        if (::clientSocket.isInitialized) {
                            clientSocket.close()
                        }
                    } catch (e: IOException) {
                        Log.e("Client", "Error closing socket: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopClient() {
        if (isConnected) {
            isConnected = false
            clientJob.cancel()
            try {
                if (::clientSocket.isInitialized) {
                    clientSocket.close()
                }
            } catch (e: IOException) {
                Log.e("Client", "Error closing socket: ${e.message}")
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw image size
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
