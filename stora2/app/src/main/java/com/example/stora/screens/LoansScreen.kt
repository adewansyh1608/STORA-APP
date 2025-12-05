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
import androidx.navigation.NavHostController
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.data.LoansData
import com.example.stora.data.LoanItem
import kotlinx.coroutines.launch

data class GroupedLoanItem(
    val groupId: Int,
    val borrower: String?,
    val borrowDate: String?,
    val returnDate: String?,
    val totalQuantity: Int,
    val items: List<LoanItem>,
    val firstItemId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    navController: NavHostController,
    showDeleteSnackbar: Boolean = false
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val textGray = Color(0xFF585858)
    val dividerYellow = Color(0xFFEFBF6A)
    
    // Check for delete result from previous screen
    val deleteResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("history_deleted")
        ?.value
    
    // Show snackbar when delete result is received
    LaunchedEffect(deleteResult) {
        if (deleteResult == true) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "History berhasil dihapus",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
                // Clear the result after snackbar is shown
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("history_deleted", false)
            }
        }
    }

    // Filter items berdasarkan tab yang dipilih
    val currentItems = if (selectedTab == 0) LoansData.loansOnLoan else LoansData.loansHistory

    val filteredItems = remember(searchQuery, currentItems) {
        if (searchQuery.isBlank()) {
            currentItems
        } else {
            currentItems.filter { item ->
                item.borrower?.contains(searchQuery, ignoreCase = true) == true ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Group items by groupId and create aggregated display items
    val groupedItems = remember(filteredItems) {
        filteredItems
            .groupBy { it.groupId }
            .map { (groupId, items) ->
                GroupedLoanItem(
                    groupId = groupId,
                    borrower = items.firstOrNull()?.borrower,
                    borrowDate = items.firstOrNull()?.borrowDate,
                    returnDate = items.firstOrNull()?.returnDate,
                    totalQuantity = items.sumOf { it.quantity },
                    items = items,
                    firstItemId = items.firstOrNull()?.id ?: -1
                )
            }
    }

    Scaffold(
        containerColor = StoraWhite,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = snackbarData.visuals.message,
                                color = StoraWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.NEW_LOAN_SCREEN) },
                containerColor = StoraYellowButton,
                contentColor = StoraWhite
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Pinjaman")
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
            // Title
            Text(
                text = "Loans",
                color = StoraBlueDark,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            // Tab Selector
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

            // Search Bar
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

            // Content
            if (currentItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "Tidak ada barang yang dipinjam" else "Tidak ada riwayat pinjaman",
                        color = StoraWhite
                    )
                }
            } else if (groupedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada hasil untuk \"$searchQuery\"", color = StoraWhite)
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
                                // Navigate to detail screen with firstItemId to show all items in group
                                if (selectedTab == 1) {
                                    navController.navigate(Routes.detailLoanHistoryScreen(groupedItem.firstItemId))
                                } else {
                                    navController.navigate(Routes.detailLoanScreen(groupedItem.firstItemId))
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = StoraWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left colored bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .background(if (isHistory) Color(0xFF4CAF50) else StoraYellowButton)
            )

            // Content
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Nama Peminjam
                Text(
                    text = groupedItem.borrower ?: "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StoraBlueDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Tanggal Peminjaman
                Text(
                    text = groupedItem.borrowDate ?: "-",
                    color = textGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Total Jumlah Barang
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
