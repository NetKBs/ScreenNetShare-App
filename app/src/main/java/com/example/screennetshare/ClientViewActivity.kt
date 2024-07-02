package com.example.screennetshare

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController

class ClientViewActivity : AppCompatActivity() {

    private lateinit var screenView: ImageView
    private lateinit var client: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_view)

        screenView = findViewById(R.id.screenView)
        client = Client(screenView)

        // Obtener los datos de IP y puerto desde la actividad anterior
        val ip = intent.getStringExtra("ip")
        val port = intent.getIntExtra("port", 0)
        if (ip != null) {
            client.startClient(ip, port)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::client.isInitialized) {
            client.stopClient()
        }

    }
}
