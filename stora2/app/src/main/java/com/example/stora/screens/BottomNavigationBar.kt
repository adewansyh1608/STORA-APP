package com.example.stora.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellowButton

/**
 * Komponen Bottom Navigation Bar yang reusable untuk aplikasi STORA
 * 
 * @param navController NavHostController untuk menangani navigasi
 * @param modifier Modifier opsional untuk kustomisasi tampilan
 */
@Composable
fun StoraBottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Mendapatkan route saat ini dari back stack
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Menentukan warna background berdasarkan halaman saat ini
    val backgroundColor = if (currentRoute == Routes.INVENTORY_SCREEN) {
        StoraWhite
    } else {
        StoraBlueDark
    }

    // Menentukan warna icon dan text berdasarkan halaman saat ini
    val isOnInventoryScreen = currentRoute == Routes.INVENTORY_SCREEN
    val unselectedIconColor = if (isOnInventoryScreen) Color.Gray else Color.White.copy(alpha = 0.6f)
    val unselectedTextColor = if (isOnInventoryScreen) Color.Gray else Color.White.copy(alpha = 0.6f)
    val selectedIconColor = StoraYellowButton
    val selectedTextColor = StoraYellowButton

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = 16.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            tonalElevation = 0.dp,
            modifier = Modifier.height(96.dp)
        ) {
            // Item Home
            NavigationBarItem(
                selected = currentRoute == Routes.HOME_SCREEN,
                onClick = {
                    navController.navigate(Routes.HOME_SCREEN) {
                        // Pop up to start destination untuk menghindari back stack menumpuk
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Hindari multiple copies dari destination yang sama
                        launchSingleTop = true
                        // Restore state saat reselecting item yang sudah dipilih sebelumnya
                        restoreState = true
                    }
                },
                label = {
                    Text(
                        text = "Home",
                        fontWeight = if (currentRoute == Routes.HOME_SCREEN) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(32.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedIconColor,
                    selectedTextColor = selectedTextColor,
                    unselectedIconColor = unselectedIconColor,
                    unselectedTextColor = unselectedTextColor,
                    indicatorColor = Color.Transparent
                )
            )

            // Item Inventory
            NavigationBarItem(
                selected = currentRoute == Routes.INVENTORY_SCREEN,
                onClick = {
                    navController.navigate(Routes.INVENTORY_SCREEN) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                label = {
                    Text(
                        text = "Inventory",
                        fontWeight = if (currentRoute == Routes.INVENTORY_SCREEN) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = "Inventory",
                        modifier = Modifier.size(32.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedIconColor,
                    selectedTextColor = selectedTextColor,
                    unselectedIconColor = unselectedIconColor,
                    unselectedTextColor = unselectedTextColor,
                    indicatorColor = Color.Transparent
                )
            )

            // Item Loans
            NavigationBarItem(
                selected = currentRoute == Routes.LOANS_SCREEN,
                onClick = {
                    navController.navigate(Routes.LOANS_SCREEN) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                label = {
                    Text(
                        text = "Loans",
                        fontWeight = if (currentRoute == Routes.LOANS_SCREEN) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = "Loans",
                        modifier = Modifier.size(32.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedIconColor,
                    selectedTextColor = selectedTextColor,
                    unselectedIconColor = unselectedIconColor,
                    unselectedTextColor = unselectedTextColor,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
