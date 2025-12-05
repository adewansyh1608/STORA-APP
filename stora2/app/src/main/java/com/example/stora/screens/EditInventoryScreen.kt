package com.example.stora.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.stora.data.InventoryItem
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellow
import com.example.stora.viewmodel.InventoryViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInventoryScreen(
    navController: NavHostController,
    itemId: String?,
    viewModel: InventoryViewModel = viewModel()
) {
    var item by remember { mutableStateOf<InventoryItem?>(null) }
    var isVisible by remember { mutableStateOf(false) }

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
            StoraTopBar(
                title = "Edit Item",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(StoraWhite)
                .padding(top = 24.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(animationSpec = tween(800, delayMillis = 200)) { it } + fadeIn(animationSpec = tween(800, delayMillis = 200)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentItem != null) {
                    EditItemForm(navController = navController, item = currentItem, viewModel = viewModel)
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Item tidak ditemukan!", color = Color.Black)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemForm(
    navController: NavHostController,
    item: InventoryItem,
    viewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(item.name) }
    var noinv by remember { mutableStateOf(item.noinv) }
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    var category by remember { mutableStateOf(item.category) }
    var condition by remember { mutableStateOf(item.condition) }
    var location by remember { mutableStateOf(item.location) }
    var date by remember { mutableStateOf(item.date) }
    var description by remember { mutableStateOf(item.description) }
    var photoUri by remember { mutableStateOf<Uri?>(item.photoUri?.let { Uri.parse(it) }) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showConditionDropdown by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val conditionOptions = listOf("Baik", "Rusak Ringan", "Rusak Berat")

    val calendar = Calendar.getInstance()
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            photoUri = it
            showPhotoOptions = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let {
                photoUri = it
                showPhotoOptions = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(
                context.cacheDir,
                "photo_${System.currentTimeMillis()}.jpg"
            )
            cameraImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(cameraImageUri!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StoraFormField(
            value = name,
            onValueChange = { name = it; isError = false },
            label = "Nama Inventaris"
        )
        StoraFormField(
            value = noinv,
            onValueChange = { noinv = it; isError = false },
            label = "Nomor Inventaris"
        )
        QuantityInputField(
            value = quantity,
            onValueChange = { quantity = it; isError = false },
            label = "Jumlah"
        )
        StoraFormField(
            value = category,
            onValueChange = { category = it; isError = false },
            label = "Kategori"
        )
        
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Kondisi",
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
            ExposedDropdownMenuBox(
                expanded = showConditionDropdown,
                onExpandedChange = { showConditionDropdown = !showConditionDropdown }
            ) {
                OutlinedTextField(
                    value = condition,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showConditionDropdown)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE9E4DE),
                        unfocusedContainerColor = Color(0xFFE9E4DE),
                        disabledContainerColor = Color(0xFFE9E4DE),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showConditionDropdown,
                    onDismissRequest = { showConditionDropdown = false }
                ) {
                    conditionOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                condition = option
                                showConditionDropdown = false
                                isError = false
                            }
                        )
                    }
                }
            }
        }
        
        StoraFormField(
            value = location,
            onValueChange = { location = it; isError = false },
            label = "Lokasi"
        )

        StoraDatePickerField(
            value = date,
            label = "Tanggal pencatatan",
            onClick = { showDatePicker = true }
        )

        PhotoInputSection(
            photoUri = photoUri,
            onPhotoOptionsClick = { showPhotoOptions = true },
            onRemovePhoto = { photoUri = null }
        )

        StoraFormField(
            value = description,
            onValueChange = { description = it; isError = false },
            label = "Deskripsi",
            singleLine = false,
            modifier = Modifier.height(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isError) {
            Text(
                text = "Semua kolom harus diisi!",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = calendar.timeInMillis
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Date(millis)
                            date = dateFormatter.format(selectedDate)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Batal")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showPhotoOptions) {
            PhotoPickerBottomSheet(
                onDismiss = { showPhotoOptions = false },
                onGalleryClick = {
                    showPhotoOptions = false
                    galleryLauncher.launch("image/*")
                },
                onCameraClick = {
                    showPhotoOptions = false
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) -> {
                            val photoFile = File(
                                context.cacheDir,
                                "photo_${System.currentTimeMillis()}.jpg"
                            )
                            cameraImageUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            cameraLauncher.launch(cameraImageUri!!)
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            )
        }

        Button(
            onClick = {
                val qtyInt = quantity.toIntOrNull()
                if (name.isNotBlank() && noinv.isNotBlank() && qtyInt != null && category.isNotBlank() && condition.isNotBlank() && location.isNotBlank() && description.isNotBlank() && date.isNotBlank()) {
                    val updatedItem = InventoryItem(
                        id = item.id,
                        name = name,
                        noinv = noinv,
                        quantity = qtyInt,
                        category = category,
                        condition = condition,
                        location = location,
                        description = description,
                        date = date,
                        photoUri = photoUri?.toString(),
                        serverId = item.serverId,
                        isSynced = false,
                        isDeleted = false,
                        lastModified = System.currentTimeMillis(),
                        needsSync = true
                    )
                    viewModel.updateInventoryItem(
                        item = updatedItem,
                        onSuccess = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle?.set("itemUpdated", true)
                            navController.popBackStack()
                        },
                        onError = { error ->
                            isError = true
                        }
                    )
                    isError = false
                } else {
                    isError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StoraYellow,
                contentColor = StoraBlueDark
            )
        ) {
            Text("Update", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun QuantityInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    val fieldColor = Color(0xFFE9E4DE)
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(fieldColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val currentValue = value.toIntOrNull() ?: 0
                    if (currentValue > 0) {
                        onValueChange((currentValue - 1).toString())
                    }
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "−",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                )
            }

            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "0",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(
                onClick = {
                    val currentValue = value.toIntOrNull() ?: 0
                    onValueChange((currentValue + 1).toString())
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StoraBlueDark
                )
            }
        }
    }
}
