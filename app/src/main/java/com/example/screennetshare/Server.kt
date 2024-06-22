package com.example.screennetshare

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class Server () {

    lateinit var serverSocket: ServerSocket
    lateinit var serverJob: Job
    private var clients = mutableListOf<Socket>()
    private var serverRunning = false

    fun startServer() {
        if (!serverRunning) {
            serverRunning = true

            serverJob = CoroutineScope(Dispatchers.IO).launch {
                serverSocket = ServerSocket(8080)

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
                    }
                }
            }
        }
    }

    private fun clientHandler(client: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val input = DataInputStream(client.getInputStream())

                while (client.isConnected) {
                    val imageLength = input.readInt()
                    val imageByte = ByteArray(imageLength)
                    input.readFully(imageByte)

                    if (imageLength > 0) {
                        // Retransmitir imagen a todos los clientes
                        synchronized(clients) {
                            clients.forEach { otherClient ->
                                if (otherClient != client && otherClient.isConnected) {
                                    try {
                                        val output = DataOutputStream(otherClient.getOutputStream())
                                        output.writeInt(imageByte.size)
                                        output.write(imageByte)
                                        output.flush()
                                    } catch (e: IOException) {
                                        Log.e("ClientHandler", "Error sending image to client: ${e.message}")
                                        otherClient.close() // Cierra el socket del cliente problemático
                                        clients.remove(otherClient)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("ClientHandler", "Error handling client: ${e.message}")
            } finally {
                synchronized(clients) {
                    clients.remove(client)
                }
                try {
                    client.close() // Cerrar el socket al finalizar
                } catch (e: IOException) {
                    Log.e("ClientHandler", "Error closing client socket: ${e.message}")
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
        serverSocket.close()
        serverJob.cancel()
    }

}