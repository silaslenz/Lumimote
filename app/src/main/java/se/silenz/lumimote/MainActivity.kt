package se.silenz.lumimote

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.fixedRateTimer


class MainActivity : AppCompatActivity() {
    val currentSettings = mutableMapOf<String, String>()
    @ExperimentalUnsignedTypes
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
                queue.add(getXMLData())
            }
        }
        queue.add(autoreviewunlock())

        while (true) {
            socket.receive(receivedPacket)
            try {
                val imageOffset = 2 + 30 + receivedPacket.data[31].toUByte().toInt()
                val bmp =
                    BitmapFactory.decodeByteArray(receivedPacket.data, imageOffset, receivedPacket.length - imageOffset)

                runOnUiThread {
                    imageView.setCurrentImage(bmp)

                }
            } catch (e: Exception) {
                Log.e("ViewFinder", e.toString())

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        capture_button.setOnClickListener { captureImage() }

        val queue = Volley.newRequestQueue(this)
        CoroutineScope(Dispatchers.IO).launch {
            queue.add(recMode())
            queue.add(enableStream())
            Thread.sleep(100)
            println("Start stream")
            getImage()


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

    fun getXMLData(): StringRequest {
        return StringRequest(
            Request.Method.GET, "http://192.168.54.1/cam.cgi?mode=getinfo&type=curmenu",
            Response.Listener { response ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()

                xpp.setInput(StringReader(response))
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getAttributeValue(null,"value")!=null){
                            currentSettings[xpp.getAttributeValue(null,"id")]=xpp.getAttributeValue(null,"value")
                        }
                    }
                    eventType = xpp.next()
                }
                textView.text = currentSettings["menu_item_id_sensitivity"]
            },
            Response.ErrorListener { response ->

                //            Log.e("VOLLEY", response.message)
            }
        )
    }
}
