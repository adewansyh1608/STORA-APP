package com.example.stora.screens

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.stora.data.LoansData
import com.example.stora.data.NotificationHistoryEntity
import com.example.stora.data.NotificationHistoryApiModel
import com.example.stora.network.ApiConfig
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.utils.TokenManager
import com.example.stora.viewmodel.NotificationViewModel
import com.example.stora.viewmodel.UserProfileViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HomeScreen(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel,
    inventoryViewModel: com.example.stora.viewmodel.InventoryViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = ApiConfig.provideApiService()
    val tokenManager = TokenManager.getInstance(context)

    var isReminderExpanded by rememberSaveable { mutableStateOf(false) }
    val inventoryItems by inventoryViewModel.inventoryItems.collectAsState()

    val notificationHistory by notificationViewModel.notificationHistory.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val isOnline by notificationViewModel.isOnline.collectAsState()

    LaunchedEffect(Unit) {
        userProfileViewModel.loadProfileFromToken()
        notificationViewModel.syncData()
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            notificationViewModel.refreshNetworkStatus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StoraBlueDark)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            StoraTopBar(navController = navController, userProfileViewModel = userProfileViewModel)

            val cardsAlpha by animateFloatAsState(
                targetValue = if (isReminderExpanded) 0f else 1f,
                animationSpec = tween(durationMillis = 400),
                label = "Cards Alpha"
            )

            val cardsOffset by animateDpAsState(
                targetValue = if (isReminderExpanded) (-50).dp else 0.dp,
                animationSpec = tween(durationMillis = 400),
                label = "Cards Offset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, cardsOffset.roundToPx()) }
                    .alpha(cardsAlpha)
            ) {
                if (cardsAlpha > 0.01f) {
                    SummaryCardsRow(totalInventory = inventoryItems.size)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val sheetOffsetY by animateDpAsState(
                targetValue = if (isReminderExpanded) (-80).dp else 0.dp,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                label = "Sheet Offset"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset { IntOffset(0, sheetOffsetY.roundToPx()) },
                color = Color.White,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                shadowElevation = 8.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        ReminderHeader(
                            isExpanded = isReminderExpanded,
                            onClick = { isReminderExpanded = !isReminderExpanded },
                            count = notificationHistory.size
                        )
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = StoraBlueDark)
                            }
                        }
                    } else if (notificationHistory.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (!isOnline) {
                                            Icon(
                                                Icons.Default.CloudOff,
                                                contentDescription = "Offline",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        Text(
                                            text = if (!isOnline) "Mode Offline\nData notifikasi kosong" else "Belum ada notifikasi.\nBuat pengingat untuk memulai.",
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(notificationHistory) { notification ->
                            NotificationHistoryEntityItem(notification = notification)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            StoraBottomNavigationBar(navController = navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoraTopBar(navController: NavHostController, userProfileViewModel: UserProfileViewModel) {
    val userProfile by userProfileViewModel.userProfile.collectAsStateWithLifecycle()
    TopAppBar(
        title = {
            Text(
                text = "STORA",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        },
        actions = {
            IconButton(onClick = {
                navController.navigate(com.example.stora.navigation.Routes.PROFILE_SCREEN)
            }) {
                if (userProfile.profileImageUri != null && userProfile.profileImageUri.toString().isNotEmpty()) {
                    AsyncImage(
                        model = userProfile.profileImageUri,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun SummaryCardsRow(totalInventory: Int) {
    val totalLoaned = remember(LoansData.loansOnLoan.size) {
        LoansData.loansOnLoan.sumOf { it.quantity }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryCard(
            title = "Total Inventaris",
            count = totalInventory.toString(),
            icon = Icons.Outlined.Inventory2,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        SummaryCard(
            title = "Barang Di Pinjam",
            count = totalLoaned.toString(),
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StoraBlueDark.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = StoraBlueDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Text(
                    text = count,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                )
            }
        }
    }
}

@Composable
private fun ReminderHeader(isExpanded: Boolean, onClick: () -> Unit, count: Int = 0) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Chevron Rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Reminder",
            tint = StoraBlueDark,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (count > 0) "Notifikasi ($count)" else "Notifikasi",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = StoraBlueDark
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = StoraBlueDark.copy(alpha = 0.7f),
            modifier = Modifier
                .size(24.dp)
                .rotate(chevronRotation)
        )
    }
}

@Composable
private fun NotificationHistoryItem(notification: NotificationHistoryApiModel) {
    val isLoanNotification = notification.judul?.contains("Deadline") == true || 
                              notification.judul?.contains("Pengembalian") == true ||
                              notification.pesan?.contains("pengembalian") == true ||
                              notification.pesan?.contains("peminjaman") == true
    
    val iconColor = if (isLoanNotification) Color(0xFFE65100) else StoraBlueDark
    val bgColor = if (isLoanNotification) Color(0xFFFFF3E0) else StoraBlueDark.copy(alpha = 0.1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLoanNotification) Icons.AutoMirrored.Outlined.ReceiptLong else Icons.Outlined.Inventory2,
                    contentDescription = "Notification",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.judul ?: "Notifikasi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isLoanNotification) Color(0xFFE65100) else StoraBlueDark
                )
                Text(
                    text = notification.pesan ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                notification.tanggal?.let { dateStr ->
                    Text(
                        text = formatNotificationDate(dateStr),
                        fontSize = 12.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatNotificationDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = inputFormat.parse(dateStr)
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
private fun NotificationHistoryEntityItem(notification: NotificationHistoryEntity) {
    val isLoanNotification = notification.title?.contains("Deadline") == true || 
                              notification.title?.contains("Pengembalian") == true ||
                              notification.message?.contains("pengembalian") == true ||
                              notification.message?.contains("peminjaman") == true
    
    val iconColor = if (isLoanNotification) Color(0xFFE65100) else StoraBlueDark
    val bgColor = if (isLoanNotification) Color(0xFFFFF3E0) else StoraBlueDark.copy(alpha = 0.1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLoanNotification) Icons.AutoMirrored.Outlined.ReceiptLong else Icons.Outlined.Inventory2,
                    contentDescription = "Notification",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title ?: "Notifikasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isLoanNotification) Color(0xFFE65100) else StoraBlueDark
                    )
                    if (notification.needsSync) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Belum tersinkron",
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = notification.message ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = formatNotificationTimestamp(notification.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatNotificationTimestamp(timestamp: Long): String {
    return try {
        val date = Date(timestamp)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        outputFormat.format(date)
    } catch (e: Exception) {
        "Tanggal tidak valid"
    }
}
