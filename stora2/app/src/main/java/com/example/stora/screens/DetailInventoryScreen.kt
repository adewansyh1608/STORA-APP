package com.example.stora.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.stora.data.InventoryItem
import com.example.stora.data.LoansData
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.viewmodel.InventoryViewModel
import kotlinx.coroutines.delay

@Composable
fun DetailInventoryScreen(
    navController: NavHostController,
    itemId: String?,
    viewModel: InventoryViewModel = viewModel()
) {
    var item by remember { mutableStateOf<InventoryItem?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        itemId?.let {
            item = viewModel.getInventoryItemById(it)
        }
    }

    val currentItem = item

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val blueBgWeight by animateFloatAsState(
        targetValue = if (isVisible) 0.15f else 1f,
        animationSpec = tween(durationMillis = 800),
        label = "Blue BG Weight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StoraBlueDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(blueBgWeight)
                .background(StoraBlueDark),
            contentAlignment = Alignment.Center
        ) {
            DetailTopBar(
                title = "Detail",
                onBackClick = {
                    navController.popBackStack()
                },
                onEditClick = if (currentItem != null) {
                    { navController.navigate("edit_item/${currentItem.id}") }
                } else null,
                onDeleteClick = if (currentItem != null) {
                    { showDeleteDialog = true }
                } else null
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(StoraWhite)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(animationSpec = tween(800, delayMillis = 200)) { it } + fadeIn(animationSpec = tween(800, delayMillis = 200)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentItem != null) {
                    DetailContent(item = currentItem)
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Item tidak ditemukan!", color = Color.Black)
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && currentItem != null) {
            val textGray = Color(0xFF585858)

            Dialog(onDismissRequest = { showDeleteDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StoraWhite
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Title
                        Text(
                            text = "Hapus Item?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoraBlueDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        Text(
                            text = "Item ${currentItem.name} akan dihapus secara permanen dan tidak dapat dikembalikan.",
                            fontSize = 14.sp,
                            color = textGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = { showDeleteDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = textGray
                                )
                            ) {
                                Text(
                                    text = "Batal",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Delete Button
                            Button(
                                onClick = {
                                    viewModel.deleteInventoryItem(
                                        id = currentItem.id,
                                        onSuccess = {
                                            showDeleteDialog = false
                                            navController.popBackStack()
                                        },
                                        onError = { error ->
                                            showDeleteDialog = false
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935)
                                )
                            ) {
                                Text(
                                    text = "Hapus",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StoraWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailTopBar(
    title: String,
    onBackClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = StoraYellow
            )
        }
        Text(
            text = title,
            color = StoraYellow,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )

        // Action Icons
        if (onDeleteClick != null) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = Color.Red
                )
            }
        }

        if (onEditClick != null) {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = StoraYellow
                )
            }
        }
    }
}

@Composable
fun DetailContent(item: InventoryItem) {
    val textGray = Color(0xFF585858)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.noinv,
                fontSize = 16.sp,
                color = textGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Photo Item
            item.photoUri?.let { photoUriString ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photoUriString,
                        contentDescription = "Foto ${item.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Quantity Breakdown Section
                Text(
                    text = "Stok Barang",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StoraBlueDark,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Calculate quantities
                val totalQuantity = item.quantity
                val borrowedQuantity = LoansData.getBorrowedQuantity(item)
                val availableQuantity = totalQuantity - borrowedQuantity
                
                // Quantity Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Quantity Card
                    QuantityCard(
                        modifier = Modifier.weight(1f),
                        label = "Total",
                        value = totalQuantity,
                        backgroundColor = Color(0xFFF5F5F5),
                        valueColor = StoraBlueDark
                    )
                    
                    // Borrowed Quantity Card
                    QuantityCard(
                        modifier = Modifier.weight(1f),
                        label = "Dipinjam",
                        value = borrowedQuantity,
                        backgroundColor = Color(0xFFFFF3E0),
                        valueColor = Color(0xFFE65100)
                    )
                    
                    // Available Quantity Card
                    QuantityCard(
                        modifier = Modifier.weight(1f),
                        label = "Tersedia",
                        value = availableQuantity,
                        backgroundColor = Color(0xFFE8F5E9),
                        valueColor = Color(0xFF2E7D32)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailInfoRow(label = "Kategori", value = item.category)
                DetailInfoRow(label = "Kondisi", value = item.condition)
                DetailInfoRow(label = "Lokasi", value = item.location)
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = StoraYellow
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Deskripsi :",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = textGray,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StoraBlueDark.copy(alpha = 0.13f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dibuat pada ${item.date}",
                color = Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(100.dp),
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Normal
        )

        Text(
            text = ": ",
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Normal
        )

        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun QuantityCard(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    backgroundColor: Color,
    valueColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}
