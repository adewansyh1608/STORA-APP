package com.example.stora.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.viewmodel.UserProfileViewModel
import com.example.stora.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: UserProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Reload profile from TokenManager to get latest data
        viewModel.loadProfileFromToken()
        delay(100)
        isVisible = true
    }

    // Navigate to auth screen after successful logout
    LaunchedEffect(authState.isLoggedIn) {
        if (!authState.isLoggedIn && showLogoutDialog) {
            navController.navigate(Routes.AUTH_SCREEN) {
                popUpTo(0) { inclusive = true }
            }
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
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFFFA726) // Orange color
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))

            // White Card Container with overlapping avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // White Card with Animation
                androidx.compose.animation.AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(animationSpec = tween(800, delayMillis = 200)) { it } + fadeIn(
                        animationSpec = tween(800, delayMillis = 200)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp), // Space for half of avatar
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Space for avatar overlap
                            Spacer(modifier = Modifier.height(70.dp))

                            // Additional space between avatar and name
                            Spacer(modifier = Modifier.height(16.dp))

                            // User Name
                            Text(
                                text = userProfile.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(40.dp))

                            // Menu Items
                            ProfileMenuItem(
                                icon = Icons.Default.Person,
                                text = "Edit Profile",
                                onClick = { navController.navigate(Routes.EDIT_PROFILE_SCREEN) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ProfileMenuItem(
                                icon = Icons.Default.Settings,
                                text = "Setting",
                                onClick = { navController.navigate(Routes.SETTING_SCREEN) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ProfileMenuItem(
                                icon = Icons.AutoMirrored.Filled.Logout,
                                text = "LogOut",
                                onClick = { showLogoutDialog = true }
                            )
                        }
                    }
                }

                // Profile Avatar - positioned to overlap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFA726)), // Orange background
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.profileImageUri != null && userProfile.profileImageUri.toString().isNotEmpty()) {
                            AsyncImage(
                                model = userProfile.profileImageUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.size(80.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Logout Confirmation Dialog with Bottom Sheet Animation
        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    // Call logout from AuthViewModel
                    authViewModel.logout()
                    // Navigation will happen in LaunchedEffect after logout completes
                }
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3)), // Blue background
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Keluar dari Akun?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = "Apakah Anda yakin ingin keluar dari akun Anda?",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StoraBlueDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Confirm Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Keluar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
