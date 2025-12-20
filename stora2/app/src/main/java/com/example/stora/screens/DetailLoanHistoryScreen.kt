package com.example.stora.screens

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.stora.data.LoansData
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.viewmodel.LoanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailLoanHistoryScreen(
    navController: NavHostController,
    loanId: Int,
    loanViewModel: LoanViewModel = viewModel()
) {
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by loanViewModel.isLoading.collectAsState()
    
    val loan = remember(loanId) {
        LoansData.loansHistory.find { it.id == loanId }
    }
    
    // Get all items with the same groupId
    val loanGroup = remember(loanId) {
        loan?.let { firstLoan ->
            LoansData.loansHistory.filter { it.groupId == firstLoan.groupId }
        } ?: emptyList()
    }
    
    val textGray = Color(0xFF585858)
    
    // Calculate return status based on actual return date vs deadline
    val returnStatus = remember(loan) {
        if (loan?.borrowDate != null && loan.returnDate != null && loan?.actualReturnDate != null) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val returnDateDeadline = sdf.parse(loan.returnDate) // Deadline (tanggal pengembalian yang dijanjikan)
                val actualReturnDate = sdf.parse(loan.actualReturnDate) // Actual return date (tanggal benar-benar dikembalikan)
                
                if (returnDateDeadline != null && actualReturnDate != null) {
                    // Check if returned after deadline
                    if (actualReturnDate.after(returnDateDeadline)) {
                        "Telat" to Color(0xFFE53935) // Red
                    } else {
                        "Tepat Waktu" to Color(0xFF4CAF50) // Green
                    }
                } else {
                    "Tepat Waktu" to Color(0xFF4CAF50)
                }
            } catch (e: Exception) {
                "Tepat Waktu" to Color(0xFF4CAF50)
            }
        } else {
            "Tepat Waktu" to Color(0xFF4CAF50)
        }
    }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    if (loan == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loan history not found")
        }
        return
    }

    Scaffold(
        containerColor = StoraWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Loan History",
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
                        // Borrower Name at the top
                        Text(
                            text = loan.borrower ?: "-",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoraBlueDark
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Phone Number
                        if (!loan.borrowerPhone.isNullOrEmpty()) {
                            Text(
                                text = loan.borrowerPhone,
                                fontSize = 14.sp,
                                color = textGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Borrow and Return Dates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tanggal Pinjam",
                                    fontSize = 11.sp,
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
                                    text = "Tanggal Kembali",
                                    fontSize = 11.sp,
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
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Return Status Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = returnStatus.second.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = returnStatus.second,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: ",
                                    fontSize = 12.sp,
                                    color = textGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = returnStatus.first,
                                    fontSize = 14.sp,
                                    color = returnStatus.second,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
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
                                            Text(
                                                text = "${item.quantity}",
                                                fontSize = 12.sp,
                                                color = StoraBlueDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Loan Photo
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
                                                .height(120.dp)
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
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    
                                    // Return Photo
                                    if (item.returnImageUri != null) {
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
                                                .background(Color(0xFFE8E8E8))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFFD0D0D0),
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(Uri.parse(item.returnImageUri)),
                                                contentDescription = "Return Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
