package com.checkealo.app.data

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("checkealo_settings", Context.MODE_PRIVATE)

    var mqttHost: String
        get() = prefs.getString("mqtt_host", "broker.hivemq.com") ?: "broker.hivemq.com"
        set(value) = prefs.edit().putString("mqtt_host", value).apply()

    var mqttPort: Int
        get() = prefs.getInt("mqtt_port", 1883)
        set(value) = prefs.edit().putInt("mqtt_port", value).apply()

    var mqttTopic: String
        get() = prefs.getString("mqtt_topic", "checkealo/notifications") ?: "checkealo/notifications"
        set(value) = prefs.edit().putString("mqtt_topic", value).apply()

    var mqttClientId: String
        get() = prefs.getString("mqtt_client_id", "checkealo_android_${System.currentTimeMillis()}") ?: "checkealo_android"
        set(value) = prefs.edit().putString("mqtt_client_id", value).apply()

    var mqttUsername: String
        get() = prefs.getString("mqtt_username", "") ?: ""
        set(value) = prefs.edit().putString("mqtt_username", value).apply()

    var mqttPassword: String
        get() = prefs.getString("mqtt_password", "") ?: ""
        set(value) = prefs.edit().putString("mqtt_password", value).apply()

    var mqttEnabled: Boolean
        get() = prefs.getBoolean("mqtt_enabled", false)
        set(value) = prefs.edit().putBoolean("mqtt_enabled", value).apply()

    var whatsappApiUrl: String
        get() = prefs.getString("whatsapp_api_url", "") ?: ""
        set(value) = prefs.edit().putString("whatsapp_api_url", value).apply()

    var whatsappToken: String
        get() = prefs.getString("whatsapp_token", "") ?: ""
        set(value) = prefs.edit().putString("whatsapp_token", value).apply()

    var whatsappPhone: String
        get() = prefs.getString("whatsapp_phone", "") ?: ""
        set(value) = prefs.edit().putString("whatsapp_phone", value).apply()

    var whatsappEnabled: Boolean
        get() = prefs.getBoolean("whatsapp_enabled", false)
        set(value) = prefs.edit().putBoolean("whatsapp_enabled", value).apply()
}
