package com.checkealo.app.network

import android.content.Context
import android.util.Log
import com.checkealo.app.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttClientHelper(private val context: Context) {
    private val TAG = "MqttClientHelper"

    suspend fun publishNotification(payloadJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        val settings = AppSettings(context)
        if (!settings.mqttEnabled) {
            return@withContext Result.failure(Exception("MQTT is disabled in settings"))
        }

        val brokerUrl = "tcp://${settings.mqttHost}:${settings.mqttPort}"
        val clientId = settings.mqttClientId

        try {
            Log.d(TAG, "Connecting to MQTT Broker: $brokerUrl with client ID: $clientId")
            val persistence = MemoryPersistence()
            val client = MqttClient(brokerUrl, clientId, persistence)
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 60
                if (settings.mqttUsername.isNotEmpty()) {
                    userName = settings.mqttUsername
                }
                if (settings.mqttPassword.isNotEmpty()) {
                    password = settings.mqttPassword.toCharArray()
                }
            }

            client.connect(options)
            Log.d(TAG, "Connected to MQTT Broker!")

            val message = MqttMessage(payloadJson.toByteArray()).apply {
                qos = 1 // At least once
                isRetained = false
            }

            Log.d(TAG, "Publishing to topic: ${settings.mqttTopic}")
            client.publish(settings.mqttTopic, message)
            Log.d(TAG, "Published successfully!")

            client.disconnect()
            client.close()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing to MQTT Broker", e)
            Result.failure(e)
        }
    }

    // Function to test connection synchronously
    suspend fun testConnection(
        host: String,
        port: Int,
        clientId: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val brokerUrl = "tcp://$host:$port"
        try {
            val persistence = MemoryPersistence()
            val client = MqttClient(brokerUrl, clientId, persistence)
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 5
                if (username.isNotEmpty()) {
                    userName = username
                }
                if (password.isNotEmpty()) {
                    password = this@testConnection.password.toCharArray()
                }
            }
            client.connect(options)
            client.disconnect()
            client.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
