package com.example.stora.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.stora.data.*
import com.example.stora.network.ApiConfig
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.utils.TokenManager
import com.example.stora.viewmodel.NotificationViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    navController: NavHostController,
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = ApiConfig.provideApiService()
    val tokenManager = TokenManager.getInstance(context)

    // Collect state from ViewModel (data from Room database)
    val reminders by viewModel.reminders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var fcmToken by remember { mutableStateOf<String?>(null) }
    var showPeriodicDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Izin notifikasi diberikan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    // Check and request notification permission
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Get FCM token and register
    LaunchedEffect(Unit) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            fcmToken = token
            Log.d("ReminderSettings", "FCM Token: $token")

            // Save token to SharedPreferences
            context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply()

            // Register token with backend (only if online)
            val authHeader = tokenManager.getAuthHeader()
            if (authHeader != null) {
                try {
                    apiService.registerFcmToken(authHeader, FcmTokenRequest(token))
                } catch (e: Exception) {
                    Log.e("ReminderSettings", "Error registering FCM token (offline?)", e)
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderSettings", "Error getting FCM token", e)
        }
    }

    // Sync on screen load
    LaunchedEffect(Unit) {
        viewModel.syncData()
    }

    // Refresh network status periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            viewModel.refreshNetworkStatus()
        }
    }

    // Create or update periodic reminder using ViewModel (works offline!)
    fun createPeriodicReminder(months: Int) {
        viewModel.savePeriodicReminder(
            months = months,
            onSuccess = {
                val statusMsg = if (isOnline) "Pengingat periodik berhasil disimpan" else "Tersimpan lokal (akan sync)"
                Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
        showPeriodicDialog = false
    }

    // Create custom reminder using ViewModel (works offline!)
    fun createCustomReminder(datetime: String, title: String) {
        // Parse datetime string to timestamp
        val timestamp = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(datetime)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        viewModel.saveCustomReminder(
            title = title,
            scheduledDatetime = timestamp,
            onSuccess = {
                val statusMsg = if (isOnline) "Pengingat kustom berhasil dibuat" else "Tersimpan lokal (akan sync)"
                Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
        showCustomDialog = false
    }

    // Delete reminder using ViewModel (works offline!)
    fun deleteReminder(id: String) {
        viewModel.deleteReminder(
            id = id,
            onSuccess = {
                Toast.makeText(context, "Pengingat dihapus", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Toggle reminder using ViewModel (works offline!)
    fun toggleReminder(reminder: ReminderEntity) {
        viewModel.toggleReminder(
            reminder = reminder,
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StoraBlueDark)
            .statusBarsPadding()
    ) {
        // Top Bar with proper back navigation
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pengingat Inventory",
                        color = StoraYellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Show offline indicator
                    if (!isOnline) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = "Offline",
                            tint = Color.Yellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(StoraWhite)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StoraBlueDark)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Check if periodic reminder already exists
                    val existingPeriodicReminder = reminders.find { it.reminderType == "periodic" }

                    // Action buttons
                    item {
                        Text(
                            text = if (existingPeriodicReminder != null) "Pengaturan Pengingat" else "Buat Pengingat Baru",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoraBlueDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showPeriodicDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StoraBlueDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Repeat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (existingPeriodicReminder != null) "Ubah Periodik" else "Periodik")
                            }

                            Button(
                                onClick = { showCustomDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StoraYellow),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = StoraBlueDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kustom", color = StoraBlueDark)
                            }
                        }

                        // Show current periodic setting if exists
                        if (existingPeriodicReminder != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pengingat periodik aktif: Setiap ${existingPeriodicReminder.periodicMonths} bulan",
                                        fontSize = 14.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    // Reminders list
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pengingat Aktif (${reminders.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoraBlueDark
                        )
                    }

                    if (reminders.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada pengingat.\nBuat pengingat baru untuk memulai.",
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(reminders.filter { !it.isDeleted }) { reminder ->
                            ReminderCardEntity(
                                reminder = reminder,
                                onToggle = { toggleReminder(reminder) },
                                onDelete = { deleteReminder(reminder.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Periodic Dialog
    if (showPeriodicDialog) {
        val existingPeriodicReminder = reminders.find { it.reminderType == "periodic" }
        PeriodicReminderDialog(
            currentMonths = existingPeriodicReminder?.periodicMonths ?: 3,
            isUpdate = existingPeriodicReminder != null,
            onDismiss = { showPeriodicDialog = false },
            onConfirm = { months -> createPeriodicReminder(months) }
        )
    }

    // Custom Dialog
    if (showCustomDialog) {
        CustomReminderDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { datetime, title -> createCustomReminder(datetime, title) }
        )
    }
}

@Composable
fun ReminderCardEntity(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isActive) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (reminder.reminderType == "periodic") Icons.Default.Repeat else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (reminder.isActive) StoraBlueDark else Color.Gray,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.title ?: "Pengingat Inventory",
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isActive) StoraBlueDark else Color.Gray
                    )
                    // Show sync status indicator
                    if (reminder.needsSync) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Belum tersinkron",
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = if (reminder.reminderType == "periodic") {
                        "Setiap ${reminder.periodicMonths} bulan"
                    } else {
                        formatDateTimeLong(reminder.scheduledDatetime)
                    },
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                if (!reminder.isActive) {
                    Text(
                        text = "Nonaktif",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            }

            Switch(
                checked = reminder.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StoraYellow,
                    checkedTrackColor = StoraBlueDark
                )
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun PeriodicReminderDialog(
    currentMonths: Int = 3,
    isUpdate: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedMonths by remember { mutableIntStateOf(currentMonths) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StoraWhite)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isUpdate) "Ubah Pengingat Periodik" else "Pengingat Periodik",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ingatkan saya setiap:",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { if (selectedMonths > 1) selectedMonths-- }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                    }

                    Text(
                        text = "$selectedMonths",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoraBlueDark,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    IconButton(
                        onClick = { if (selectedMonths < 12) selectedMonths++ }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }

                Text(
                    text = "bulan",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                if (isUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hitungan akan dimulai dari sekarang",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = { onConfirm(selectedMonths) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StoraBlueDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isUpdate) "Perbarui" else "Simpan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val calendar = remember { Calendar.getInstance() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StoraWhite)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pengingat Kustom",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date selection
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${selectedDay}/${selectedMonth + 1}/${selectedYear}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time selection
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(String.format("%02d:%02d", selectedHour, selectedMinute))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                            }
                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            val datetime = sdf.format(cal.time)
                            onConfirm(datetime, title)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StoraBlueDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        selectedYear = cal.get(Calendar.YEAR)
                        selectedMonth = cal.get(Calendar.MONTH)
                        selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Batal")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

private fun formatDateTime(datetime: String?): String {
    if (datetime == null) return "Tidak diatur"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(datetime)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        outputFormat.format(date!!)
    } catch (e: Exception) {
        datetime
    }
}

// Overload for Long timestamp (used by ReminderEntity)
private fun formatDateTimeLong(timestamp: Long?): String {
    if (timestamp == null) return "Tidak diatur"
    return try {
        val date = Date(timestamp)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        outputFormat.format(date)
    } catch (e: Exception) {
        "Tidak valid"
    }
}
