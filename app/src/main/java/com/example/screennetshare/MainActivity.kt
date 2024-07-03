package com.example.screennetshare


import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class MainActivity : ComponentActivity() {

    private lateinit var server: Server
    private val handler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Permissions For Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        setContentView(R.layout.main_view)

        // Get UI elements
        val ipInput = findViewById<EditText>(R.id.ipInput)
        val portInput = findViewById<EditText>(R.id.portInput)
        val ipShow = findViewById<TextView>(R.id.ipShow)
        val serverStatus = findViewById<TextView>(R.id.serverStatus)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val btnStartServer = findViewById<Button>(R.id.btnStartServer)
        val btnStopServer = findViewById<Button>(R.id.btnStopServer)

        ipShow.text = getLocalIpAddress()
        serverStatus.text = "Servidor no iniciado"

        server = Server(serverStatus)

        // Client handler
        btnConnect.setOnClickListener {
            if (ipInput.text.toString().isEmpty()) {
                handler.post {
                    Toast.makeText(applicationContext, "Please enter an IP address", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            if (portInput.text.toString().isEmpty()) {
                handler.post {
                    Toast.makeText(applicationContext, "Please enter a port number", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            val clientView = Intent(this, ClientViewActivity::class.java).apply {
                putExtra("ip", ipInput.text.toString())
                putExtra("port", portInput.text.toString().toInt())
            }
            startActivity(clientView)
        }

        // Server handler
        btnStartServer.setOnClickListener {
            server.startServer()
            requestScreenCapturePermission()
        }
        btnStopServer.setOnClickListener {
            server.stopServer()
            stopService(Intent(this, ForegroundService::class.java))
            serverStatus.text = "Servidor no iniciado"
        }

    }

    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(this, ForegroundService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startForegroundService(intent)
            Log.d("MainActivity","Screen capture permission granted")
        } else {
            Log.d("MainActivity", "Screen capture permission denied")
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress?.toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e("ServerActivity", ex.toString())
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::server.isInitialized) {
            server.stopServer()
        }
        stopService(Intent(this, ForegroundService::class.java))
    }

}


