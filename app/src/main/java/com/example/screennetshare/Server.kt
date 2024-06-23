package com.example.screennetshare

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CopyOnWriteArrayList

class Server {

    private lateinit var serverSocket: ServerSocket
    private lateinit var serverJob: Job
    private val clients = mutableListOf<Socket>()
    private var serverRunning = false

    fun startServer() {
        if (!serverRunning) {
            serverRunning = true

            serverJob = CoroutineScope(Dispatchers.IO).launch {
                serverSocket = ServerSocket(8080)
                Log.d("Server", "Server started on port 8080")

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
                        val client = serverSocket.accept()
                        Log.d("Server", "Client connected: ${client.inetAddress.hostAddress}")

                        synchronized(clients) {
                            clients.add(client)
                        }
                        launch {
                            clientHandler(client)
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

    private fun clientHandler(client: Socket) {
        try {
            val input = DataInputStream(client.getInputStream())

            while (client.isConnected) {
                try {
                    val message = input.readUTF()
                    Log.d("ClientHandler", "Received message from client: $message")
                } catch (e: EOFException) {
                    break
                } catch (e: IOException) {
                    Log.e("ClientHandler", "Error reading from client: ${e.message}")
                    break
                }
            }
        } catch (e: IOException) {
            Log.e("ClientHandler", "Error handling client: ${e.message}")
        } finally {
            synchronized(clients) {
                clients.remove(client)
            }
            try {
                client.close()
            } catch (e: IOException) {
                Log.e("ClientHandler", "Error closing client socket: ${e.message}")
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
            serverSocket.close()
        } catch (e: IOException) {
            Log.e("Server", "Error closing server socket: ${e.message}")
        }
        serverJob.cancel()
    }
}
