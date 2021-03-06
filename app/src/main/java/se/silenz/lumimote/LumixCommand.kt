package se.silenz.lumimote

import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

internal fun recMode(): StringRequest {
    return camCmd("recmode")
}

internal fun capture(): StringRequest {
    return camCmd("capture")
}

internal fun enableStream(): StringRequest {
    return createRequest("http://192.168.54.1/cam.cgi?mode=startstream&value=49199")
}

internal fun stopStream(): StringRequest {
    return createRequest("http://192.168.54.1/cam.cgi?mode=stopstream")
}

internal fun autoreviewunlock(): StringRequest {
    return camCmd("autoreviewunlock")
}

internal fun pinch(): StringRequest {
    return createRequest("http://192.168.54.1/cam.cgi?mode=camctrl&type=pinch&value=continue&value2=675/161/582/692")
}

internal fun pinchStop(): StringRequest {
    return createRequest("http://192.168.54.1/cam.cgi?mode=camctrl&type=pinch&value=stop&value2=675/161/582/692")
}


internal fun setSetting(type: String, value: String): StringRequest {
    return createRequest("http://192.168.54.1/cam.cgi?mode=setsetting&type=$type&value=$value")

}

internal fun camCmd(value: String): StringRequest {
    return createRequest("http://192.168.54.1/cam.cgi?mode=camcmd&value=$value")
}

internal fun camCtrl(type: String, value: String, value2: String): StringRequest {
    return createRequest("http://192.168.54.1/cam.cgi?mode=camctrl&type=$type&value=$value&value2=$value2")
}


private fun createRequest(url: String): StringRequest {
    return StringRequest(
        Request.Method.GET, url,
        Response.Listener<String> { response ->
            Log.d("Network", response)
        },
        Response.ErrorListener { response ->

            //            Log.e("VOLLEY", response.message)
        }
    )
}