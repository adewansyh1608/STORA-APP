package com.example.stora.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.stora.data.LoansData
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.viewmodel.LoanViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanScreen(
    navController: NavHostController,
    loanId: Int,
    loanViewModel: LoanViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by loanViewModel.isLoading.collectAsState()
    
    val textGray = Color(0xFF585858)
    
    // Get loan data
    val loan = remember(loanId) {
        LoansData.loansOnLoan.find { it.id == loanId }
    }
    
    // Get all items with the same groupId
    val loanGroup = remember(loanId) {
        loan?.let { firstLoan ->
            LoansData.loansOnLoan.filter { it.groupId == firstLoan.groupId }
        } ?: emptyList()
    }
    
    // Date format
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeSdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Parse loan's returnDate to get initial values
    val initialDate = remember(loan) {
        loan?.returnDate?.let { returnDateStr ->
            try {
                val parts = returnDateStr.split(" ")
                parts.getOrNull(0) ?: sdf.format(Date())
            } catch (e: Exception) {
                sdf.format(Date())
            }
        } ?: sdf.format(Date())
    }
    
    val initialTime = remember(loan) {
        loan?.returnDate?.let { returnDateStr ->
            try {
                val parts = returnDateStr.split(" ")
                parts.getOrNull(1) ?: "12:00"
            } catch (e: Exception) {
                "12:00"
            }
        } ?: "12:00"
    }
    
    // Edit state - deadline
    var editDeadlineDate by remember { mutableStateOf(initialDate) }
    var editDeadlineTime by remember { mutableStateOf(initialTime) }
    var showEditDatePicker by remember { mutableStateOf(false) }
    var showEditTimePicker by remember { mutableStateOf(false) }
    
    // Edit state - item quantities
    val editItemQuantities = remember { mutableStateMapOf<String, Int>() }
    
    // Initialize edit quantities
    LaunchedEffect(loanGroup) {
        loanGroup.forEach { item ->
            item.roomItemId?.let { itemId ->
                editItemQuantities[itemId] = item.quantity
            }
        }
    }
    
    if (loan == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loan not found")
        }
        return
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
        ) {
            // White content area with rounded top corners
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(StoraWhite)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Borrower Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Peminjam",
                            fontSize = 12.sp,
                            color = textGray
                        )
                        Text(
                            text = loan.borrower ?: "-",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StoraBlueDark
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "No. HP",
                            fontSize = 12.sp,
                            color = textGray
                        )
                        Text(
                            text = loan.borrowerPhone ?: "-",
                            fontSize = 14.sp,
                            color = StoraBlueDark
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Deadline Section
                Text(
                    text = "Deadline Pengembalian",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editDeadlineDate,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        label = { Text("Tanggal") },
                        trailingIcon = {
                            IconButton(onClick = { showEditDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = "Calendar",
                                    tint = StoraBlueDark
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StoraBlueDark,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )
                    
                    OutlinedTextField(
                        value = editDeadlineTime,
                        onValueChange = {},
                        modifier = Modifier.weight(0.7f),
                        readOnly = true,
                        label = { Text("Jam") },
                        trailingIcon = {
                            IconButton(onClick = { showEditTimePicker = true }) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = "Time",
                                    tint = StoraBlueDark
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StoraBlueDark,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Item Quantities Section
                Text(
                    text = "Jumlah Barang Dipinjam",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                loanGroup.forEach { item ->
                    item.roomItemId?.let { itemId ->
                        val currentQty = editItemQuantities[itemId] ?: item.quantity
                        val originalQty = item.quantity
                        val maxAllowed = originalQty
                        val canIncrease = currentQty < maxAllowed
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Item Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StoraBlueDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Kode: ${item.code}",
                                        fontSize = 12.sp,
                                        color = textGray
                                    )
                                    if (!canIncrease) {
                                        Text(
                                            text = "Maksimal tercapai",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE53935)
                                        )
                                    }
                                }
                                
                                // Quantity Controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Minus button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (currentQty > 1) Color(0xFFE8E8E8) else Color(0xFFF0F0F0)
                                            )
                                            .clickable(enabled = currentQty > 1) {
                                                if (currentQty > 1) {
                                                    editItemQuantities[itemId] = currentQty - 1
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "−",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentQty > 1) StoraBlueDark else Color(0xFFBBBBBB)
                                        )
                                    }
                                    
                                    // Quantity display
                                    Box(
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(40.dp)
                                            .background(Color.White, RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$currentQty",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StoraBlueDark
                                        )
                                    }
                                    
                                    // Plus button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (canIncrease) StoraBlueDark else Color(0xFFCCCCCC)
                                            )
                                            .clickable(enabled = canIncrease) {
                                                if (canIncrease) {
                                                    editItemQuantities[itemId] = currentQty + 1
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFE0E0E0))
                    ) {
                        Text("Batal", color = textGray, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    
                    Button(
                        onClick = {
                            val roomLoanId = loanGroup.firstOrNull()?.roomLoanId
                            if (roomLoanId != null) {
                                val newDeadline = "$editDeadlineDate $editDeadlineTime"
                                loanViewModel.updateLoan(
                                    loanId = roomLoanId,
                                    newDeadline = newDeadline,
                                    itemQuantities = editItemQuantities.toMap(),
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Peminjaman berhasil diperbarui",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        // Navigate back to loans list
                                        navController.popBackStack()
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
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StoraYellowButton),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = StoraBlueDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Simpan", color = StoraBlueDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Date Picker
        if (showEditDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showEditDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            editDeadlineDate = sdf.format(Date(millis))
                        }
                        showEditDatePicker = false
                    }) {
                        Text("OK", color = StoraBlueDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDatePicker = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                }
            ) {
                DatePicker(state = datePickerState, showModeToggle = false)
            }
        }
        
        // Time Picker
        if (showEditTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = editDeadlineTime.split(":").getOrNull(0)?.toIntOrNull() ?: 12,
                initialMinute = editDeadlineTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
                is24Hour = true
            )
            
            AlertDialog(
                onDismissRequest = { showEditTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        editDeadlineTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showEditTimePicker = false
                    }) {
                        Text("OK", color = StoraBlueDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTimePicker = false }) {
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
