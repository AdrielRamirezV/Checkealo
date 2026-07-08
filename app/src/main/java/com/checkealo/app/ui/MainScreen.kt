package com.checkealo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkealo.app.data.NotificationLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val logs by viewModel.notificationLogs.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val primaryColor = Color(0xFF0F2027)
    val secondaryColor = Color(0xFF203A43)
    val accentColor = Color(0xFF2C5364)
    val tealAccent = Color(0xFF00B4DB)
    val darkBg = Color(0xFF0E1218)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = tealAccent,
            background = darkBg,
            surface = Color(0xFF1E2631),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Checkealo",
                            fontWeight = FontWeight.Bold,
                            color = tealAccent,
                            fontSize = 24.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Limpiar Historial", tint = Color.LightGray)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = primaryColor
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.List, contentDescription = "Historial") },
                        label = { Text("Historial") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tealAccent,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = tealAccent,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                        label = { Text("Ajustes") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tealAccent,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = tealAccent,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Simular") },
                        label = { Text("Simulador") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tealAccent,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = tealAccent,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(darkBg)
            ) {
                when (selectedTab) {
                    0 -> HistoryTab(logs = logs, hasPermission = hasNotificationPermission, onRequestPermission = onRequestPermission)
                    1 -> SettingsTab(viewModel = viewModel, hasPermission = hasNotificationPermission, onRequestPermission = onRequestPermission)
                    2 -> SimulatorTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    logs: List<NotificationLog>,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!hasPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Alerta",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Servicio Desactivado",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Necesitamos acceso para escuchar las notificaciones.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Activar")
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "No hay notificaciones",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Esperando pagos...",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Las transacciones de Yape y Plin aparecerán aquí.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(logs) { log ->
                    NotificationLogCard(log = log)
                }
            }
        }
    }
}

@Composable
fun NotificationLogCard(log: NotificationLog) {
    var showDetails by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(log.timestamp))

    val isYape = log.appName.contains("yape", ignoreCase = true)
    val appBadgeColor = if (isYape) Color(0xFF6B1D7C) else Color(0xFF007A87)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetails = !showDetails }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(appBadgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            log.appName.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formattedDate,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Text(
                    "S/ ${String.format("%.2f", log.amount)}",
                    color = Color(0xFF00C853),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.sender,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.rawText,
                color = Color.LightGray,
                fontSize = 13.sp,
                maxLines = if (showDetails) Int.MAX_VALUE else 1
            )
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MQTT: ", fontSize = 12.sp, color = Color.Gray)
                    Icon(
                        imageVector = if (log.mqttSent) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = "Status MQTT",
                        tint = if (log.mqttSent) Color.Green else Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    if (!log.mqttSent && log.mqttError != null && showDetails) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(log.mqttError, fontSize = 11.sp, color = Color.Red)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("WhatsApp: ", fontSize = 12.sp, color = Color.Gray)
                    Icon(
                        imageVector = if (log.whatsappSent) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = "Status WhatsApp",
                        tint = if (log.whatsappSent) Color.Green else Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    if (!log.whatsappSent && log.whatsappError != null && showDetails) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(log.whatsappError, fontSize = 11.sp, color = Color.Red)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    val mqttHost by viewModel.mqttHost.collectAsState()
    val mqttPort by viewModel.mqttPort.collectAsState()
    val mqttTopic by viewModel.mqttTopic.collectAsState()
    val mqttClientId by viewModel.mqttClientId.collectAsState()
    val mqttUsername by viewModel.mqttUsername.collectAsState()
    val mqttPassword by viewModel.mqttPassword.collectAsState()
    val mqttEnabled by viewModel.mqttEnabled.collectAsState()

    val whatsappApiUrl by viewModel.whatsappApiUrl.collectAsState()
    val whatsappToken by viewModel.whatsappToken.collectAsState()
    val whatsappPhone by viewModel.whatsappPhone.collectAsState()
    val whatsappEnabled by viewModel.whatsappEnabled.collectAsState()

    val mqttTestStatus by viewModel.mqttTestStatus.collectAsState()
    val whatsappTestStatus by viewModel.whatsappTestStatus.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

    // Form inputs
    var hostInput by remember { mutableStateOf(mqttHost) }
    var portInput by remember { mutableStateOf(mqttPort.toString()) }
    var topicInput by remember { mutableStateOf(mqttTopic) }
    var clientIdInput by remember { mutableStateOf(mqttClientId) }
    var userInput by remember { mutableStateOf(mqttUsername) }
    var passInput by remember { mutableStateOf(mqttPassword) }
    var mqttEnableInput by remember { mutableStateOf(mqttEnabled) }

    var waUrlInput by remember { mutableStateOf(whatsappApiUrl) }
    var waTokenInput by remember { mutableStateOf(whatsappToken) }
    var waPhoneInput by remember { mutableStateOf(whatsappPhone) }
    var waEnableInput by remember { mutableStateOf(whatsappEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Permissions Group
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permisos del Sistema", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Acceso a Notificaciones", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (hasPermission) "Autorizado" else "Falta autorización",
                            color = if (hasPermission) Color.Green else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasPermission) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (hasPermission) "Revisar" else "Otorgar")
                    }
                }
            }
        }

        // MQTT Group
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Broker MQTT", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Switch(
                        checked = mqttEnableInput,
                        onCheckedChange = { 
                            mqttEnableInput = it
                            viewModel.updateMqttSettings(hostInput, portInput.toIntOrNull() ?: 1883, topicInput, clientIdInput, userInput, passInput, it)
                        }
                    )
                }
                
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Broker Host") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text("Broker Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    label = { Text("MQTT Topic") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text("MQTT Client ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Username (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passInput,
                    onValueChange = { passInput = it },
                    label = { Text("Password (opcional)") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Lock else Icons.Default.Lock, // Replace with dynamic if desired
                                contentDescription = "Mostrar contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewModel.updateMqttSettings(
                                hostInput,
                                portInput.toIntOrNull() ?: 1883,
                                topicInput,
                                clientIdInput,
                                userInput,
                                passInput,
                                mqttEnableInput
                            )
                        }
                    ) {
                        Text("Guardar MQTT")
                    }
                    Button(
                        onClick = { viewModel.testMqttConnection() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Probar Conexión")
                    }
                }
                if (mqttTestStatus != null) {
                    Text(
                        mqttTestStatus!!,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // WhatsApp Group
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("WhatsApp Gateway", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Switch(
                        checked = waEnableInput,
                        onCheckedChange = { 
                            waEnableInput = it
                            viewModel.updateWhatsappSettings(waUrlInput, waTokenInput, waPhoneInput, it)
                        }
                    )
                }

                OutlinedTextField(
                    value = waUrlInput,
                    onValueChange = { waUrlInput = it },
                    label = { Text("API Webhook URL") },
                    placeholder = { Text("https://api.gateway.com/send") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = waTokenInput,
                    onValueChange = { waTokenInput = it },
                    label = { Text("Authorization Token / API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = waPhoneInput,
                    onValueChange = { waPhoneInput = it },
                    label = { Text("Destination Phone Number") },
                    placeholder = { Text("51987654321") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewModel.updateWhatsappSettings(
                                waUrlInput,
                                waTokenInput,
                                waPhoneInput,
                                waEnableInput
                            )
                        }
                    ) {
                        Text("Guardar WhatsApp")
                    }
                    Button(
                        onClick = { viewModel.testWhatsappMessage() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Enviar Prueba")
                    }
                }
                if (whatsappTestStatus != null) {
                    Text(
                        whatsappTestStatus!!,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatorTab(viewModel: MainViewModel) {
    var senderName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var simulateStatus by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Simulador de Notificaciones",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Utiliza este simulador para generar una notificación de Yape/Plin falsa. Esto te permite testear si tu MQTT y WhatsApp funcionan sin recibir un pago real.",
                fontSize = 13.sp,
                color = Color.LightGray
            )

            OutlinedTextField(
                value = senderName,
                onValueChange = { senderName = it },
                label = { Text("Nombre del Emisor") },
                placeholder = { Text("Juan Perez") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Monto (S/)") },
                placeholder = { Text("20.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (senderName.isNotEmpty() && amount != null) {
                        viewModel.triggerMockNotification(senderName, amount)
                        simulateStatus = "Notificación enviada! Revisa en 1 segundo."
                    } else {
                        simulateStatus = "Ingresa datos válidos."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generar Notificación de Prueba")
            }

            if (simulateStatus != null) {
                Text(
                    simulateStatus!!,
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
