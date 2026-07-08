# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Ginita\AppData\Local\Android\sdk/proguard-android.txt
# You can edit the include path and share the file with other subprojects.

# Keep Room library classes
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.MultiInstanceInvalidationService { *; }

# Keep MQTT client
-keep class org.eclipse.paho.client.mqttv3.** { *; }

# Keep GSON classes and model classes
-keep class com.google.gson.** { *; }
-keep class com.checkealo.app.data.NotificationLog { *; }
-keep class com.checkealo.app.service.NotificationService$ParsedNotification { *; }
