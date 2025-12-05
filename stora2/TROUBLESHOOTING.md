# Troubleshooting Guide - File Merah & Koneksi Backend

## 🔴 Masalah File Merah di Android Studio

### Langkah 1: Sync Gradle Project
1. Buka **File** → **Sync Project with Gradle Files**
2. Atau klik ikon **Sync** di toolbar
3. Tunggu sampai sync selesai (bisa 2-5 menit)

### Langkah 2: Clean & Rebuild Project
1. **Build** → **Clean Project**
2. Setelah selesai: **Build** → **Rebuild Project**
3. Tunggu sampai build selesai

### Langkah 3: Invalidate Caches
Jika masih merah:
1. **File** → **Invalidate Caches and Restart**
2. Pilih **Invalidate and Restart**
3. Android Studio akan restart

### Langkah 4: Check Dependencies
Pastikan di `app/build.gradle.kts` ada:
```kotlin
// Retrofit dependencies
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

## 🌐 Test Koneksi Backend

### Cara 1: Gunakan SimpleLoginScreen
1. Tambahkan ke navigation:
```kotlin
composable("test") { SimpleLoginScreen() }
```
2. Navigate ke screen test
3. Masukkan email/password dummy
4. Klik "Test Login"
5. Lihat hasilnya

### Cara 2: Gunakan NetworkTest
1. Panggil di MainActivity:
```kotlin
NetworkTest.testBackendConnection()
```
2. Check Logcat untuk hasil

### Cara 3: Manual Test
1. Buka browser di komputer
2. Akses: `http://localhost:3000/api/auth/login`
3. Harus dapat response (meski error 405/404 OK)

## 🔧 Konfigurasi Network

### Untuk Android Emulator:
- Base URL: `http://10.0.2.2:3000/api/`
- IP `10.0.2.2` = localhost komputer

### Untuk Device Fisik:
1. Cari IP komputer: `ipconfig` (Windows) / `ifconfig` (Mac/Linux)
2. Base URL: `http://[IP_KOMPUTER]:3000/api/`
3. Contoh: `http://192.168.1.100:3000/api/`

### Check Firewall:
- Windows: Allow port 3000 di Windows Firewall
- Antivirus: Pastikan tidak memblokir koneksi

## 🚀 Quick Test

### Test Backend Hidup:
```bash
curl http://localhost:3000/api/login
```

### Test dari Android:
1. Gunakan `SimpleLoginScreen`
2. Email: `test@test.com`
3. Password: `password`
4. Lihat response di message

## 📱 Implementasi Bertahap

### Step 1: Test Koneksi Dulu
Gunakan `SimpleLoginScreen` untuk memastikan backend terhubung

### Step 2: Setelah Koneksi OK
Baru gunakan file Retrofit yang lengkap:
- `LoginScreen.kt`
- `SignupScreen.kt`
- `AuthViewModel.kt`

### Step 3: Integration
Tambahkan ke navigation dan test end-to-end

## ⚠️ Common Issues

1. **File Merah**: Sync Gradle + Clean Build
2. **Network Error**: Check IP address & firewall
3. **Backend Not Found**: Pastikan server berjalan di port 3000
4. **CORS Error**: Tambahkan CORS middleware di backend
5. **Import Error**: Pastikan dependencies ter-install

## 📞 Debug Tips

1. **Logcat Filter**: `NetworkTest` atau `SimpleLogin`
2. **Backend Logs**: Check console Node.js
3. **Network Inspector**: Gunakan Charles/Wireshark
4. **Postman**: Test API endpoints manual
