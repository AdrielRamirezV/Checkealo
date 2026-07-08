package com.checkealo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val sender: String,
    val amount: Double,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mqttSent: Boolean = false,
    val whatsappSent: Boolean = false,
    val mqttError: String? = null,
    val whatsappError: String? = null
)
