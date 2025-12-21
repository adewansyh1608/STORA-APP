package com.example.stora.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.stora.data.LoansData
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.utils.FileUtils
import com.example.stora.viewmodel.LoanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailLoanScreen(
    navController: NavHostController,
    loanId: String,
    loanViewModel: LoanViewModel = viewModel()
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by loanViewModel.isLoading.collectAsState()
    
    // Load loan data from Room - use a refresh trigger to reload when coming back
    var loanWithItems by remember { mutableStateOf<com.example.stora.data.LoanWithItems?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    // Reload loan data when loanId changes OR when refreshTrigger changes (e.g., after navigation back)
    LaunchedEffect(loanId, refreshTrigger) {
        if (loanId.isNotEmpty()) {
            loanWithItems = loanViewModel.getLoanById(loanId)
            android.util.Log.d("DetailLoanScreen", "Loaded loan: ${loanWithItems?.loan?.tanggalKembali}")
        }
    }
    
    // Observe active loans to detect changes (when coming back from edit)
    val activeLoans by loanViewModel.activeLoans.collectAsState()
    
    // Reload when activeLoans changes (triggered by edit/sync operations)
    LaunchedEffect(activeLoans) {
        if (loanId.isNotEmpty()) {
            val updatedLoan = loanViewModel.getLoanById(loanId)
            if (updatedLoan != null && updatedLoan.loan.tanggalKembali != loanWithItems?.loan?.tanggalKembali) {
                loanWithItems = updatedLoan
                android.util.Log.d("DetailLoanScreen", "Loan updated from observer: ${updatedLoan.loan.tanggalKembali}")
            }
        }
    }
    
    // Map untuk menyimpan return image per item - now uses String item ID
    val returnImageUris = remember { mutableStateMapOf<String, Uri?>() }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedItemForReturn by remember { mutableStateOf<String?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    
    // Return date and time states
    val calendar = remember { java.util.Calendar.getInstance() }
    val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val timeSdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    var returnDate by remember { mutableStateOf(sdf.format(calendar.time)) }
    var returnTime by remember { mutableStateOf(timeSdf.format(calendar.time)) }
    var showReturnDatePicker by remember { mutableStateOf(false) }
    var showReturnTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Derive data from Room loanWithItems
    val loan = loanWithItems?.loan
    val loanItems = loanWithItems?.items ?: emptyList()
    
    val textGray = Color(0xFF585858)
    
    // Check if edit is allowed (only until 1 hour before deadline)
    // Recompute when loanWithItems changes
    val isEditAllowed = remember(loanWithItems?.loan?.tanggalKembali) {
        val l = loanWithItems?.loan
        if (l == null) {
            android.util.Log.d("DetailLoanScreen", "isEditAllowed: loan is null, returning false")
            return@remember false
        }
        
        try {
            val deadline = l.tanggalKembali
            android.util.Log.d("DetailLoanScreen", "Checking isEditAllowed for deadline: $deadline")
            
            // Support multiple date formats
            val dateFormats = listOf(
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()),
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()),
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()),
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()),
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            )
            
            // Try each format until one works
            var deadlineDate: java.util.Date? = null
            var usedFormat: java.text.SimpleDateFormat? = null
            for (format in dateFormats) {
                try {
                    format.isLenient = false
                    val parsed = format.parse(deadline)
                    if (parsed != null) {
                        deadlineDate = parsed
                        usedFormat = format
                        android.util.Log.d("DetailLoanScreen", "Successfully parsed with format: ${format.toPattern()}")
                        break
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }
            
            if (deadlineDate == null) {
                android.util.Log.e("DetailLoanScreen", "Failed to parse deadline with any format: $deadline")
                return@remember false
            }
            
            // Determine if the format has time component
            val hasTimeComponent = deadline.contains(":") || 
                (usedFormat?.toPattern()?.contains("HH") == true)
            
            // Calculate the cutoff time (1 hour before deadline)
            val cutoffTime = java.util.Calendar.getInstance().apply {
                time = deadlineDate
                // If no time was specified in deadline, set to end of day first
                if (!hasTimeComponent) {
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                }
                // Subtract 1 hour from the deadline
                add(java.util.Calendar.HOUR_OF_DAY, -1)
            }
            
            val now = java.util.Date()
            val allowed = now.before(cutoffTime.time)
            android.util.Log.d("DetailLoanScreen", "Edit allowed: $allowed (now: $now, cutoff: ${cutoffTime.time}, deadline: $deadline)")
            allowed
        } catch (e: Exception) {
            android.util.Log.e("DetailLoanScreen", "Error parsing deadline: ${e.message}")
            false
        }
    }
    
    // Image picker launcher for return photo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedItemForReturn?.let { itemId ->
            returnImageUris[itemId] = uri
        }
        selectedItemForReturn = null
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedItemForReturn?.let { itemId ->
                returnImageUris[itemId] = tempCameraUri
            }
            selectedItemForReturn = null
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempCameraUri = FileUtils.createImageUri(context)
            tempCameraUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    if (loan == null) {
        // Show loading indicator while loan is being fetched
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = StoraBlueDark)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading loan data...", color = StoraBlueDark)
            }
        }
        return
    }

    Scaffold(
        containerColor = StoraWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Items On Loan",
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
                actions = {
                    // Edit button - only enabled until 1 hour before deadline
                    IconButton(
                        onClick = { 
                            // loanId is already the roomLoanId
                            navController.navigate("edit_loan/$loanId")
                        },
                        enabled = isEditAllowed
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = if (isEditAllowed) StoraWhite else StoraWhite.copy(alpha = 0.5f)
                        )
                    }
                    // Delete button - only enabled until 1 hour before deadline
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = isEditAllowed
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = if (isEditAllowed) StoraWhite else StoraWhite.copy(alpha = 0.5f)
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
            // White content area with rounded top corners
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 32.dp, bottom = 24.dp)
                    ) {
                        // Borrower Name at the top
                        Text(
                            text = loan.namaPeminjam,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoraBlueDark
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Phone Number
                        if (loan.noHpPeminjam.isNotEmpty()) {
                            Text(
                                text = loan.noHpPeminjam,
                                fontSize = 14.sp,
                                color = textGray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        // Borrow Date
                        Text(
                            text = loan.tanggalPinjam,
                            fontSize = 14.sp,
                            color = textGray,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Tanggal Pengembalian
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dipinjam",
                                    fontSize = 12.sp,
                                    color = textGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loan.tanggalPinjam,
                                    fontSize = 13.sp,
                                    color = StoraBlueDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dikembalikan",
                                    fontSize = 12.sp,
                                    color = textGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loan.tanggalKembali,
                                    fontSize = 13.sp,
                                    color = StoraBlueDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        // Items Details Section
                        Text(
                            text = "Detail Barang (${loanItems.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StoraBlueDark
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Display all items in the group
                        loanItems.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Item Name
                                    Text(
                                        text = item.namaBarang,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StoraBlueDark
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Item Code and Quantity
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Kode",
                                                fontSize = 11.sp,
                                                color = textGray,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = item.kodeBarang,
                                                fontSize = 12.sp,
                                                color = StoraBlueDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Jumlah",
                                                fontSize = 11.sp,
                                                color = textGray,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Widgets,
                                                    contentDescription = "Jumlah",
                                                    tint = StoraBlueDark,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${item.jumlah}",
                                                    fontSize = 12.sp,
                                                    color = StoraBlueDark,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Photo Section - Loan Photo
                                    if (item.imageUri != null) {
                                        Text(
                                            text = "Foto Barang",
                                            fontSize = 11.sp,
                                            color = textGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE8E8E8))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFFD0D0D0),
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(Uri.parse(item.imageUri)),
                                                contentDescription = "Item Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Return Photo Section - Per Item
                                    Text(
                                        text = "Foto Pengembalian",
                                        fontSize = 11.sp,
                                        color = textGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF5F5F5))
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFFE0E0E0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedItemForReturn = item.id
                                                showImagePickerDialog = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val returnUri = returnImageUris[item.id]
                                        if (returnUri != null) {
                                            Image(
                                                painter = rememberAsyncImagePainter(returnUri),
                                                contentDescription = "Return Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CameraAlt,
                                                    contentDescription = "Upload",
                                                    tint = Color(0xFF9E9E9E),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Klik untuk foto",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF9E9E9E)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Return Date and Time Section
                        Text(
                            text = "Tanggal & Jam Pengembalian",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StoraBlueDark
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Return Date
                            OutlinedTextField(
                                value = returnDate,
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
                                            .clickable { showReturnDatePicker = true }
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
                            
                            // Return Time
                            OutlinedTextField(
                                value = returnTime,
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
                                            .clickable { showReturnTimePicker = true }
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
                        
                        // Return Button
                        Button(
                            onClick = {
                                // Combine return date and time
                                val fullReturnDateTime = "$returnDate $returnTime"
                                
                                // Prepare item return images map using item id
                                val itemReturnImages = loanItems.associate { item ->
                                    item.id to returnImageUris[item.id]?.toString()
                                }
                                
                                // Call ViewModel to update Room and sync
                                loanViewModel.returnLoan(
                                    loanId = loanId,
                                    returnDateTime = fullReturnDateTime,
                                    itemReturnImages = itemReturnImages,
                                    onSuccess = {
                                        // Navigate back to LoansScreen
                                        navController.popBackStack(Routes.LOANS_SCREEN, false)
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
                                    text = "Kembalikan Barang",
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

        // Image Picker Dialog
        if (showImagePickerDialog) {
            DetailLoanImagePickerDialog(
                onDismiss = { showImagePickerDialog = false },
                onGalleryClick = {
                    showImagePickerDialog = false
                    imagePickerLauncher.launch("image/*")
                },
                onCameraClick = {
                    showImagePickerDialog = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
        
        // Return Date Picker
        if (showReturnDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showReturnDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            returnDate = sdf.format(java.util.Date(millis))
                        }
                        showReturnDatePicker = false
                    }) {
                        Text("OK", color = StoraBlueDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReturnDatePicker = false }) {
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
        
        // Return Time Picker
        if (showReturnTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
                initialMinute = calendar.get(java.util.Calendar.MINUTE),
                is24Hour = true
            )
            
            AlertDialog(
                onDismissRequest = { showReturnTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        returnTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showReturnTimePicker = false
                    }) {
                        Text("OK", color = StoraBlueDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReturnTimePicker = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                title = { Text("Pilih Jam Pengembalian") },
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
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Peminjaman?")
                }
            },
            text = {
                Text("Peminjaman ini akan dihapus. Tindakan ini tidak dapat dibatalkan.")
            },
                confirmButton = {
                    Button(
                        onClick = {
                            // Use loanId directly
                            loanViewModel.deleteLoan(
                                loanId = loanId,
                                onSuccess = {
                                    showDeleteDialog = false
                                    navController.popBackStack(Routes.LOANS_SCREEN, false)
                                },
                                onError = { error ->
                                    showDeleteDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Error: $error",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = StoraWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Hapus", color = StoraWhite)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailLoanImagePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Pilih Sumber Gambar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Gallery Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onGalleryClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(StoraBlueDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Pilih dari Galeri",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCameraClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(StoraBlueDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Ambil Foto",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
