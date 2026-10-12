package com.nama30.tv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** Session + settings. Observable fields are Compose state so the UI follows changes (language, login). */
object Prefs {
    private lateinit var sp: SharedPreferences

    var token by mutableStateOf<String?>(null); private set
    var user by mutableStateOf<User?>(null); private set
    var lang by mutableStateOf("en"); private set
    var guest by mutableStateOf(false); private set

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("n30", Context.MODE_PRIVATE)
        token = sp.getString("token", null)
        user = sp.getString("user", null)?.let { runCatching { User.from(JSONObject(it)) }.getOrNull() }
        lang = sp.getString("lang", "en") ?: "en"
        guest = sp.getBoolean("guest", false)
    }

    val loggedIn get() = !token.isNullOrBlank()

    fun setSession(token: String, userJson: JSONObject?) {
        this.token = token
        sp.edit().putString("token", token).apply()
        if (userJson != null) { user = User.from(userJson); sp.edit().putString("user", userJson.toString()).apply() }
    }
    fun clearSession() { token = null; user = null; sp.edit().remove("token").remove("user").apply() }
    fun changeLang(v: String) { lang = v; sp.edit().putString("lang", v).apply() }
    fun changeGuest(v: Boolean) { guest = v; sp.edit().putBoolean("guest", v).apply() }

    var host: String?
        get() = sp.getString("host", null)
        set(v) { sp.edit().putString("host", v).apply() }
    var defaultSub: String
        get() = sp.getString("sub", "off") ?: "off"
        set(v) { sp.edit().putString("sub", v).apply() }
    var subSize: Int
        get() = sp.getInt("subSize", 46)
        set(v) { sp.edit().putInt("subSize", v).apply() }
    var autoplay: Boolean
        get() = sp.getBoolean("autoplay", true)
        set(v) { sp.edit().putBoolean("autoplay", v).apply() }
    var homeCache: String?
        get() = sp.getString("homeCache", null)
        set(v) { sp.edit().putString("homeCache", v).apply() }
}
