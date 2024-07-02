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
            isConnected = true

            clientJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    clientSocket = Socket(ip, port)
                    val input = DataInputStream(clientSocket.getInputStream())

                    while (true) {
                        val imageLength = input.readInt()
                        val imageByte = ByteArray(imageLength)
                        input.readFully(imageByte)

                        if (imageLength > 0) {
                            val bitmap = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.size)
                            launch(Dispatchers.Main) {
                                imageView.setImageBitmap(bitmap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Client", "Error: ${e.message}")
                }
            }
        }
    }


    fun stopClient() {
        isConnected = false
        if (::clientJob.isInitialized) {
            clientJob.cancel()
        }
        if (::clientSocket.isInitialized) {
            try {
                clientSocket.close()
            } catch (e: IOException) {
                Log.e("Client", "Error closing socket: ${e.message}")
            }
        }
    }



}