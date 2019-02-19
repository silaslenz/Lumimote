package se.silenz.lumimote

import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {

    private fun getImage() {
        println("un func")
        val socket = DatagramSocket(49199)

        val udpPacketBuffer = ByteArray(35000)
        val receivedPacket = DatagramPacket(
            udpPacketBuffer, udpPacketBuffer.size,
            InetAddress.getByName("192.168.54.1"), 49199
        )
        val queue = Volley.newRequestQueue(this)

        fixedRateTimer("timer", false, 0, 5000) {
            this@MainActivity.runOnUiThread {
                queue.add(enableStream())
            }
        }
        queue.add(autoreviewunlock())
//        queue.add(pinch())
//        queue.add(pinchStop())

        while (true) {
            socket.receive(receivedPacket)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {

                    val img = ImageDecoder.decodeDrawable(
                        ImageDecoder.createSource(
                            ByteBuffer.wrap(
                                receivedPacket.data.copyOfRange(
                                    168,
                                    receivedPacket.data.size
                                )
                            )
                        )
                    )

                    runOnUiThread {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            imageView.setImageDrawable(img)
                        }

                    }
                } catch (e: Exception) {

                }
            } else {
                TODO("VERSION.SDK_INT < P")
            }


        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        capture_button.setOnClickListener { captureImage() }

        val queue = Volley.newRequestQueue(this)
        fab.setOnClickListener() {
            CoroutineScope(Dispatchers.IO).launch {
                queue.add(recMode())
                queue.add(enableStream())
                Thread.sleep(100)
                println("Start stream")
                getImage()


            }

        }
    }

    private fun captureImage() {
        val queue = Volley.newRequestQueue(this)
        queue.add(recMode())
        queue.add(capture())
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
