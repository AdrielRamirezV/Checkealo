package com.checkealo.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.checkealo.app.data.NotificationDatabase
import com.checkealo.app.data.NotificationLog
import com.checkealo.app.network.MqttClientHelper
import com.checkealo.app.network.WhatsAppClientHelper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {
    private val TAG = "NotificationService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    data class ParsedNotification(
        val appName: String,
        val sender: String,
        val amount: Double,
        val transactionCode: String? = null
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener connected!")
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "checkealo_foreground_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio Activo Checkealo",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Mantiene el lector de pagos activo las 24 horas"
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("Checkealo Activo")
            .setContentText("Monitoreando pagos de Yape y Plin en segundo plano")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()

        Log.d(TAG, "Notification received from package: $packageName | Title: $title | Text: $text")

        // Parse notification
        val parsed = parseNotification(packageName, title, text)
        if (parsed != null) {
            Log.d(TAG, "Parsed notification successfully: App=${parsed.appName}, Sender=${parsed.sender}, Amount=${parsed.amount}")
            serviceScope.launch {
                processTransaction(packageName, parsed, text ?: "")
            }
        } else {
            // FALLBACK DEBUG LOG: Save unparsed notifications from Yape/Plin packages to the database
            val isYape = packageName.contains("yape", ignoreCase = true)
            val isPlinBank = packageName.contains("bbva", ignoreCase = true) || 
                              packageName.contains("interbank", ignoreCase = true) || 
                              packageName.contains("scotiabank", ignoreCase = true)
            
            if (isYape || isPlinBank) {
                val appNameLabel = if (isYape) "Yape (No Parseado)" else "Plin (No Parseado)"
                serviceScope.launch {
                    val database = NotificationDatabase.getDatabase(applicationContext)
                    val dao = database.notificationDao()
                    val debugLog = NotificationLog(
                        appName = appNameLabel,
                        packageName = packageName,
                        sender = "Desconocido",
                        amount = 0.0,
                        rawText = "Título: $title | Texto: $text",
                        mqttSent = false,
                        whatsappSent = false
                    )
                    dao.insertLog(debugLog)
                }
            }
        }
    }

    private fun parseNotification(packageName: String, title: String?, text: String?): ParsedNotification? {
        if (text == null) return null

        val isYapePackage = packageName.contains("yape", ignoreCase = true) || packageName == "com.checkealo.app"

        // 1a. Yape confirmation payment notifications (Text body check)
        // Text: "Juan Perez te envio un pago por S/ 20.00 el cod de seguridad es 123456"
        // Also supports when Title is "Confirmación de Pago" and Text doesn't repeat the title
        if (isYapePackage) {
            // Regex matches the body text, allowing optional "Confirmacion de Pago" prefix, flexible decimals (0.1, 1, 1.00), period after amount, and variations in "El cód. de seguridad es: XXX"
            val yapeConfirmBodyRegex = Regex("""(?:Confirmaci[oó]n de Pago\s+)?(.+?)\s+te envi[oó]\s+un pago por\s+S/\s*([\d,]+(?:\.\d+)?)\.?(?:\s+[eé]l c[oó]d\.?\s+de seguridad\s+es:?\s*(\w+))?""", RegexOption.IGNORE_CASE)
            val yapeConfirmMatch = yapeConfirmBodyRegex.find(text)
            if (yapeConfirmMatch != null) {
                val sender = yapeConfirmMatch.groupValues[1].trim()
                val amountStr = yapeConfirmMatch.groupValues[2].replace(",", "").trim()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val code = if (yapeConfirmMatch.groupValues.size > 3 && yapeConfirmMatch.groupValues[3].isNotEmpty()) yapeConfirmMatch.groupValues[3].trim() else null
                return ParsedNotification("Yape", sender, amount, code)
            }

            // 1b. Yape standard notifications
            // Example: "Juan Perez te yapeó S/ 15.00"
            val yapeRegex = Regex("""(?:¡Yape!\s+)?(.+?)\s+te yapeó\s+S/(\s*[\d,]+\.\d{2})""", RegexOption.IGNORE_CASE)
            val yapeMatch = yapeRegex.find(text)
            if (yapeMatch != null) {
                val sender = yapeMatch.groupValues[1].trim()
                val amountStr = yapeMatch.groupValues[2].replace(",", "").trim()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                return ParsedNotification("Yape", sender, amount)
            }
        }

        // 2. Plin notifications
        // Example: "Plin: Juan Perez te envió S/ 15.00" or "¡Recibiste un Plin! Juan Perez te envió S/ 15.00"
        val plinRegex = Regex("""(?:plin:?|recibiste un plin!?)\s*(.+?)\s+te (?:envió|transfirió)\s+S/(\s*[\d,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        val plinMatch = plinRegex.find(text)
        if (plinMatch != null) {
            val sender = plinMatch.groupValues[1].trim()
            val amountStr = plinMatch.groupValues[2].replace(",", "").trim()
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            return ParsedNotification("Plin", sender, amount)
        }

        // 3. Generic backup pattern for banks sending SMS/Notifications
        // Example: "Juan Perez te envió S/ 15.00"
        val genericRegex = Regex("""(.+?)\s+te (?:envió|transfirió|yapeó)\s+S/(\s*[\d,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        val genericMatch = genericRegex.find(text)
        if (genericMatch != null) {
            val sender = genericMatch.groupValues[1].trim()
            val amountStr = genericMatch.groupValues[2].replace(",", "").trim()
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val app = if (packageName.contains("yape", ignoreCase = true)) "Yape" else "Plin"
            return ParsedNotification(app, sender, amount)
        }

        return null
    }

    private suspend fun processTransaction(packageName: String, parsed: ParsedNotification, rawText: String) {
        val database = NotificationDatabase.getDatabase(applicationContext)
        val dao = database.notificationDao()

        // 1. Save initial log to database
        var log = NotificationLog(
            appName = parsed.appName,
            packageName = packageName,
            sender = parsed.sender,
            amount = parsed.amount,
            rawText = rawText,
            mqttSent = false,
            whatsappSent = false,
            transactionCode = parsed.transactionCode
        )
        val logId = dao.insertLog(log)
        log = log.copy(id = logId)

        // 2. Build structured JSON payload
        val payloadMap = mutableMapOf<String, Any>(
            "id" to log.id,
            "appName" to log.appName,
            "sender" to log.sender,
            "amount" to log.amount,
            "timestamp" to log.timestamp,
            "rawText" to log.rawText
        )
        log.transactionCode?.let { payloadMap["transactionCode"] = it }
        val payloadJson = gson.toJson(payloadMap)

        // 3. Publish to MQTT Broker
        val mqttHelper = MqttClientHelper(applicationContext)
        val mqttResult = mqttHelper.publishNotification(payloadJson)
        var mqttSent = false
        var mqttError: String? = null
        if (mqttResult.isSuccess) {
            mqttSent = true
        } else {
            mqttError = mqttResult.exceptionOrNull()?.message ?: "Unknown MQTT error"
        }

        // 4. Send to WhatsApp Webhook
        // We construct a user-friendly message
        var messageText = "🔔 *Checkealo Pago Recibido*\n" +
                "📱 *App:* ${parsed.appName}\n" +
                "👤 *Emisor:* ${parsed.sender}\n" +
                "💰 *Monto:* S/ ${String.format("%.2f", parsed.amount)}\n"
        parsed.transactionCode?.let {
            messageText += "🔑 *Código:* $it\n"
        }
        messageText += "🕒 *Fecha:* ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(log.timestamp)}"

        val whatsappHelper = WhatsAppClientHelper(applicationContext)
        val whatsappResult = whatsappHelper.sendMessage(messageText)
        var whatsappSent = false
        var whatsappError: String? = null
        if (whatsappResult.isSuccess) {
            whatsappSent = true
        } else {
            whatsappError = whatsappResult.exceptionOrNull()?.message ?: "Unknown WhatsApp error"
        }

        // 5. Update log in Database with success/error status
        val updatedLog = log.copy(
            mqttSent = mqttSent,
            mqttError = mqttError,
            whatsappSent = whatsappSent,
            whatsappError = whatsappError
        )
        dao.updateLog(updatedLog)
        Log.d(TAG, "Transaction processed and database updated. MQTT=$mqttSent, WhatsApp=$whatsappSent")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Notification Listener destroyed!")
    }
}
