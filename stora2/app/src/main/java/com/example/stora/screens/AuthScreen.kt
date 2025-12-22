package com.example.stora.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.stora.R
import com.example.stora.navigation.Routes
import com.example.stora.ui.theme.StoraBlueButton
import com.example.stora.ui.theme.StoraBlueDark
import com.example.stora.ui.theme.StoraWhite
import com.example.stora.ui.theme.StoraYellowButton
import com.example.stora.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

enum class AuthScreenState {
    WELCOME,
    LANDING,
    LOGIN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    navController: NavHostController,
    authViewModel: com.example.stora.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    var authScreenState by rememberSaveable { mutableStateOf(AuthScreenState.WELCOME) }

    val animationDuration = 1000

    LaunchedEffect(authScreenState) {
        if (authScreenState == AuthScreenState.WELCOME) {
            delay(3000)
            authScreenState = AuthScreenState.LANDING
        }
    }

    val blueBgWeight by animateFloatAsState(
        targetValue = when (authScreenState) {
            AuthScreenState.WELCOME -> 1f
            AuthScreenState.LANDING -> 0.65f
            else -> 0.3f
        },
        animationSpec = tween(durationMillis = animationDuration),
        label = "Blue BG Weight"
    )

    val whiteBgWeight by animateFloatAsState(
        targetValue = when (authScreenState) {
            AuthScreenState.WELCOME -> 0f
            AuthScreenState.LANDING -> 0.35f
            else -> 0.7f
        },
        animationSpec = tween(durationMillis = animationDuration),
        label = "White BG Weight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        val blueCorner by animateDpAsState(
            targetValue = if (authScreenState != AuthScreenState.WELCOME) 47.dp else 0.dp,
            animationSpec = tween(durationMillis = animationDuration),
            label = "Blue BG Corner"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(blueBgWeight)
                .clip(
                    RoundedCornerShape(
                        bottomStart = blueCorner,
                        bottomEnd   = blueCorner
                    )
                )
                .background(
                    color = StoraBlueDark,
                    shape = RoundedCornerShape(
                        bottomStart = blueCorner,
                        bottomEnd   = blueCorner
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = authScreenState,
                label = "Header Content Animation",
                transitionSpec = {
                    fadeIn(animationSpec = tween(animationDuration)) togetherWith
                            fadeOut(animationSpec = tween(animationDuration))
                }
            ) { state ->
                when (state) {
                    AuthScreenState.WELCOME, AuthScreenState.LANDING -> {
                        WelcomeLandingHeader()
                    }
                    else -> {
                        LoginSignupHeader()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.White)
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            this@Column.AnimatedVisibility(
                visible = authScreenState == AuthScreenState.LANDING,
                modifier = Modifier.fillMaxSize(),
                enter = slideInVertically(animationSpec = tween(animationDuration)) { it } + fadeIn(),
                exit = slideOutVertically(animationSpec = tween(animationDuration)) { it } + fadeOut()
            ) {
                LandingButtons(
                    onLoginClicked = { authScreenState = AuthScreenState.LOGIN },
                    onSignUpClicked = { authScreenState = AuthScreenState.SIGN_UP }
                )
            }

            this@Column.AnimatedVisibility(
                visible = authScreenState == AuthScreenState.LOGIN,
                modifier = Modifier.fillMaxSize(),
                enter = slideInVertically(animationSpec = tween(animationDuration)) { it } + fadeIn(),
                exit = slideOutVertically(animationSpec = tween(animationDuration)) { it } + fadeOut()
            ) {
                LoginForm(
                    onSignUpClicked = { authScreenState = AuthScreenState.SIGN_UP },
                    onForgotPasswordClicked = { authScreenState = AuthScreenState.FORGOT_PASSWORD },
                    navController = navController
                )
            }

            this@Column.AnimatedVisibility(
                visible = authScreenState == AuthScreenState.FORGOT_PASSWORD,
                modifier = Modifier.fillMaxSize(),
                enter = slideInVertically(animationSpec = tween(animationDuration)) { it } + fadeIn(),
                exit = slideOutVertically(animationSpec = tween(animationDuration)) { it } + fadeOut()
            ) {
                ForgotPasswordForm(
                    onBackToLoginClicked = { authScreenState = AuthScreenState.LOGIN }
                )
            }

            this@Column.AnimatedVisibility(
                visible = authScreenState == AuthScreenState.SIGN_UP,
                modifier = Modifier.fillMaxSize(),
                enter = slideInVertically(animationSpec = tween(animationDuration)) { it } + fadeIn(),
                exit = slideOutVertically(animationSpec = tween(animationDuration)) { it } + fadeOut()
            ) {
                SignUpForm(
                    onLoginClicked = { authScreenState = AuthScreenState.LOGIN }
                )
            }
        }
    }
}

@Composable
fun WelcomeLandingHeader() {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.stora_logo),
            contentDescription = "Stora Logo",
            modifier = Modifier.size(130.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "STORA",
            color = StoraWhite,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Student Organization Asset Manager",
            color = StoraWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun LoginSignupHeader() {

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.stora_logo),
            contentDescription = "Stora Logo",
            modifier = Modifier.size(60.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "STORA",
            color = StoraWhite,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun LandingButtons(onLoginClicked: () -> Unit, onSignUpClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Button(
            onClick = onLoginClicked,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = StoraBlueButton,
                contentColor = StoraWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSignUpClicked,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = StoraYellowButton.copy(alpha = 0.43f),
                contentColor = StoraBlueButton
            ),
            border = BorderStroke(2.dp, StoraYellowButton),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "SIGN UP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoginForm(
    onSignUpClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit = {},
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSuccess, uiState.isLoggedIn) {
        if (uiState.isSuccess && uiState.isLoggedIn) {
            navController.navigate(Routes.HOME_SCREEN) {
                popUpTo(Routes.AUTH_SCREEN) { inclusive = true }
            }
            authViewModel.clearState()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        StoraTextField(
            label = "Email",
            keyboardType = KeyboardType.Email,
            value = email,
            onValueChange = { email = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        StoraTextField(
            label = "Password",
            isPassword = true,
            value = password,
            onValueChange = { password = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Forgot Password?",
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onForgotPasswordClicked() },
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.height(24.dp))

        uiState.errorMessage?.let { error ->
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    authViewModel.login(email.trim(), password)
                }
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = StoraBlueButton,
                contentColor = StoraWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = StoraWhite
                )
            } else {
                Text(text = "LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sign Up",
            color = StoraYellowButton,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.clickable { onSignUpClicked() }
        )
    }
}

@Composable
fun SignUpForm(
    onLoginClicked: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSuccess, uiState.isLoggedIn) {
        if (uiState.isSuccess && uiState.isLoggedIn) {
            snackbarHostState.showSnackbar(
                message = "Pendaftaran berhasil! Silakan login dengan akun Anda.",
                duration = androidx.compose.material3.SnackbarDuration.Long
            )

            name = ""
            email = ""
            password = ""
            confirmPassword = ""

            authViewModel.clearState()

            delay(1500)
            onLoginClicked()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            StoraTextField(
                label = "User Name",
                value = name,
                onValueChange = { name = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StoraTextField(
                label = "Email",
                keyboardType = KeyboardType.Email,
                value = email,
                onValueChange = { email = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StoraTextField(
                label = "Password",
                isPassword = true,
                value = password,
                onValueChange = { password = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StoraTextField(
                label = "Confirm Password",
                isPassword = true,
                value = confirmPassword,
                onValueChange = { confirmPassword = it }
            )

            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text(
                    text = "Password tidak sama",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            uiState.errorMessage?.let { error ->
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank() &&
                        password.isNotBlank() && password == confirmPassword) {
                        authViewModel.signup(
                            name.trim(),
                            email.trim(),
                            password,
                            confirmPassword
                        )
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StoraBlueButton,
                    contentColor = StoraWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading &&
                        name.isNotBlank() &&
                        email.isNotBlank() &&
                        password.isNotBlank() &&
                        password == confirmPassword
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = StoraWhite
                    )
                } else {
                    Text(text = "SIGN UP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Login",
                color = StoraYellowButton,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onLoginClicked() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { snackbarData ->
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00C853)),
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
                            imageVector = Icons.Filled.CheckCircle,
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
    }
}

@Composable
fun ForgotPasswordForm(
    onBackToLoginClicked: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && !uiState.isLoggedIn) {
            showSuccessDialog = true
            authViewModel.clearState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Reset Password",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StoraBlueDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Masukkan email dan password baru Anda",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            StoraTextField(
                label = "Email",
                keyboardType = KeyboardType.Email,
                value = email,
                onValueChange = { email = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StoraTextField(
                label = "Password Baru",
                isPassword = true,
                value = newPassword,
                onValueChange = { newPassword = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StoraTextField(
                label = "Konfirmasi Password",
                isPassword = true,
                value = confirmPassword,
                onValueChange = { confirmPassword = it }
            )

            if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                Text(
                    text = "Password tidak sama",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            uiState.errorMessage?.let { error ->
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (email.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmPassword) {
                        authViewModel.resetPassword(email.trim(), newPassword, confirmPassword)
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StoraBlueButton,
                    contentColor = StoraWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading &&
                        email.isNotBlank() &&
                        newPassword.isNotBlank() &&
                        newPassword == confirmPassword &&
                        newPassword.length >= 6
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = StoraWhite
                    )
                } else {
                    Text(text = "RESET PASSWORD", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Kembali ke Login",
                color = StoraYellowButton,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onBackToLoginClicked() }
            )
        }

        if (showSuccessDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    onBackToLoginClicked()
                },
                title = { Text("Berhasil") },
                text = { Text("Password berhasil direset. Silakan login dengan password baru Anda.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onBackToLoginClicked()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StoraBlueButton)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun StoraTextField(
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    value: String = "",
    onValueChange: ((String) -> Unit)? = null
) {
    var internalText by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(!isPassword) }

    val textValue = if (onValueChange != null) value else internalText
    val textOnValueChange: (String) -> Unit = onValueChange ?: { newValue ->
        internalText = newValue
    }

    OutlinedTextField(
        value = textValue,
        onValueChange = textOnValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StoraBlueButton,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
            cursorColor = StoraBlueButton
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            if (isPassword) {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        }
    )
}
