package com.example.screennetshare


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Scanner

class MainActivity : ComponentActivity() {

    private lateinit var ipShow: TextView
    private lateinit var ipAddress: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var imageView: ImageView

    private lateinit var server: Server
    private lateinit var client: Client



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

        ipAddress = findViewById(R.id.ip_address)
        ipShow = findViewById(R.id.ip_show)
        ipShow.text = getLocalIpAddress()
        ipAddress.setText(getLocalIpAddress())

        imageView = findViewById(R.id.image_view)

        // Initialize Server and Client
        server = Server()
        client = Client(imageView)

        // Set click listeners
        connectButton.setOnClickListener {
            client.startClient(ipAddress.text.toString(), 8080)
        }
        disconnectButton.setOnClickListener {
            client.stopClient()
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
                putExtra("ip", ipShow.text.toString())
                putExtra("port", 8080)
            }
            startForegroundService(intent)
            println("Screen capture permission granted")
        } else {
            Log.d("MainActivity", "Screen capture permission denied")
        }
    }

    fun requestScreenCapturePermission() {
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
}


