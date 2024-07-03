package com.example.screennetshare

import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class Server (private val portPreview: TextView) {

    private var serverSocket: ServerSocket? = null
    private lateinit var serverJob: Job
    private val clients = mutableListOf<Socket>()
    private var serverRunning = false
    private var port = 8080

    fun startServer() {
        if (!serverRunning) {
            serverRunning = true

            serverJob = CoroutineScope(Dispatchers.IO).launch {
                // Conseguir un puerto libre
                while (serverSocket == null && port < 8100) {
                    try {
                        serverSocket = ServerSocket(port)
                        Log.d("Server", "Server started on port $port")
                        launch(Dispatchers.Main) {
                            portPreview.text = "Servidor escuchando en el puerto: $port"
                        }
                    } catch (e: IOException) {
                        Log.w("Server", "Port $port is in use, trying next port...")
                        port++
                    }
                }

                launch {
                    while (serverRunning) {
                        if (!BufferImages.isEmpty()) {
                            val imageData = BufferImages.getImage()
                            if (imageData != null) {
                                synchronized(clients) {
                                    clients.filter { it.isConnected }.forEach { client ->
                                        try {
                                            val output = DataOutputStream(client.getOutputStream())
                                            output.writeInt(imageData.size)
                                            output.write(imageData)
                                            output.flush()
                                        } catch (e: IOException) {
                                            Log.e("Server", "Error sending image to client: ${e.message}")
                                            client.close()
                                            clients.remove(client)
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

                while (serverRunning) {
                    try {
                        val client = serverSocket?.accept()
                        if (client != null) {
                            Log.d("Server", "Client connected: ${client.inetAddress.hostAddress}")
                            synchronized(clients) {
                                clients.add(client)
                            }
                        }

                    } catch (e: SocketException) {
                        if (!serverRunning) {
                            Log.e("Server", "Server socket closed: ${e.message}")
                            break
                        }
                    } catch (e: IOException) {
                        Log.e("Server", "Error accepting client: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopServer() {
        serverRunning = false
        synchronized(clients) {
            clients.forEach { it.close() }
            clients.clear()
        }
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("Server", "Error closing server socket: ${e.message}")
        }
        if (::serverJob.isInitialized) {
            serverJob.cancel()
        }
    }

}
