package com.checkealo.app.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkealo.app.data.AppSettings
import com.checkealo.app.data.NotificationDatabase
import com.checkealo.app.data.NotificationLog
import com.checkealo.app.network.MqttClientHelper
import com.checkealo.app.network.WhatsAppClientHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NotificationDatabase.getDatabase(application)
    private val dao = database.notificationDao()
    private val settings = AppSettings(application)

    // Flow of logs from Room database
    val notificationLogs: StateFlow<List<NotificationLog>> = dao.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI configuration states
    private val _mqttHost = MutableStateFlow(settings.mqttHost)
    val mqttHost = _mqttHost.asStateFlow()

    private val _mqttPort = MutableStateFlow(settings.mqttPort)
    val mqttPort = _mqttPort.asStateFlow()

    private val _mqttTopic = MutableStateFlow(settings.mqttTopic)
    val mqttTopic = _mqttTopic.asStateFlow()

    private val _mqttClientId = MutableStateFlow(settings.mqttClientId)
    val mqttClientId = _mqttClientId.asStateFlow()

    private val _mqttUsername = MutableStateFlow(settings.mqttUsername)
    val mqttUsername = _mqttUsername.asStateFlow()

    private val _mqttPassword = MutableStateFlow(settings.mqttPassword)
    val mqttPassword = _mqttPassword.asStateFlow()

    private val _mqttEnabled = MutableStateFlow(settings.mqttEnabled)
    val mqttEnabled = _mqttEnabled.asStateFlow()

    private val _whatsappApiUrl = MutableStateFlow(settings.whatsappApiUrl)
    val whatsappApiUrl = _whatsappApiUrl.asStateFlow()

    private val _whatsappToken = MutableStateFlow(settings.whatsappToken)
    val whatsappToken = _whatsappToken.asStateFlow()

    private val _whatsappPhone = MutableStateFlow(settings.whatsappPhone)
    val whatsappPhone = _whatsappPhone.asStateFlow()

    private val _whatsappEnabled = MutableStateFlow(settings.whatsappEnabled)
    val whatsappEnabled = _whatsappEnabled.asStateFlow()

    // Test results
    private val _mqttTestStatus = MutableStateFlow<String?>(null)
    val mqttTestStatus = _mqttTestStatus.asStateFlow()

    private val _whatsappTestStatus = MutableStateFlow<String?>(null)
    val whatsappTestStatus = _whatsappTestStatus.asStateFlow()

    fun updateMqttSettings(
        host: String,
        port: Int,
        topic: String,
        clientId: String,
        username: String,
        password: String,
        enabled: Boolean
    ) {
        settings.mqttHost = host
        settings.mqttPort = port
        settings.mqttTopic = topic
        settings.mqttClientId = clientId
        settings.mqttUsername = username
        settings.mqttPassword = password
        settings.mqttEnabled = enabled

        _mqttHost.value = host
        _mqttPort.value = port
        _mqttTopic.value = topic
        _mqttClientId.value = clientId
        _mqttUsername.value = username
        _mqttPassword.value = password
        _mqttEnabled.value = enabled
    }

    fun updateWhatsappSettings(
        apiUrl: String,
        token: String,
        phone: String,
        enabled: Boolean
    ) {
        settings.whatsappApiUrl = apiUrl
        settings.whatsappToken = token
        settings.whatsappPhone = phone
        settings.whatsappEnabled = enabled

        _whatsappApiUrl.value = apiUrl
        _whatsappToken.value = token
        _whatsappPhone.value = phone
        _whatsappEnabled.value = enabled
    }

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearAllLogs()
        }
    }

    fun testMqttConnection() {
        viewModelScope.launch {
            _mqttTestStatus.value = "Conectando..."
            val helper = MqttClientHelper(getApplication())
            val result = helper.testConnection(
                host = _mqttHost.value,
                port = _mqttPort.value,
                clientId = _mqttClientId.value,
                username = _mqttUsername.value,
                password = _mqttPassword.value
            )
            if (result.isSuccess) {
                _mqttTestStatus.value = "Conexión Exitosa ✅"
            } else {
                _mqttTestStatus.value = "Fallo: ${result.exceptionOrNull()?.message} ❌"
            }
        }
    }

    fun testWhatsappMessage() {
        viewModelScope.launch {
            _whatsappTestStatus.value = "Enviando mensaje..."
            val helper = WhatsAppClientHelper(getApplication())
            val testMsg = "🔔 *Checkealo de Prueba*\nEsto es una prueba de integración exitosa."
            val result = helper.testMessage(
                apiUrl = _whatsappApiUrl.value,
                token = _whatsappToken.value,
                phone = _whatsappPhone.value,
                messageText = testMsg
            )
            if (result.isSuccess) {
                _whatsappTestStatus.value = "Envío Exitoso ✅"
            } else {
                _whatsappTestStatus.value = "Fallo: ${result.exceptionOrNull()?.message} ❌"
            }
        }
    }

    fun triggerMockNotification(sender: String, amount: Double) {
        val context = getApplication<Application>()
        val channelId = "checkealo_test_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pruebas Checkealo",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val randomCode = 100000 + (Math.random() * 900000).toInt()
        val text = "Confirmacion de Pago $sender te envio un pago por S/ ${String.format("%.2f", amount)} el cod de seguridad es $randomCode"
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Confirmación de Pago")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun clearTestStatus() {
        _mqttTestStatus.value = null
        _whatsappTestStatus.value = null
    }
}
