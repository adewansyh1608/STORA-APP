package com.example.stora.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.stora.navigation.Routes
import com.example.stora.repository.LoanItemInfo
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.utils.FileUtils
import com.example.stora.viewmodel.InventoryViewModel
import com.example.stora.viewmodel.LoanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanScreen(
    navController: NavHostController,
    loanId: String,
    loanViewModel: LoanViewModel = viewModel(),
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by loanViewModel.isLoading.collectAsState()
    
    val inventoryItems by inventoryViewModel.inventoryItems.collectAsState()
    var showAddItemSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var loanWithItems by remember { mutableStateOf<com.example.stora.data.LoanWithItems?>(null) }
    
    val editableItems = remember { mutableStateMapOf<String, Int>() }
    val removedItems = remember { mutableStateListOf<String>() }
    
    val newAddedItems = remember { mutableStateListOf<LoanItemInfo>() }
    
    var selectedNewItemForPhoto by remember { mutableStateOf<Int?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    
    val newItemImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedNewItemForPhoto?.let { index ->
            if (index >= 0 && index < newAddedItems.size) {
                val currentItem = newAddedItems[index]
                newAddedItems[index] = currentItem.copy(imageUri = uri?.toString())
            }
        }
        selectedNewItemForPhoto = null
        showPhotoPickerDialog = false
    }
    
    val newItemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedNewItemForPhoto?.let { index ->
                if (index >= 0 && index < newAddedItems.size) {
                    val currentItem = newAddedItems[index]
                    newAddedItems[index] = currentItem.copy(imageUri = tempCameraUri?.toString())
                }
            }
        }
        selectedNewItemForPhoto = null
        showPhotoPickerDialog = false
    }
    
    val newItemCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempCameraUri = FileUtils.createImageUri(context)
            tempCameraUri?.let { uri ->
                newItemCameraLauncher.launch(uri)
            }
        }
    }
    
    LaunchedEffect(loanId) {
        loanWithItems = loanViewModel.getLoanById(loanId)
    }
    
    val calendar = remember { java.util.Calendar.getInstance() }
    val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val timeSdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    
    var deadlineDate by remember { mutableStateOf(sdf.format(calendar.time)) }
    var deadlineTime by remember { mutableStateOf(timeSdf.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
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
        loanWithItems?.items?.forEach { item ->
            if (!editableItems.containsKey(item.id)) {
                editableItems[item.id] = item.jumlah
            }
        }
    }
    
    fun getMaxAvailableQty(kodeBarang: String, originalQty: Int): Int {
        val inventoryItem = inventoryItems.find { it.noinv == kodeBarang }
        val totalStock = inventoryItem?.quantity ?: 0
        
        return totalStock
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
                            
                            val visibleItems = items.filter { !removedItems.contains(it.id) }
                            val totalItemCount = visibleItems.size + newAddedItems.size
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Barang Dipinjam ($totalItemCount)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StoraBlueDark
                                )
                                
                                TextButton(
                                    onClick = { showAddItemSheet = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Tambah",
                                        tint = StoraBlueDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tambah",
                                        fontSize = 14.sp,
                                        color = StoraBlueDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            visibleItems.forEach { item ->
                                val currentQty = editableItems[item.id] ?: item.jumlah
                                val maxAvailable = getMaxAvailableQty(item.kodeBarang, item.jumlah)
                                val canIncrease = currentQty < maxAvailable
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
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
                                                Text(
                                                    text = "Tersedia: $maxAvailable unit",
                                                    fontSize = 11.sp,
                                                    color = if (maxAvailable > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = { removedItems.add(item.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Jumlah:",
                                                fontSize = 13.sp,
                                                color = textGray,
                                                fontWeight = FontWeight.Medium
                                            )
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (currentQty > 1) StoraBlueDark.copy(alpha = 0.1f) 
                                                            else Color(0xFFE0E0E0)
                                                        )
                                                        .border(1.dp, 
                                                            if (currentQty > 1) StoraBlueDark.copy(alpha = 0.3f) 
                                                            else Color(0xFFBDBDBD), 
                                                            CircleShape)
                                                        .clickable(enabled = currentQty > 1) {
                                                            editableItems[item.id] = currentQty - 1
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Remove,
                                                        contentDescription = "Kurangi",
                                                        tint = if (currentQty > 1) StoraBlueDark else Color(0xFF9E9E9E),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                
                                                Text(
                                                    text = "$currentQty",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StoraBlueDark,
                                                    modifier = Modifier.width(32.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (canIncrease) StoraBlueDark.copy(alpha = 0.1f) 
                                                            else Color(0xFFE0E0E0)
                                                        )
                                                        .border(1.dp, 
                                                            if (canIncrease) StoraBlueDark.copy(alpha = 0.3f) 
                                                            else Color(0xFFBDBDBD), 
                                                            CircleShape)
                                                        .clickable(enabled = canIncrease) {
                                                            editableItems[item.id] = currentQty + 1
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = "Tambah",
                                                        tint = if (canIncrease) StoraBlueDark else Color(0xFF9E9E9E),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            newAddedItems.forEachIndexed { index, item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    val newItemMaxAvailable = getMaxAvailableQty(item.kodeBarang, 0)
                                    val newItemCanIncrease = item.jumlah < newItemMaxAvailable
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "BARU",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier
                                                            .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = item.namaBarang,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = StoraBlueDark
                                                    )
                                                }
                                                Text(
                                                    text = item.kodeBarang,
                                                    fontSize = 12.sp,
                                                    color = textGray
                                                )
                                                Text(
                                                    text = "Tersedia: $newItemMaxAvailable unit",
                                                    fontSize = 11.sp,
                                                    color = if (newItemMaxAvailable > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = { newAddedItems.removeAt(index) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Jumlah:",
                                                fontSize = 13.sp,
                                                color = textGray,
                                                fontWeight = FontWeight.Medium
                                            )
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (item.jumlah > 1) StoraBlueDark.copy(alpha = 0.1f) 
                                                            else Color(0xFFE0E0E0)
                                                        )
                                                        .border(1.dp, 
                                                            if (item.jumlah > 1) StoraBlueDark.copy(alpha = 0.3f) 
                                                            else Color(0xFFBDBDBD), 
                                                            CircleShape)
                                                        .clickable(enabled = item.jumlah > 1) {
                                                            val updatedItem = item.copy(jumlah = item.jumlah - 1)
                                                            newAddedItems[index] = updatedItem
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Remove,
                                                        contentDescription = "Kurangi",
                                                        tint = if (item.jumlah > 1) StoraBlueDark else Color(0xFF9E9E9E),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                
                                                Text(
                                                    text = "${item.jumlah}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StoraBlueDark,
                                                    modifier = Modifier.width(32.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (newItemCanIncrease) StoraBlueDark.copy(alpha = 0.1f) 
                                                            else Color(0xFFE0E0E0)
                                                        )
                                                        .border(1.dp, 
                                                            if (newItemCanIncrease) StoraBlueDark.copy(alpha = 0.3f) 
                                                            else Color(0xFFBDBDBD), 
                                                            CircleShape)
                                                        .clickable(enabled = newItemCanIncrease) {
                                                            val updatedItem = item.copy(jumlah = item.jumlah + 1)
                                                            newAddedItems[index] = updatedItem
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = "Tambah",
                                                        tint = if (newItemCanIncrease) StoraBlueDark else Color(0xFF9E9E9E),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Text(
                                            text = "Foto Barang:",
                                            fontSize = 13.sp,
                                            color = textGray,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF5F5F5))
                                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedNewItemForPhoto = index
                                                    showPhotoPickerDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.imageUri != null) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(Uri.parse(item.imageUri)),
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
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Klik untuk foto",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF9E9E9E),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (visibleItems.isEmpty() && newAddedItems.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                                ) {
                                    Text(
                                        text = "⚠️ Semua barang dihapus. Peminjaman akan dianggap dikembalikan.",
                                        fontSize = 13.sp,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = {
                                    val newDeadline = "$deadlineDate $deadlineTime"
                                    
                                    val modifiedItems = items
                                        .filter { !removedItems.contains(it.id) }
                                        .map { item ->
                                            LoanItemInfo(
                                                inventarisId = item.inventarisId ?: 0,
                                                namaBarang = item.namaBarang,
                                                kodeBarang = item.kodeBarang,
                                                jumlah = editableItems[item.id] ?: item.jumlah,
                                                imageUri = item.imageUri
                                            )
                                        }
                                    
                                    val allItems = modifiedItems + newAddedItems.toList()
                                    
                                    loanViewModel.updateLoan(
                                        loanId = loanId,
                                        newDeadline = newDeadline,
                                        newItems = allItems,
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
        
        if (showAddItemSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showAddItemSheet = false
                    searchQuery = ""
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = StoraWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Tambah Barang",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoraBlueDark
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari barang...", fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF9F9F9),
                            unfocusedContainerColor = Color(0xFFF9F9F9),
                            focusedBorderColor = StoraBlueDark,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val existingIds = loanWithItems?.items?.map { it.inventarisId } ?: emptyList()
                    val newIds = newAddedItems.map { it.inventarisId }
                    val allAddedIds = existingIds + newIds
                    
                    val filteredItems = inventoryItems
                        .filter { (it.serverId ?: 0) !in allAddedIds }
                        .filter { 
                            searchQuery.isEmpty() || 
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.noinv.contains(searchQuery, ignoreCase = true)
                        }
                    
                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Tidak ada barang tersedia" else "Barang tidak ditemukan",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            filteredItems.take(20).forEach { inventoryItem ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            newAddedItems.add(
                                                LoanItemInfo(
                                                    inventarisId = inventoryItem.serverId ?: 0,
                                                    namaBarang = inventoryItem.name,
                                                    kodeBarang = inventoryItem.noinv,
                                                    jumlah = 1,
                                                    imageUri = null
                                                )
                                            )
                                            showAddItemSheet = false
                                            searchQuery = ""
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = inventoryItem.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = StoraBlueDark
                                            )
                                            Text(
                                                text = inventoryItem.noinv,
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Tambah",
                                            tint = StoraBlueDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showPhotoPickerDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPhotoPickerDialog = false
                selectedNewItemForPhoto = null
            },
            title = { 
                Text(
                    "Pilih Foto", 
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                ) 
            },
            text = {
                Column {
                    Text("Pilih sumber foto untuk item baru:", color = Color.Gray)
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            newItemImagePickerLauncher.launch("image/*")
                        }
                    ) {
                        Text("Galeri", color = StoraBlueDark)
                    }
                    
                    TextButton(
                        onClick = {
                            newItemCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text("Kamera", color = StoraBlueDark)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPhotoPickerDialog = false
                        selectedNewItemForPhoto = null
                    }
                ) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}
