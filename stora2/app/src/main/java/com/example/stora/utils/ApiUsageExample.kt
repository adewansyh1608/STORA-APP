package com.example.stora.utils

/**
 * PANDUAN LENGKAP IMPLEMENTASI AUTHENTICATION
 * 
 * ========================================
 * 1. SETUP BACKEND
 * ========================================
 * - Pastikan backend Node.js/Express sudah berjalan di port 3000
 * - Endpoint yang tersedia:
 *   POST /api/login
 *   POST /api/signup  
 *   POST /api/logout
 *   GET /api/profile
 *   PUT /api/profile
 * 
 * ========================================
 * 2. KONFIGURASI ANDROID
 * ========================================
 * - Base URL sudah diset ke http://10.0.2.2:3000/api/ (untuk emulator)
 * - Untuk device fisik, ganti dengan IP komputer Anda: http://192.168.x.x:3000/api/
 * - Permission INTERNET sudah ditambahkan di AndroidManifest.xml
 * 
 * ========================================
 * 3. CARA PENGGUNAAN DI NAVIGATION
 * ========================================
 * 
 * // Di MainActivity atau NavHost
 * @Composable
 * fun AppNavigation() {
 *     val navController = rememberNavController()
 *     val context = LocalContext.current
 *     val tokenManager = remember { TokenManager(context) }
 *     
 *     // Check if user is already logged in
 *     val startDestination = if (tokenManager.isLoggedIn()) "main" else "login"
 *     
 *     NavHost(
 *         navController = navController,
 *         startDestination = startDestination
 *     ) {
 *         composable("login") {
 *             LoginScreen(
 *                 onLoginSuccess = {
 *                     navController.navigate("main") {
 *                         popUpTo("login") { inclusive = true }
 *                     }
 *                 },
 *                 onNavigateToSignup = {
 *                     navController.navigate("signup")
 *                 }
 *             )
 *         }
 *         
 *         composable("signup") {
 *             SignupScreen(
 *                 onSignupSuccess = {
 *                     navController.navigate("main") {
 *                         popUpTo("signup") { inclusive = true }
 *                     }
 *                 },
 *                 onNavigateToLogin = {
 *                     navController.popBackStack()
 *                 }
 *             )
 *         }
 *         
 *         composable("main") {
 *             MainScreen()
 *         }
 *     }
 * }
 * 
 * ========================================
 * 4. MENYIMPAN TOKEN (OPSIONAL)
 * ========================================
 * 
 * // Di LoginScreen atau SignupScreen success handler
 * LaunchedEffect(uiState.isSuccess, uiState.token) {
 *     if (uiState.isSuccess && uiState.token != null) {
 *         val tokenManager = TokenManager(context)
 *         tokenManager.saveToken(uiState.token)
 *         
 *         uiState.authResponse?.data?.let { userData ->
 *             tokenManager.saveUserData(
 *                 userData.id,
 *                 userData.name,
 *                 userData.email
 *             )
 *         }
 *         
 *         onLoginSuccess()
 *     }
 * }
 * 
 * ========================================
 * 5. TESTING
 * ========================================
 * 
 * 1. Jalankan backend Node.js di port 3000
 * 2. Build dan jalankan aplikasi Android
 * 3. Coba daftar dengan data baru
 * 4. Coba login dengan akun yang sudah dibuat
 * 5. Check log di Android Studio untuk melihat request/response
 * 
 * ========================================
 * 6. TROUBLESHOOTING
 * ========================================
 * 
 * - Jika koneksi gagal, pastikan:
 *   * Backend berjalan di port 3000
 *   * Firewall tidak memblokir koneksi
 *   * Untuk device fisik, gunakan IP yang benar
 * 
 * - Jika error 404, pastikan endpoint backend sesuai:
 *   * /api/login (bukan /auth/login)
 *   * /api/signup (bukan /register)
 * 
 * - Check logcat untuk detail error network
 */
