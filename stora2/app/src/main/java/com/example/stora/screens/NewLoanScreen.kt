package com.example.stora.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.stora.data.InventoryItem
import com.example.stora.viewmodel.InventoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stora.data.LoansData
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLoanScreen(
    navController: NavHostController,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val items by inventoryViewModel.inventoryItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<String>() }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val textGray = Color(0xFF585858)

    // Filter items dengan available quantity > 0
    val availableItems = remember(items.size, LoansData.loansOnLoan.size) {
        items.filter { item ->
            LoansData.getAvailableQuantity(item) > 0
        }
    }

    val filteredItems = remember(searchQuery, availableItems.size) {
        if (searchQuery.isBlank()) {
            availableItems
        } else {
            availableItems.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.noinv.contains(searchQuery, ignoreCase = true)
            }
        }
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
            // White content area with rounded top corners only
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
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp)
                ) {
                    // Search Bar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Cari barang yang akan di Pinjam", color = textGray, fontSize = 14.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = "Cari", tint = textGray)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF5F5F5),
                                unfocusedContainerColor = Color(0xFFF5F5F5),
                                disabledContainerColor = Color(0xFFF5F5F5),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider untuk mempertegas batas
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = Color(0xFFE0E0E0)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content
                    if (availableItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Semua inventaris sedang dipinjam.", color = textGray)
                        }
                    } else if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada hasil untuk \"$searchQuery\"", color = textGray)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val bottomPadding = if (selectedItems.isNotEmpty()) 80.dp else 8.dp
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    var itemVisible by remember { mutableStateOf(false) }

                                    LaunchedEffect(Unit) {
                                        itemVisible = true
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = itemVisible,
                                        enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500))
                                    ) {
                                        NewLoanItemCard(
                                            item = item,
                                            isSelected = selectedItems.contains(item.id),
                                            onClick = {
                                                if (selectedItems.contains(item.id)) {
                                                    selectedItems.remove(item.id)
                                                } else {
                                                    selectedItems.add(item.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Button Pinjam
                            androidx.compose.animation.AnimatedVisibility(
                                visible = selectedItems.isNotEmpty(),
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(500)
                                ) + fadeIn(animationSpec = tween(500)),
                                exit = androidx.compose.animation.slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(500)
                                ) + androidx.compose.animation.fadeOut(animationSpec = tween(500)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val selectedItemsString = selectedItems.joinToString(",")
                                        navController.navigate(Routes.loanFormScreen(selectedItemsString))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StoraBlueDark
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 8.dp
                                    )
                                ) {
                                    Text(
                                        text = "Pinjam (${selectedItems.size} item)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
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
}

@Composable
fun NewLoanItemCard(
    item: InventoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textGray = Color(0xFF585858)
    val availableQty = LoansData.getAvailableQuantity(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StoraBlueDark.copy(alpha = 0.1f) else StoraWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, StoraBlueDark) else null
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left colored bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .background(if (isSelected) StoraBlueDark else StoraYellow)
            )

            // Content
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = StoraBlueDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.noinv,
                    color = textGray,
                    fontSize = 14.sp
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
                        text = "= $availableQty",
                        color = textGray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
