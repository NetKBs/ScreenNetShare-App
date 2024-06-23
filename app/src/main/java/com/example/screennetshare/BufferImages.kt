package com.example.screennetshare

import java.util.concurrent.ConcurrentLinkedQueue

object BufferImages {
    private val buffer: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue()
    private const val BUFFER_SIZE: Int = 60 // Tamaño del buffer

    fun addImage(imageData: ByteArray) {
        if (buffer.size >= BUFFER_SIZE) {
            buffer.poll() // Eliminar la imagen más antigua
        }
        buffer.offer(imageData)
    }

    fun getImage(): ByteArray? {
        return buffer.poll()
    }

    fun isEmpty(): Boolean {
        return buffer.isEmpty()
    }
}