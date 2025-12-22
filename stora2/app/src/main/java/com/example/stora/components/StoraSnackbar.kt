package com.example.stora.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite

enum class StoraSnackbarType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING,
    SYNC
}

data class StoraSnackbarConfig(
    val type: StoraSnackbarType,
    val backgroundColor: Color,
    val icon: ImageVector,
    val iconTint: Color = StoraWhite
)

fun getSnackbarConfig(type: StoraSnackbarType): StoraSnackbarConfig {
    return when (type) {
        StoraSnackbarType.SUCCESS -> StoraSnackbarConfig(
            type = type,
            backgroundColor = Color(0xFF00C853),
            icon = Icons.Filled.CheckCircle
        )
        StoraSnackbarType.ERROR -> StoraSnackbarConfig(
            type = type,
            backgroundColor = Color(0xFFE53935),
            icon = Icons.Filled.Error
        )
        StoraSnackbarType.INFO -> StoraSnackbarConfig(
            type = type,
            backgroundColor = Color(0xFF2196F3),
            icon = Icons.Filled.Info
        )
        StoraSnackbarType.WARNING -> StoraSnackbarConfig(
            type = type,
            backgroundColor = Color(0xFFFF9800),
            icon = Icons.Filled.Warning
        )
        StoraSnackbarType.SYNC -> StoraSnackbarConfig(
            type = type,
            backgroundColor = Color(0xFF1976D2),
            icon = Icons.Filled.Sync
        )
    }
}

@Composable
fun StoraSnackbar(
    snackbarData: SnackbarData,
    type: StoraSnackbarType = StoraSnackbarType.INFO,
    modifier: Modifier = Modifier
) {
    val config = getSnackbarConfig(type)
    
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = config.backgroundColor),
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
                imageVector = config.icon,
                contentDescription = null,
                tint = config.iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = snackbarData.visuals.message,
                color = StoraWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = StoraWhite.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StoraSnackbarHost(
    hostState: SnackbarHostState,
    type: StoraSnackbarType = StoraSnackbarType.INFO,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            StoraSnackbar(
                snackbarData = snackbarData,
                type = type
            )
        }
    )
}

@Composable
fun SyncSnackbar(
    snackbarData: SnackbarData,
    isSuccess: Boolean = true,
    modifier: Modifier = Modifier
) {
    val config = if (isSuccess) {
        getSnackbarConfig(StoraSnackbarType.SUCCESS)
    } else {
        getSnackbarConfig(StoraSnackbarType.ERROR)
    }
    
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = config.backgroundColor),
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
                imageVector = if (isSuccess) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
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
