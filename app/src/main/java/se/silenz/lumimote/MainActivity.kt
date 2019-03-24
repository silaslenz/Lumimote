package se.silenz.lumimote

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
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
    private val currentSettings = mutableMapOf<String, String>()
    private val isos = mutableListOf<String>()
    @ExperimentalUnsignedTypes
    private fun getImage() {
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
                // https://gist.github.com/FWeinb/2d51fe63d0f9f5fc4d32d8a420b2c18d
                val additionalMetadata = bytesToInt(receivedPacket.data[47], receivedPacket.data[48])
                val imageOffset = 2 + 30 + receivedPacket.data[31].toUByte().toInt()
                val metaData = receivedPacket.data.slice((48 + 16)..48 + additionalMetadata)
                if (additionalMetadata == 1282) {
                    //TODO: Find better way to detect focus data. This only works for exactly four focus boxes (and probably only on this camera).
                    val boxes =
                        mutableListOf<Rect>() // List of boxes, <TopLeft<X,Y>, BottomRight<X,Y>>
                    for (i in 0 until 4) {
                        val topleftX = bytesToInt(metaData[16 * i], metaData[1 + 16 * i])
                        val topleftY = bytesToInt(metaData[2 + 16 * i], metaData[3 + 16 * i])
                        val bottomrightX =
                            bytesToInt(metaData[4 + 16 * i], metaData[5 + 16 * i])
                        val bottomrightY =
                            bytesToInt(metaData[6 + 16 * i], metaData[7 + 16 * i])
                        boxes.add(Rect(topleftX, topleftY, bottomrightX, bottomrightY))
                    }
                    for (box in boxes) {

                        box.left = (box.left * (0.001 * viewFinder.viewFinderWidth)).toInt()
                        box.right = (box.right * (0.001 * viewFinder.viewFinderWidth)).toInt()
                        box.top = (box.top * (0.001 * viewFinder.viewFinderHeight)).toInt()
                        box.bottom = (box.bottom * (0.001 * viewFinder.viewFinderHeight)).toInt()
                    }
                    viewFinder.boxes = boxes.toTypedArray()
                }
                // TODO: Add else to remove focus boxes from viewfinder again
                val bmp =
                    BitmapFactory.decodeByteArray(receivedPacket.data, imageOffset, receivedPacket.length - imageOffset)

                runOnUiThread {
                    viewFinder.setCurrentImage(bmp)

                }
            } catch (e: Exception) {
                Log.e("ViewFinder", e.toString())

            }
        }
    }

    private fun bytesToInt(byte1: Byte, byte2: Byte) =
        ((byte1.toInt() and 255) shl 8) or (byte2.toInt() and 255)

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
        isoButton.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pick a color")
            builder.setItems(isos.toTypedArray()) { _, which ->
                val queue = Volley.newRequestQueue(this)
                queue.add(setSetting("iso", isos[which]))
                queue.add(getXMLData())
            }
            builder.show()
        }
        oneshotAF.setOnClickListener {
            val queue = Volley.newRequestQueue(this)
            queue.add(camCmd("oneshot_af"))
        }

        viewFinder.setOnTouchListener { v, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {
                val queue = Volley.newRequestQueue(this)
                val location =
                    ((event.x * 1000) / viewFinder.viewFinderWidth).toInt().toString() + "/" + ((event.y * 1000) / viewFinder.viewFinderHeight).toInt().toString()
                queue.add(camCtrl("touch", location, "on"))

            } else if (event.action == MotionEvent.ACTION_UP) {
                val queue = Volley.newRequestQueue(this)
                val location =
                    ((event.x * 1000) / viewFinder.viewFinderWidth).toInt().toString() + "/" + ((event.y * 1000) / viewFinder.viewFinderHeight).toInt().toString()
                queue.add(camCtrl("touch", location, "off"))
            }
            val d = Log.d(
                "ViewFinder",
                "Touched at ${event.x / viewFinder.viewFinderWidth * 1000}, ${event.y / viewFinder.viewFinderHeight * 1000} ${event.action}"
            )
            true
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

    private fun getXMLData(): StringRequest {
        return StringRequest(
            Request.Method.GET, "http://192.168.54.1/cam.cgi?mode=getinfo&type=curmenu",
            Response.Listener { response ->
                isos.clear()
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()

                xpp.setInput(StringReader(response))
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getAttributeValue(null, "value") != null) {
                            currentSettings[xpp.getAttributeValue(null, "id")] = xpp.getAttributeValue(null, "value")
                        } else if (xpp.getAttributeValue(null, "enable") == "yes") {
                            if (xpp.getAttributeValue(null, "id").contains("menu_item_id_sensitivity_")) {
                                isos.add(xpp.getAttributeValue(null, "id").split("menu_item_id_sensitivity_")[1])
                            }

                        }
                    }
                    eventType = xpp.next()
                }
                isoButton.text = getString(R.string.iso, currentSettings["menu_item_id_sensitivity"])
            },
            Response.ErrorListener { response ->

                //            Log.e("VOLLEY", response.message)
            }
        )
    }
}
