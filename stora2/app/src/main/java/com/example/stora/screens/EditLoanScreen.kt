package com.example.stora.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.viewmodel.LoanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanScreen(
    navController: NavHostController,
    loanId: String,
    loanViewModel: LoanViewModel = viewModel()
) {
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by loanViewModel.isLoading.collectAsState()
    
    // Get current loan data
    var loanWithItems by remember { mutableStateOf<com.example.stora.data.LoanWithItems?>(null) }
    
    LaunchedEffect(loanId) {
        loanWithItems = loanViewModel.getLoanById(loanId)
    }
    
    // Date/Time states
    val calendar = remember { java.util.Calendar.getInstance() }
    val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val timeSdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    
    // Initialize with current deadline or current date
    var deadlineDate by remember { mutableStateOf(sdf.format(calendar.time)) }
    var deadlineTime by remember { mutableStateOf(timeSdf.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Update when loan data is loaded
    LaunchedEffect(loanWithItems) {
        loanWithItems?.loan?.tanggalKembali?.let { deadline ->
            if (deadline.contains(":")) {
                val parts = deadline.split(" ")
                if (parts.size >= 2) {
                    deadlineDate = parts[0]
                    deadlineTime = parts[1]
                }
            } else {
                deadlineDate = deadline
            }
        }
    }
    
    val textGray = Color(0xFF585858)
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    Scaffold(
        containerColor = StoraWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Peminjaman",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = StoraYellow
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StoraWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StoraBlueDark
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(StoraBlueDark)
                .padding(top = 24.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(animationSpec = tween(800, delayMillis = 200)) { it } + fadeIn(animationSpec = tween(800, delayMillis = 200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StoraWhite,
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                ) {
                    if (loanWithItems == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = StoraBlueDark)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 32.dp, bottom = 24.dp)
                        ) {
                            val loan = loanWithItems!!.loan
                            val items = loanWithItems!!.items
                            
                            // Borrower Info (read-only)
                            Text(
                                text = loan.namaPeminjam,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = StoraBlueDark
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (loan.noHpPeminjam.isNotEmpty()) {
                                Text(
                                    text = loan.noHpPeminjam,
                                    fontSize = 14.sp,
                                    color = textGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Edit Deadline Section
                            Text(
                                text = "Ubah Deadline",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StoraBlueDark
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Date
                                OutlinedTextField(
                                    value = deadlineDate,
                                    onValueChange = {},
                                    modifier = Modifier.weight(1f),
                                    readOnly = true,
                                    label = { Text("Tanggal", fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarToday,
                                            contentDescription = "Calendar",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { showDatePicker = true }
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF9F9F9),
                                        unfocusedContainerColor = Color(0xFFF9F9F9),
                                        focusedBorderColor = StoraBlueDark,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                                
                                // Time
                                OutlinedTextField(
                                    value = deadlineTime,
                                    onValueChange = {},
                                    modifier = Modifier.weight(0.7f),
                                    readOnly = true,
                                    label = { Text("Jam", fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.AccessTime,
                                            contentDescription = "Time",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { showTimePicker = true }
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF9F9F9),
                                        unfocusedContainerColor = Color(0xFFF9F9F9),
                                        focusedBorderColor = StoraBlueDark,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Current Items (Display only for now)
                            Text(
                                text = "Barang Dipinjam (${items.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StoraBlueDark
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            items.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = item.namaBarang,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = StoraBlueDark
                                            )
                                            Text(
                                                text = item.kodeBarang,
                                                fontSize = 12.sp,
                                                color = textGray
                                            )
                                        }
                                        Text(
                                            text = "Qty: ${item.jumlah}",
                                            fontSize = 12.sp,
                                            color = textGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Save Button
                            Button(
                                onClick = {
                                    val newDeadline = "$deadlineDate $deadlineTime"
                                    loanViewModel.updateLoan(
                                        loanId = loanId,
                                        newDeadline = newDeadline,
                                        newItems = null, // Not changing items for now
                                        onSuccess = {
                                            navController.popBackStack()
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Error: $error",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StoraYellowButton
                                ),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = StoraBlueDark
                                    )
                                } else {
                                    Text(
                                        text = "Simpan Perubahan",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StoraBlueDark
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
        
        // Date Picker
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            deadlineDate = sdf.format(java.util.Date(millis))
                        }
                        showDatePicker = false
                    }) {
                        Text("OK", color = StoraBlueDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    showModeToggle = false
                )
            }
        }
        
        // Time Picker
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
                initialMinute = calendar.get(java.util.Calendar.MINUTE),
                is24Hour = true
            )
            
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        deadlineTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }) {
                        Text("OK", color = StoraBlueDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                title = { Text("Pilih Jam Deadline") },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = timePickerState)
                    }
                }
            )
        }
    }
}
