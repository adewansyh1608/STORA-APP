package com.example.stora.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.stora.data.LoansData
import com.example.stora.data.LoanItem
import com.example.stora.viewmodel.LoanViewModel
import kotlinx.coroutines.launch

data class GroupedLoanItem(
    val groupId: Int,
    val borrower: String?,
    val borrowDate: String?,
    val returnDate: String?,
    val actualReturnDate: String?,
    val status: String?,
    val totalQuantity: Int,
    val items: List<LoanItem>,
    val firstItemId: Int,
    val roomLoanId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    navController: NavHostController,
    showDeleteSnackbar: Boolean = false,
    loanViewModel: LoanViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isSyncing by loanViewModel.isSyncing.collectAsState()
    val syncStatus by loanViewModel.syncStatus.collectAsState()
    val unsyncedCount by loanViewModel.unsyncedCount.collectAsState()
    val error by loanViewModel.error.collectAsState()
    val isServerAvailable by loanViewModel.isServerAvailable.collectAsState()
    
    val isActuallyOnline = loanViewModel.isOnline() && isServerAvailable

    val textGray = Color(0xFF585858)
    val dividerYellow = Color(0xFFEFBF6A)
    
    val deleteResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("history_deleted")
        ?.value
    
    LaunchedEffect(deleteResult) {
        if (deleteResult == true) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "History berhasil dihapus",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("history_deleted", false)
            }
        }
    }

    LaunchedEffect(syncStatus) {
        syncStatus?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            loanViewModel.clearError()
        }
    }

    val currentItems = if (selectedTab == 0) LoansData.loansOnLoan else LoansData.loansHistory

    val filteredItems = remember(searchQuery, currentItems, currentItems.size) {
        if (searchQuery.isBlank()) {
            currentItems.toList()
        } else {
            currentItems.filter { item ->
                item.borrower?.contains(searchQuery, ignoreCase = true) == true ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val groupedItems = remember(filteredItems) {
        filteredItems
            .groupBy { it.groupId }
            .map { (groupId, items) ->
                GroupedLoanItem(
                    groupId = groupId,
                    borrower = items.firstOrNull()?.borrower,
                    borrowDate = items.firstOrNull()?.borrowDate,
                    returnDate = items.firstOrNull()?.returnDate,
                    actualReturnDate = items.firstOrNull()?.actualReturnDate,
                    status = items.firstOrNull()?.status,
                    totalQuantity = items.sumOf { it.quantity },
                    items = items,
                    firstItemId = items.firstOrNull()?.id ?: -1,
                    roomLoanId = items.firstOrNull()?.roomLoanId
                )
            }
    }

    Scaffold(
        containerColor = StoraWhite,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    val isSuccess = snackbarData.visuals.message.contains("berhasil", ignoreCase = true) ||
                            snackbarData.visuals.message.contains("success", ignoreCase = true)
                    val isError = snackbarData.visuals.message.contains("gagal", ignoreCase = true) ||
                            snackbarData.visuals.message.contains("error", ignoreCase = true) ||
                            snackbarData.visuals.message.contains("failed", ignoreCase = true)

                    val backgroundColor = when {
                        isSuccess -> Color(0xFF00C853)
                        isError -> Color(0xFFE53935)
                        else -> Color(0xFF1976D2)
                    }

                    val icon = when {
                        isSuccess -> Icons.Filled.CheckCircle
                        isError -> Icons.Filled.Error
                        else -> Icons.Filled.CloudDone
                    }

                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = StoraWhite,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = snackbarData.visuals.message,
                                color = StoraWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (unsyncedCount > 0) {
                            Badge(
                                containerColor = Color.Red,
                                contentColor = StoraWhite
                            ) {
                                Text(
                                    text = unsyncedCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = { 
                            if (isActuallyOnline || unsyncedCount > 0) {
                                loanViewModel.syncData() 
                            }
                        },
                        containerColor = when {
                            isActuallyOnline && unsyncedCount == 0 -> Color(0xFF4CAF50)
                            isActuallyOnline && unsyncedCount > 0 -> StoraYellow
                            else -> Color.Gray
                        },
                        contentColor = StoraWhite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = StoraWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    !isActuallyOnline -> Icons.Filled.CloudOff
                                    unsyncedCount > 0 -> Icons.Filled.Sync
                                    else -> Icons.Filled.CloudDone
                                },
                                contentDescription = when {
                                    !isActuallyOnline -> "Offline"
                                    unsyncedCount > 0 -> "Sync"
                                    else -> "Synced"
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { navController.navigate(Routes.NEW_LOAN_SCREEN) },
                    containerColor = StoraYellowButton,
                    contentColor = StoraWhite
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah Pinjaman")
                }
            }
        },
        bottomBar = {
            StoraBottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StoraWhite)
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Loans",
                    color = StoraBlueDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isActuallyOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = if (isActuallyOnline) "Online" else "Offline",
                        tint = if (isActuallyOnline) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isActuallyOnline) "Online" else "Offline",
                        color = if (isActuallyOnline) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabButton(
                    text = "Items on loan",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Loan History",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StoraWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Cari Barang Yang Sedang di Pinjam", color = textGray, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Cari", tint = textGray)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StoraWhite,
                        unfocusedContainerColor = StoraWhite,
                        disabledContainerColor = StoraWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = dividerYellow
            )

            if (currentItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Tidak ada barang yang dipinjam" else "Tidak ada riwayat pinjaman",
                            color = textGray
                        )
                        if (!isActuallyOnline) {
                            Text(
                                text = "Offline - Data lokal",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else if (groupedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada hasil untuk \"$searchQuery\"", color = textGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(groupedItems, key = { it.groupId }) { groupedItem ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { it / 2 })
                        ) {
                            LoanGroupCard(groupedItem = groupedItem, isHistory = selectedTab == 1) {
                                groupedItem.roomLoanId?.let { roomId ->
                                    if (selectedTab == 1) {
                                        navController.navigate(Routes.detailLoanHistoryScreen(roomId))
                                    } else {
                                        navController.navigate(Routes.detailLoanScreen(roomId))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) StoraYellowButton else StoraBlueDark.copy(alpha = 0.7f),
            contentColor = if (isSelected) StoraBlueDark else StoraWhite
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
fun LoanGroupCard(
    groupedItem: GroupedLoanItem,
    isHistory: Boolean,
    onClick: () -> Unit
) {
    val textGray = Color(0xFF585858)
    
    val statusColor = if (isHistory) {
        when (groupedItem.status) {
            "Selesai" -> Color(0xFF4CAF50)
            "Terlambat" -> Color(0xFFE53935)
            else -> Color(0xFF4CAF50)
        }
    } else {
        StoraYellowButton
    }
    
    val statusText = if (isHistory) {
        when (groupedItem.status) {
            "Selesai" -> "Tepat Waktu"
            "Terlambat" -> "Terlambat"
            else -> "Selesai"
        }
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = StoraWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = groupedItem.borrower ?: "-",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StoraBlueDark,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isHistory && statusText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = groupedItem.borrowDate ?: "-",
                    color = textGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Widgets,
                        contentDescription = "Jumlah",
                        tint = textGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${groupedItem.totalQuantity} barang",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
