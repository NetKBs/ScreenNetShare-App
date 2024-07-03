package com.example.screennetshare

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ClientViewActivity : AppCompatActivity() {

    private lateinit var client: Client
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_view)

        val ip = intent.getStringExtra("ip") ?: ""
        val port = intent.getIntExtra("port", 0)
        val screenView = findViewById<ImageView>(R.id.screenView)
        val disconnectButton = findViewById<ImageButton>(R.id.btnDisconnect)

        client = Client(screenView)
        lifecycleScope.launch {
            val deferred = client.startClient(ip, port)
            val isConnected = deferred.await()

            if (isConnected) {
                Log.d("ClientViewActivity", "Conexión establecida con el servidor")
            } else {
                Log.d("ClientViewActivity", "Error al conectar con el servidor")
                finish()
            }
        }

        disconnectButton.setOnClickListener {
            if (client.isConnected) {
                client.stopClient()
            }
            finish()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::client.isInitialized) {
            client.stopClient()
        }
    }

}
