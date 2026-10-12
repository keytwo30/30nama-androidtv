package com.nama30.tv

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** fetch() bridge: JS hands over a request, gets a completion callback, then pulls the body with take(). */
class NetBridge(private val web: WebView, private val http: OkHttpClient) {
    private val main = Handler(Looper.getMainLooper())
    private val bodies = ConcurrentHashMap<Int, String>()

    @JavascriptInterface
    fun request(id: Int, method: String, url: String, headersJson: String, body: String) {
        val h = HashMap<String, String>()
        try {
            val o = JSONObject(headersJson)
            o.keys().forEach { h[it] = o.getString(it) }
        } catch (_: Exception) {}
        if (h.keys.none { it.equals("user-agent", true) }) h["User-Agent"] = Net.UA
        val ct = h.entries.firstOrNull { it.key.equals("content-type", true) }?.value
        val rb = if (method == "GET" || method == "HEAD") null else body.toRequestBody(ct?.toMediaTypeOrNull())
        val req = try {
            Request.Builder().url(url).headers(h.toHeaders()).method(method, rb).build()
        } catch (e: Exception) {
            done(id, 0, "{}"); return
        }
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = done(id, 0, "{}")
            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    val text = try { r.body?.string() ?: "" } catch (e: IOException) { return done(id, 0, "{}") }
                    val hs = JSONObject()
                    for (name in r.headers.names()) hs.put(name, r.header(name))
                    bodies[id] = text
                    done(id, r.code, hs.toString())
                }
            }
        })
    }

    @JavascriptInterface
    fun take(id: Int): String? = bodies.remove(id)

    private fun done(id: Int, status: Int, headers: String) {
        main.post { web.evaluateJavascript("__nnDone($id,$status,${JSONObject.quote(headers)})", null) }
    }
}
