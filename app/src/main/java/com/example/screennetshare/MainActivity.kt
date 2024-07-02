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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class MainActivity : ComponentActivity() {

    private lateinit var ipShow: TextView
    private lateinit var ipAddress: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var imageView: ImageView
    private lateinit var portPreview: TextView
    private lateinit var portInput: EditText

    private lateinit var server: Server
    private lateinit var client: Client

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

        setContentView(R.layout.activity_main)

        // Get UI elements
        connectButton = findViewById(R.id.connectBtn)
        disconnectButton = findViewById(R.id.disconnectBtn)
        val startServerButton = findViewById<Button>(R.id.startServerBtn)

        portPreview = findViewById(R.id.portPreview)
        portInput = findViewById(R.id.portInput)

        ipAddress = findViewById(R.id.ip_address)
        ipShow = findViewById(R.id.ip_show)
        ipShow.text = getLocalIpAddress()
        ipAddress.setText(getLocalIpAddress())

        imageView = findViewById(R.id.image_view)

        // Initialize Server and Client
        server = Server(portPreview)

        val inflater = layoutInflater.inflate(R.layout.client_view, null)
        val screenView = inflater.findViewById<ImageView>(R.id.screenView)

        // Set click listeners
        connectButton.setOnClickListener {
            if (ipAddress.text.toString().isEmpty()) {
                handler.post {
                    Toast.makeText(applicationContext, "Please enter an IP address", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            } else if (portInput.text.toString().isEmpty()) {
                handler.post {
                    Toast.makeText(applicationContext, "Please enter a port number", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

                val nextView = Intent(this, ClientViewActivity::class.java).apply {
                    putExtra("ip", ipAddress.text.toString())
                    putExtra("port", portInput.text.toString().toInt())
                }
                startActivity(nextView)

        }


        disconnectButton.setOnClickListener {
            if (client.isConnected) {
                client.stopClient()
            }

        }
        startServerButton.setOnClickListener {
            server.startServer()
            requestScreenCapturePermission()
        }

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

    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
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
        server.stopServer()
        if (::client.isInitialized) {
            client.stopClient()
        }
        stopService(Intent(this, ForegroundService::class.java))
    }

}


