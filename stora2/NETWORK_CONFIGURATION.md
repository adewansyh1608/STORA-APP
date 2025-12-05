# Network Configuration Guide - STORA App

## 📡 Konfigurasi Koneksi Backend

### Lokasi File Konfigurasi

File yang perlu diubah:
```
STORA APP/stora2/app/src/main/java/com/example/stora/network/ApiConfig.kt
```

## 🔧 Skenario Penggunaan

### 1. Testing dengan Android Emulator

**Base URL:** `http://10.0.2.2:3000/api/v1/`

```kotlin
object ApiConfig {
    private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"
    // ... rest of code
}
```

**Penjelasan:**
- `10.0.2.2` adalah IP khusus Android Emulator yang mengarah ke `localhost` komputer host
- Port `3000` adalah port default backend server
- Gunakan ini saat testing di emulator

**Langkah-langkah:**
1. Pastikan backend server berjalan: `npm start`
2. Buka aplikasi di Android Emulator
3. Test koneksi (indikator Online akan muncul)

---

### 2. Testing dengan Device Fisik (HP Android)

**Base URL:** `http://[IP_KOMPUTER]:3000/api/v1/`

#### Cara Mendapatkan IP Komputer:

**Windows:**
```bash
ipconfig
```
Cari bagian "IPv4 Address", contoh: `192.168.1.100`

**Mac/Linux:**
```bash
ifconfig
# atau
ip addr show
```
Cari IP di interface WiFi (biasanya `en0` atau `wlan0`)

#### Update ApiConfig.kt:
```kotlin
object ApiConfig {
    // Ganti dengan IP komputer Anda
    private const val BASE_URL = "http://192.168.1.100:3000/api/v1/"
    // ... rest of code
}
```

**Requirements:**
- ✅ HP dan Komputer terhubung ke WiFi yang SAMA
- ✅ Firewall komputer dinonaktifkan (atau izinkan port 3000)
- ✅ Backend server running

**Troubleshooting:**
```bash
# Test koneksi dari HP
# Buka browser di HP, akses:
http://192.168.1.100:3000/api/v1/inventaris

# Jika bisa akses, berarti koneksi OK
```

---

### 3. Production / Server Online

**Base URL:** `https://api.yourdomain.com/api/v1/`

```kotlin
object ApiConfig {
    private const val BASE_URL = "https://api.yourdomain.com/api/v1/"
    // ... rest of code
}
```

**Catatan Production:**
- Gunakan HTTPS (SSL/TLS)
- Pastikan domain sudah ter-deploy
- Backend di server (bukan localhost)

---

## 🔐 Konfigurasi SSL (HTTPS)

Jika menggunakan HTTPS tanpa sertifikat valid (self-signed), tambahkan:

```kotlin
private fun provideOkHttpClient(): OkHttpClient {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    return OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Untuk development dengan self-signed certificate
        // .hostnameVerifier { _, _ -> true }
        .build()
}
```

⚠️ **Warning:** Jangan gunakan `hostnameVerifier` bypass di production!

---

## 🌐 Environment-based Configuration

Untuk mengelola multiple environments (dev, staging, prod):

### Cara 1: Build Variants

**build.gradle.kts:**
```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/api/v1/\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://api.yourdomain.com/api/v1/\"")
        }
    }
    
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/api/v1/\"")
        }
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://staging-api.yourdomain.com/api/v1/\"")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.yourdomain.com/api/v1/\"")
        }
    }
}
```

**ApiConfig.kt:**
```kotlin
object ApiConfig {
    private const val BASE_URL = BuildConfig.BASE_URL
    // ... rest of code
}
```

### Cara 2: Local Properties (Recommended)

**local.properties:**
```properties
# Emulator
api.base.url=http://10.0.2.2:3000/api/v1/

# Physical Device
# api.base.url=http://192.168.1.100:3000/api/v1/

# Production
# api.base.url=https://api.yourdomain.com/api/v1/
```

**build.gradle.kts:**
```kotlin
import java.util.Properties

android {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }
    
    defaultConfig {
        buildConfigField("String", "BASE_URL", 
            "\"${localProperties.getProperty("api.base.url", "http://10.0.2.2:3000/api/v1/")}\"")
    }
}
```

---

## 🧪 Testing Koneksi

### 1. Test dari Browser/Postman

```bash
# Test endpoint
GET http://10.0.2.2:3000/api/v1/inventaris
GET http://192.168.1.100:3000/api/v1/inventaris

# Expected Response:
{
  "success": true,
  "data": [...],
  "pagination": {...}
}
```

### 2. Test dari Aplikasi

**Cek Logcat:**
```bash
adb logcat -s OkHttp ApiConfig InventoryRepository
```

**Expected Logs:**
```
I/OkHttp: --> GET http://10.0.2.2:3000/api/v1/inventaris
I/OkHttp: <-- 200 OK (123ms)
I/InventoryRepository: Received 10 items from server
```

### 3. Cek UI Indicator

- **Online:** Icon cloud hijau + text "Online"
- **Offline:** Icon cloud abu-abu + text "Offline"
- **Sync Success:** Snackbar "Sinkronisasi berhasil: X ke server, Y dari server"

---

## 🚨 Troubleshooting

### Error: "Unable to resolve host"

**Problem:** Backend tidak dapat diakses

**Solutions:**
1. **Cek Backend Running:**
   ```bash
   curl http://localhost:3000/api/v1/inventaris
   ```

2. **Cek Firewall:**
   ```bash
   # Windows: Allow port 3000
   netsh advfirewall firewall add rule name="Node.js" dir=in action=allow protocol=TCP localport=3000
   ```

3. **Cek Network:**
   - Pastikan HP dan PC di WiFi yang sama
   - Ping dari HP ke PC:
     ```bash
     ping 192.168.1.100
     ```

### Error: "Network Security Configuration"

**Problem:** Android memblokir HTTP (cleartext)

**Solution:**

**AndroidManifest.xml:**
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

Atau buat `network_security_config.xml`:

**res/xml/network_security_config.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">192.168.1.100</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

**AndroidManifest.xml:**
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### Error: "Connection timeout"

**Problem:** Request terlalu lama

**Solutions:**
1. **Increase timeout di ApiConfig.kt:**
   ```kotlin
   .connectTimeout(60, TimeUnit.SECONDS)
   .readTimeout(60, TimeUnit.SECONDS)
   .writeTimeout(60, TimeUnit.SECONDS)
   ```

2. **Optimize backend query:**
   - Add database indexes
   - Limit hasil query
   - Enable caching

### Error: "401 Unauthorized"

**Problem:** Token authentication gagal

**Solutions:**
1. **Cek token di SharedPreferences:**
   ```kotlin
   val prefs = context.getSharedPreferences("stora_prefs", Context.MODE_PRIVATE)
   val token = prefs.getString("auth_token", null)
   Log.d("Auth", "Token: $token")
   ```

2. **Login ulang untuk refresh token**

3. **Cek header di request:**
   ```kotlin
   @GET("inventaris")
   suspend fun getAllInventory(
       @Header("Authorization") token: String // Format: "Bearer token_here"
   )
   ```

---

## 📝 Checklist Deployment

### Development (Emulator)
- [ ] BASE_URL = `http://10.0.2.2:3000/api/v1/`
- [ ] Backend running di localhost:3000
- [ ] usesCleartextTraffic = true
- [ ] Test CRUD operations
- [ ] Test sync functionality

### Testing (Physical Device)
- [ ] Get IP komputer
- [ ] Update BASE_URL dengan IP komputer
- [ ] HP dan PC di WiFi sama
- [ ] Firewall allow port 3000
- [ ] Test dari browser HP dulu
- [ ] Build & install APK
- [ ] Test semua fitur

### Production
- [ ] Deploy backend ke server
- [ ] Setup domain & SSL
- [ ] Update BASE_URL dengan domain production
- [ ] Remove usesCleartextTraffic (atau set false)
- [ ] Set proper network security config
- [ ] Test dengan real users
- [ ] Monitor logs & analytics

---

## 🔍 Debug Mode

Untuk melihat detail request/response:

**ApiConfig.kt:**
```kotlin
private fun provideOkHttpClient(): OkHttpClient {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY  // Detail logs
        } else {
            HttpLoggingInterceptor.Level.NONE  // No logs in production
        }
    }
    
    return OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("API_REQUEST", "URL: ${request.url}")
            Log.d("API_REQUEST", "Headers: ${request.headers}")
            
            val response = chain.proceed(request)
            Log.d("API_RESPONSE", "Code: ${response.code}")
            
            response
        }
        .build()
}
```

---

## 📞 Quick Reference

| Environment      | Base URL                                    | Use Case                |
|------------------|---------------------------------------------|-------------------------|
| Emulator         | `http://10.0.2.2:3000/api/v1/`            | Local development       |
| Physical Device  | `http://192.168.x.x:3000/api/v1/`         | Local network testing   |
| Staging          | `https://staging-api.domain.com/api/v1/`  | Pre-production testing  |
| Production       | `https://api.domain.com/api/v1/`          | Live environment        |

---

## ✅ Best Practices

1. **Never hardcode production credentials**
2. **Use environment variables**
3. **Enable logging only in debug builds**
4. **Use HTTPS in production**
5. **Implement proper error handling**
6. **Add retry logic for network failures**
7. **Cache responses when appropriate**
8. **Monitor API performance**

---

## 🎯 Summary

- ✅ Gunakan `10.0.2.2` untuk emulator
- ✅ Gunakan IP lokal untuk device fisik
- ✅ Pastikan network security config sudah benar
- ✅ Test koneksi sebelum coding
- ✅ Monitor logs untuk debugging
- ✅ Gunakan HTTPS di production

**Selamat mengembangkan! 🚀**