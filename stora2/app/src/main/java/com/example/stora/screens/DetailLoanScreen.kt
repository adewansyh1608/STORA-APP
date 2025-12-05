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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.stora.data.LoansData
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.utils.FileUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailLoanScreen(
    navController: NavHostController,
    loanId: Int
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    
    // Map untuk menyimpan return image per item
    val returnImageUris = remember { mutableMapOf<Int, Uri?>() }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedItemForReturn by remember { mutableStateOf<Int?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    
    val loan = remember(loanId) {
        LoansData.loansOnLoan.find { it.id == loanId }
    }
    
    // Get all items with the same groupId
    val loanGroup = remember(loanId) {
        loan?.let { firstLoan ->
            LoansData.loansOnLoan.filter { it.groupId == firstLoan.groupId }
        } ?: emptyList()
    }
    
    val textGray = Color(0xFF585858)
    
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
        // Handle loan not found
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
                            text = loan.borrower ?: "-",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoraBlueDark
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Borrow Date
                        Text(
                            text = loan.borrowDate ?: "-",
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
                                    text = loan.borrowDate ?: "-",
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
                                    text = loan.returnDate ?: "-",
                                    fontSize = 13.sp,
                                    color = StoraBlueDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        // Items Details Section
                        Text(
                            text = "Detail Barang (${loanGroup.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StoraBlueDark
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Display all items in the group
                        loanGroup.forEach { item ->
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
                                        text = item.name,
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
                                                text = item.code,
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
                                                    text = "${item.quantity}",
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
                        
                        // Return Button
                        Button(
                            onClick = {
                                // Return all items in the group with their respective return images
                                loanGroup.forEach { item ->
                                    LoansData.returnLoan(
                                        loanId = item.id,
                                        returnImageUri = returnImageUris[item.id]?.toString()
                                    )
                                }
                                // Navigate back to LoansScreen
                                navController.popBackStack(Routes.LOANS_SCREEN, false)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StoraYellowButton
                            )
                        ) {
                            Text(
                                text = "Return",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StoraBlueDark
                            )
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
