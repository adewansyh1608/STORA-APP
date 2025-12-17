package com.example.stora.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.stora.data.InventoryItem
import com.example.stora.viewmodel.InventoryViewModel
import com.example.stora.viewmodel.LoanViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stora.data.LoansData
import com.example.stora.repository.LoanItemInfo
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.utils.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


data class LoanFormItem(
    val inventoryItem: InventoryItem,
    var quantity: Int,
    var imageUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormScreen(
    navController: NavHostController,
    selectedItemIds: List<String>,
    inventoryViewModel: InventoryViewModel = viewModel(),
    loanViewModel: LoanViewModel = viewModel()
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    val inventoryItems by inventoryViewModel.inventoryItems.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by loanViewModel.isLoading.collectAsState()

    // Form states
    var borrowerName by remember { mutableStateOf("") }
    var borrowerPhone by remember { mutableStateOf("") }
    var borrowDate by remember { mutableStateOf("") }
    var borrowTime by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var returnTime by remember { mutableStateOf("") }
    var showBorrowDatePicker by remember { mutableStateOf(false) }
    var showBorrowTimePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }
    var showReturnTimePicker by remember { mutableStateOf(false) }
    var selectedItemForPhoto by remember { mutableStateOf<Int?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }


    // Selected items with quantities
    val loanItems = remember(inventoryItems, selectedItemIds) {
        mutableStateListOf<LoanFormItem>().apply {
            selectedItemIds.forEach { id ->
                inventoryItems.find { it.id == id }?.let { item ->
                    val availableQty = LoansData.getAvailableQuantity(item)
                    if (availableQty > 0) {
                        add(LoanFormItem(item, 1))
                    }
                }
            }
        }
    }

    val textGray = Color(0xFF585858)

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedItemForPhoto?.let { index ->
            if (index >= 0 && index < loanItems.size) {
                loanItems[index] = loanItems[index].copy(imageUri = uri)
            }
        }
        selectedItemForPhoto = null
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedItemForPhoto?.let { index ->
                if (index >= 0 && index < loanItems.size) {
                    loanItems[index] = loanItems[index].copy(imageUri = tempCameraUri)
                }
            }
            selectedItemForPhoto = null
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

    Scaffold(
        containerColor = StoraWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Loans",
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 24.dp)
                ) {
                    Text(
                        text = "Lengkapi Data Peminjaman Barang",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoraBlueDark,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Selected Items with Quantity Controls
                    Text(
                        text = "Barang yang Dipinjam",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StoraBlueDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    loanItems.forEach { loanItem ->
                        LoanItemQuantityCard(
                            loanFormItem = loanItem,
                            onQuantityChange = { newQuantity ->
                                val index = loanItems.indexOf(loanItem)
                                if (index != -1) {
                                    loanItems[index] = loanItem.copy(quantity = newQuantity)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Borrower Name
                    Text(
                        text = "Nama Peminjam",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = borrowerName,
                        onValueChange = { borrowerName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Masukkan nama peminjam") },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = StoraWhite,
                            unfocusedContainerColor = StoraWhite,
                            focusedIndicatorColor = StoraBlueDark,
                            unfocusedIndicatorColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Number
                    Text(
                        text = "Nomor HP Peminjam",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = borrowerPhone,
                        onValueChange = { borrowerPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Masukkan nomor HP") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = "Phone",
                                tint = textGray,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = StoraWhite,
                            unfocusedContainerColor = StoraWhite,
                            focusedIndicatorColor = StoraBlueDark,
                            unfocusedIndicatorColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Borrow Date and Time Section
                    Text(
                        text = "Tanggal & Jam Peminjaman",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Borrow Date
                        OutlinedTextField(
                            value = borrowDate,
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            placeholder = { Text("Pilih tanggal", fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = "Calendar",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { showBorrowDatePicker = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = StoraWhite,
                                unfocusedContainerColor = StoraWhite,
                                focusedIndicatorColor = StoraBlueDark,
                                unfocusedIndicatorColor = Color(0xFFE0E0E0)
                            )
                        )
                        
                        // Borrow Time
                        OutlinedTextField(
                            value = borrowTime,
                            onValueChange = {},
                            modifier = Modifier.weight(0.7f),
                            readOnly = true,
                            placeholder = { Text("Jam", fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.AccessTime,
                                    contentDescription = "Time",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { showBorrowTimePicker = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = StoraWhite,
                                unfocusedContainerColor = StoraWhite,
                                focusedIndicatorColor = StoraBlueDark,
                                unfocusedIndicatorColor = Color(0xFFE0E0E0)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Return Date and Time Section (Deadline)
                    Text(
                        text = "Tanggal & Jam Deadline Pengembalian",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
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
                            placeholder = { Text("Pilih tanggal", fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = "Calendar",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { showReturnDatePicker = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = StoraWhite,
                                unfocusedContainerColor = StoraWhite,
                                focusedIndicatorColor = StoraBlueDark,
                                unfocusedIndicatorColor = Color(0xFFE0E0E0)
                            )
                        )
                        
                        // Return Time
                        OutlinedTextField(
                            value = returnTime,
                            onValueChange = {},
                            modifier = Modifier.weight(0.7f),
                            readOnly = true,
                            placeholder = { Text("Jam", fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.AccessTime,
                                    contentDescription = "Time",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { showReturnTimePicker = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = StoraWhite,
                                unfocusedContainerColor = StoraWhite,
                                focusedIndicatorColor = StoraBlueDark,
                                unfocusedIndicatorColor = Color(0xFFE0E0E0)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show photo upload section based on number of items
                    if (loanItems.size == 1) {
                        // Single item: show photo only for that item
                        val loanItem = loanItems[0]
                        val index = 0

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Foto Barang",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StoraBlueDark,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedItemForPhoto = index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (loanItem.imageUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(loanItem.imageUri),
                                        contentDescription = "Item Photo",
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
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Klik untuk mengambil foto",
                                            fontSize = 13.sp,
                                            color = Color(0xFF9E9E9E),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (loanItems.size >= 2) {
                        // Multiple items: show photo for each item
                        Text(
                            text = "Foto Barang yang Dipinjam",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StoraBlueDark,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        loanItems.forEachIndexed { index, loanItem ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = loanItem.inventoryItem.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StoraBlueDark,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF5F5F5))
                                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedItemForPhoto = index
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loanItem.imageUri != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(loanItem.imageUri),
                                            contentDescription = "Item Photo",
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
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Klik untuk foto",
                                                fontSize = 12.sp,
                                                color = Color(0xFF9E9E9E),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            // Combine date and time
                            val fullBorrowDateTime = "$borrowDate $borrowTime"
                            val fullReturnDateTime = "$returnDate $returnTime"
                            
                            // Prepare LoanItemInfo list for Room storage with individual images
                            val loanItemInfos = loanItems.map { loanFormItem ->
                                LoanItemInfo(
                                    inventarisId = loanFormItem.inventoryItem.serverId ?: 0,
                                    namaBarang = loanFormItem.inventoryItem.name,
                                    kodeBarang = loanFormItem.inventoryItem.noinv,
                                    jumlah = loanFormItem.quantity,
                                    imageUri = loanFormItem.imageUri?.toString()
                                )
                            }

                            // Save to Room and sync to server via ViewModel
                            loanViewModel.createLoan(
                                namaPeminjam = borrowerName,
                                noHpPeminjam = borrowerPhone,
                                tanggalPinjam = fullBorrowDateTime,
                                tanggalKembali = fullReturnDateTime,
                                items = loanItemInfos,
                                onSuccess = {
                                    // ViewModel already syncs Room data to LoansData automatically
                                    // No need to manually add to LoansData here
                                    // Navigate back to loans screen
                                    navController.popBackStack(com.example.stora.navigation.Routes.LOANS_SCREEN, false)
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
                        enabled = borrowerName.isNotBlank() && borrowDate.isNotBlank() && borrowTime.isNotBlank() && returnDate.isNotBlank() && returnTime.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = StoraBlueDark
                            )
                        } else {
                            Text(
                                text = "Simpan",
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

    // Image Picker Dialog
    if (selectedItemForPhoto != null) {
        LoanImagePickerDialog(
            onDismiss = { selectedItemForPhoto = null },
            onGalleryClick = {
                imagePickerLauncher.launch("image/*")
            },
            onCameraClick = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    // Date Pickers
    if (showBorrowDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showBorrowDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        borrowDate = sdf.format(Date(millis))
                    }
                    showBorrowDatePicker = false
                }) {
                    Text("OK", color = StoraBlueDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBorrowDatePicker = false }) {
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

    if (showReturnDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showReturnDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        returnDate = sdf.format(Date(millis))
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

    // Time Pickers
    if (showBorrowTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE),
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showBorrowTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    borrowTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showBorrowTimePicker = false
                }) {
                    Text("OK", color = StoraBlueDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBorrowTimePicker = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            title = { Text("Pilih Jam Peminjaman") },
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

    if (showReturnTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE),
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
            title = { Text("Pilih Jam Deadline Pengembalian") },
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

@Composable
fun LoanItemQuantityCard(
    loanFormItem: LoanFormItem,
    onQuantityChange: (Int) -> Unit
) {
    val textGray = Color(0xFF585858)
    val maxQuantity = LoansData.getAvailableQuantity(loanFormItem.inventoryItem)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = StoraWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(StoraYellow)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Item info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loanFormItem.inventoryItem.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StoraBlueDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = loanFormItem.inventoryItem.noinv,
                    color = textGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tersedia: $maxQuantity",
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            // Quantity controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (loanFormItem.quantity > 1) {
                            onQuantityChange(loanFormItem.quantity - 1)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Kurangi",
                        tint = if (loanFormItem.quantity > 1) StoraBlueDark else Color.Gray
                    )
                }

                Text(
                    text = "${loanFormItem.quantity}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark,
                    modifier = Modifier.widthIn(min = 24.dp)
                )

                IconButton(
                    onClick = {
                        if (loanFormItem.quantity < maxQuantity) {
                            onQuantityChange(loanFormItem.quantity + 1)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Tambah",
                        tint = if (loanFormItem.quantity < maxQuantity) StoraBlueDark else Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanImagePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

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
