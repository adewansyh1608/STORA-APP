package com.example.stora.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
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
import com.example.stora.data.InventoryItem
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavHostController,
    viewModel: InventoryViewModel = viewModel()
) {
    val items by viewModel.inventoryItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val error by viewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val textGray = Color(0xFF585858)
    val dividerYellow = Color(0xFFEFBF6A)

    // Show snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Show snackbar for sync status
    LaunchedEffect(syncStatus) {
        syncStatus?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Filter items based on search
    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.noinv.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = StoraBlueDark,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sync button with badge
                if (unsyncedCount > 0) {
                    BadgedBox(
                        badge = {
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
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.syncData() },
                            containerColor = if (viewModel.isOnline()) StoraYellow else Color.Gray,
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
                                    imageVector = if (viewModel.isOnline()) Icons.Filled.Sync else Icons.Filled.CloudOff,
                                    contentDescription = "Sync",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else if (viewModel.isOnline()) {
                    FloatingActionButton(
                        onClick = { viewModel.syncData() },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = StoraWhite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = "Synced",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Add item button
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.ADD_ITEM_SCREEN) },
                    containerColor = StoraYellow,
                    contentColor = StoraWhite
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah Item")
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
                .background(StoraBlueDark)
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title with sync status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventaris",
                    color = StoraYellow,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                // Online/Offline indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.isOnline()) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = if (viewModel.isOnline()) "Online" else "Offline",
                        tint = if (viewModel.isOnline()) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (viewModel.isOnline()) "Online" else "Offline",
                        color = if (viewModel.isOnline()) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Cari inventaris disini", color = textGray)
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

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = dividerYellow
            )

            // Loading indicator
            if (isLoading && items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StoraYellow)
                }
            } else if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Penyimpanan kosong.",
                            color = StoraWhite,
                            fontSize = 16.sp
                        )
                        if (!viewModel.isOnline()) {
                            Text(
                                text = "Offline - Tidak ada data lokal",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada hasil untuk \"$searchQuery\"",
                        color = StoraWhite
                    )
                }
            } else {
                // Items list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { it / 2 })
                        ) {
                            InventoryItemCard(item = item) {
                                navController.navigate(Routes.detailScreen(item.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(item: InventoryItem, onClick: () -> Unit) {
    val textGray = Color(0xFF585858)
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = StoraWhite)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                // Left colored bar with sync indicator
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(16.dp)
                        .background(
                            if (!item.isSynced || item.needsSync) {
                                Color(0xFFFF9800) // Orange for unsynced
                            } else {
                                StoraYellow
                            }
                        )
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
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = StoraBlueDark,
                            modifier = Modifier.weight(1f)
                        )

                        // Sync status icon
                        if (!item.isSynced || item.needsSync) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = "Not synced",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.noinv,
                        color = textGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Widgets,
                                contentDescription = "Jumlah",
                                tint = textGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "= ${item.quantity}",
                                color = textGray,
                                fontSize = 14.sp
                            )
                        }

                        // Category badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StoraYellow.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = item.category,
                                color = StoraBlueDark,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
