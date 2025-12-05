# 🚀 Quick Start Guide - Test Sync NOW!

## ⚡ 5 Menit Setup & Testing

### Step 1: Start Backend (1 menit)

```bash
cd "D:\STORA APP\Backend STORA"
npm start
```

✅ **Check:** Server running di `http://localhost:3000`

---

### Step 2: Install & Run App (2 menit)

**Option A: Android Studio**
```
1. Open project: D:\STORA APP\stora2
2. Click Run ▶️
3. Wait for build & install
```

**Option B: Command Line**
```bash
cd "D:\STORA APP\stora2"
./gradlew installDebug
adb shell am start -n com.example.stora/.MainActivity
```

---

### Step 3: Test Sync (2 menit)

#### 3.1 Add Item Offline

1. Buka app → **Inventory** tab
2. Klik tombol **+** (kuning, pojok kanan bawah)
3. Isi form:
   - Name: `Test Item 1`
   - No Inv: `TEST/001`
   - Quantity: `5`
   - Category: `Electronics`
   - Condition: `Baik`
   - Location: `Gudang A`
   - Description: `Testing sync`
   - Date: Pilih tanggal hari ini
4. Klik **SAVE**

**✅ Expected:**
- Item muncul di list
- Badge merah muncul di tombol sync (angka 1)
- Item ada **orange bar** di kiri
- Indicator: **Online** (hijau)

#### 3.2 Sync to Server

1. Klik tombol **Sync** (ada badge merah)
2. Wait loading...
3. Lihat snackbar: "Sinkronisasi berhasil: 1 ke server..."

**✅ Expected:**
- Badge hilang
- Orange bar → Yellow bar
- Tombol sync jadi hijau dengan checkmark

#### 3.3 Verify di Database

```sql
USE stora_db;
SELECT * FROM Inventaris ORDER BY createdAt DESC LIMIT 1;
```

**✅ Expected:**
```
| ID | Nama_Barang  | Kode_Barang | Jumlah | ...
|----|--------------|-------------|--------|-----
| 1  | Test Item 1  | TEST/001    | 5      | ...
```

---

## 📋 Quick Debug

### See Real-Time Logs

```bash
adb logcat -s InventoryRepository ApiConfig | grep -E "sync|POST|✓|✗"
```

### Expected Logs

```
D/InventoryRepository: Starting sync to server: 1 items, 0 deleted
D/ApiConfig: REQUEST: POST http://10.0.2.2:3000/api/v1/inventaris
D/ApiConfig: RESPONSE: 201 (123ms)
D/InventoryRepository: ✓ Item created on server: Test Item 1, serverId: 1
D/InventoryRepository: Sync to server completed: 1 items synced, 0 errors
```

---

## ❌ Troubleshooting

### Problem: "Tidak ada koneksi internet"

**Fix:**
```bash
# Check backend
curl http://localhost:3000/api/v1/inventaris

# Should return: {"success":true,"data":[...],...}
```

### Problem: Sync button tidak ada badge

**Fix:**
1. Force close app
2. Clear app data:
   ```
   Settings → Apps → STORA → Clear Data
   ```
3. Reopen app
4. Add item lagi

### Problem: Error saat sync

**Check Logcat:**
```bash
adb logcat *:E | grep -E "Inventory|Api"
```

**Common errors:**
- `401 Unauthorized` → Backend auth masih aktif (sudah difix)
- `Connection refused` → Backend tidak running
- `Validation error` → Check date format (sudah difix)

---

## 🎯 Success Checklist

- [ ] Backend running di port 3000
- [ ] App installed & running
- [ ] Item bisa ditambah (tersimpan di Room)
- [ ] Badge muncul di sync button
- [ ] Sync berhasil (snackbar muncul)
- [ ] Data masuk ke MySQL database
- [ ] Badge hilang setelah sync
- [ ] Visual indicator berubah (orange → yellow)

**Jika semua ✅ → SYNC WORKS! 🎉**

---

## 🔥 Pro Tips

### Tip 1: Monitor Everything
```bash
# Terminal 1: App logs
adb logcat -s InventoryRepository

# Terminal 2: API logs
adb logcat -s ApiConfig

# Terminal 3: Errors only
adb logcat *:E
```

### Tip 2: Quick Reset
```bash
# Clear Room database
adb shell pm clear com.example.stora

# Restart app
adb shell am start -n com.example.stora/.MainActivity
```

### Tip 3: Test Batch Sync
```
1. Add 5 items tanpa sync
2. Badge should show: 5
3. Klik sync 1x
4. Semua 5 items ter-upload sekaligus
```

---

## 📚 Next Steps

**Setelah sync berhasil, coba:**

1. **Edit Item** → Sync → Verify update di database
2. **Delete Item** → Sync → Verify deleted di database
3. **Add item di database** → Sync → Muncul di app
4. **Offline mode** → Turn off WiFi → Add item → Turn on WiFi → Sync

---

## 📞 Need Help?

**Check logs first:**
```bash
adb logcat -s InventoryRepository ApiConfig | tail -50
```

**Look for:**
- ✓ = Success
- ✗ = Error
- RESPONSE: 201 = Created
- RESPONSE: 200 = Success
- RESPONSE: 401 = Auth error (should not happen, sudah difix)
- RESPONSE: 500 = Server error

**Backend logs:**
Backend terminal akan show:
```
POST /api/v1/inventaris 201 123ms
```

**Database check:**
```sql
SELECT COUNT(*) FROM Inventaris;
-- Should increase after each sync
```

---

## ⚙️ Configuration

**Change Backend URL (if needed):**

File: `app/src/main/java/com/example/stora/network/ApiConfig.kt`

```kotlin
// Emulator
private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"

// Physical Device (ganti dengan IP komputer)
// private const val BASE_URL = "http://192.168.1.100:3000/api/v1/"
```

After change:
```bash
./gradlew clean assembleDebug
```

---

## ✨ What's Working

✅ **Offline Storage** - Data saved in Room Database
✅ **Online Sync** - Upload to MySQL via REST API
✅ **Bidirectional** - Upload & Download
✅ **Visual Feedback** - Badge, indicators, colors
✅ **Error Handling** - Logs & user messages
✅ **No Auth Required** - Untuk testing (temporary)

---

## 🎊 Ready to Test!

1. ✅ Backend: `npm start`
2. ✅ App: Run/Install
3. ✅ Add Item
4. ✅ Click Sync
5. ✅ Check Database

**Total time: < 5 minutes**

**GO! 🚀**