# Testing Guide - Inventory Sync Functionality

## 🧪 Panduan Testing Sinkronisasi Inventory

### Prerequisites

1. **Backend Server Running**
   ```bash
   cd "D:\STORA APP\Backend STORA"
   npm start
   ```
   Pastikan server running di `http://localhost:3000`

2. **Database Ready**
   - MySQL running
   - Database sudah di-migrate
   - Pastikan tabel `Inventaris` ada

3. **Android App Installed**
   - Build & Install APK ke emulator/device
   - Atau run dari Android Studio

---

## 📱 Step-by-Step Testing

### Test 1: Add Item Offline (Room Database)

**Objective:** Verify data tersimpan di Room saat offline

**Steps:**
1. Buka aplikasi di emulator
2. Go to Inventory Screen
3. Klik tombol **+** (Add Item)
4. Isi form:
   - **Name:** "Test Item 1"
   - **No Inv:** "TEST/001"
   - **Quantity:** 5
   - **Category:** "Electronics"
   - **Condition:** "Baik"
   - **Location:** "Gudang A"
   - **Description:** "Testing offline save"
   - **Date:** Pilih tanggal hari ini
5. Klik **Save**

**Expected Result:**
- ✅ Item muncul di list inventory
- ✅ Item memiliki **orange bar** di kiri (unsynced indicator)
- ✅ Item memiliki **cloud icon** abu-abu
- ✅ Badge di sync button muncul dengan angka **1**
- ✅ Status: **Online** (hijau) di header

**Check Logcat:**
```bash
adb logcat -s InventoryViewModel InventoryRepository
```
Expected logs:
```
I/InventoryViewModel: Item added successfully: Test Item 1
I/InventoryRepository: Item inserted locally: Test Item 1
```

---

### Test 2: Sync to Server

**Objective:** Upload data dari Room ke Backend Server

**Steps:**
1. Di Inventory Screen, pastikan ada item dengan badge (angka merah)
2. Pastikan indicator menunjukkan **Online**
3. Klik tombol **Sync** (tombol dengan icon cloud/sync)
4. Tunggu loading selesai

**Expected Result:**
- ✅ Loading indicator muncul
- ✅ Snackbar muncul: "Sinkronisasi berhasil: 1 ke server, X dari server"
- ✅ Badge di sync button **hilang** atau berkurang
- ✅ Item **orange bar** berubah menjadi **yellow bar**
- ✅ Cloud icon berubah **hilang** (sudah sync)
- ✅ Tombol sync berubah jadi **hijau dengan checkmark**

**Check Logcat:**
```bash
adb logcat -s ApiConfig InventoryRepository
```

Expected logs:
```
D/ApiConfig: ========================================
D/ApiConfig: REQUEST: POST http://10.0.2.2:3000/api/v1/inventaris
D/ApiConfig: Headers: ...
D/InventoryRepository: Creating new item on server: Test Item 1
D/ApiConfig: RESPONSE: 201 (...)
D/InventoryRepository: ✓ Item created on server: Test Item 1, serverId: 1
D/InventoryRepository: Sync to server completed: 1 items synced, 0 errors
```

**Check Backend Logs:**
Terminal backend akan menampilkan:
```
POST /api/v1/inventaris 201 - ... ms
```

**Check Database:**
```sql
SELECT * FROM Inventaris ORDER BY createdAt DESC LIMIT 1;
```
Harus ada 1 row baru dengan:
- Nama_Barang = "Test Item 1"
- Kode_Barang = "TEST/001"
- Jumlah = 5
- dll.

---

### Test 3: Add Multiple Items Offline

**Objective:** Test batch sync

**Steps:**
1. Add 3 items baru:
   - Test Item 2 (TEST/002)
   - Test Item 3 (TEST/003)
   - Test Item 4 (TEST/004)
2. Verify badge menunjukkan angka **3**
3. Click Sync

**Expected Result:**
- ✅ All 3 items ter-sync
- ✅ Badge menjadi 0 atau hijau
- ✅ Snackbar: "Sinkronisasi berhasil: 3 ke server, X dari server"

**Check Logcat:**
```
D/InventoryRepository: Starting sync to server: 3 items, 0 deleted
D/InventoryRepository: ✓ Item created on server: Test Item 2, serverId: 2
D/InventoryRepository: ✓ Item created on server: Test Item 3, serverId: 3
D/InventoryRepository: ✓ Item created on server: Test Item 4, serverId: 4
D/InventoryRepository: Sync to server completed: 3 items synced, 0 errors
```

---

### Test 4: Update Item (Edit)

**Objective:** Test update sync

**Steps:**
1. Click pada item yang sudah sync
2. Click icon **Edit** (pensil)
3. Ubah **Quantity** dari 5 menjadi 10
4. Click **Save**
5. Verify badge muncul lagi (item needs sync)
6. Click **Sync**

**Expected Result:**
- ✅ Item ter-update di server
- ✅ Badge hilang setelah sync

**Check Logcat:**
```
D/InventoryRepository: Updating item on server: Test Item 1, serverId: 1
D/ApiConfig: REQUEST: PUT http://10.0.2.2:3000/api/v1/inventaris/1
D/ApiConfig: RESPONSE: 200 (...)
D/InventoryRepository: ✓ Item updated on server: Test Item 1
```

**Check Database:**
```sql
SELECT Jumlah FROM Inventaris WHERE ID_Inventaris = 1;
-- Should return 10
```

---

### Test 5: Delete Item

**Objective:** Test delete sync

**Steps:**
1. Click item untuk detail
2. Click icon **Delete** (trash)
3. Confirm delete
4. Item hilang dari list (soft delete)
5. Badge muncul
6. Click **Sync**

**Expected Result:**
- ✅ Item dihapus dari server
- ✅ Item dihapus dari Room
- ✅ Badge hilang

**Check Logcat:**
```
D/InventoryRepository: Deleting item on server: Test Item 1, serverId: 1
D/ApiConfig: REQUEST: DELETE http://10.0.2.2:3000/api/v1/inventaris/1
D/ApiConfig: RESPONSE: 200 (...)
D/InventoryRepository: ✓ Deleted item synced: Test Item 1
```

---

### Test 6: Sync from Server (Download)

**Objective:** Download data dari server ke Room

**Steps:**
1. Manually add data ke database:
   ```sql
   INSERT INTO Inventaris (Nama_Barang, Kode_Barang, Jumlah, Kategori, Kondisi, Lokasi, Tanggal_Pengadaan, ID_User)
   VALUES ('Server Item 1', 'SERVER/001', 3, 'Furniture', 'Baik', 'Ruang A', '2025-01-01', 1);
   ```
2. Di app, click **Sync**

**Expected Result:**
- ✅ Item dari server muncul di app
- ✅ Snackbar: "Sinkronisasi berhasil: 0 ke server, 1 dari server"

**Check Logcat:**
```
D/InventoryRepository: Received 5 items from server
D/InventoryRepository: ✓ Sync from server completed: 5 items
```

---

### Test 7: Conflict Resolution

**Objective:** Test update conflict

**Steps:**
1. Add item "Conflict Test" di app
2. Sync to server (get serverId)
3. Update item di database directly:
   ```sql
   UPDATE Inventaris SET Jumlah = 99 WHERE Kode_Barang = 'CONFLICT/001';
   ```
4. Di app, update item yang sama (Jumlah = 50)
5. Click Sync

**Expected Result:**
- ✅ Data di app ter-upload ke server (overwrite)
- ✅ Server data = 50 (from app)

**Note:** Current implementation: Last write wins (app overwrites server)

---

### Test 8: Error Handling

**Objective:** Test error scenarios

**Test 8.1: Backend Down**
1. Stop backend server (`npm stop`)
2. Add item di app
3. Click Sync

**Expected Result:**
- ✅ Error message muncul
- ✅ Badge tetap ada (item belum sync)
- ✅ Item tetap di Room

**Test 8.2: Network Timeout**
1. Buat backend lambat (add delay di controller)
2. Try sync

**Expected Result:**
- ✅ Timeout after 30 seconds
- ✅ Error message

---

## 🔍 Debugging Checklist

### Problem: Sync Button Tidak Muncul Badge

**Check:**
```bash
adb logcat -s InventoryViewModel | grep "Unsynced"
```
- [ ] Ada item dengan `needsSync = true`?
- [ ] ViewModel.unsyncedCount ter-update?

**Fix:**
- Clear app data & restart

---

### Problem: Sync Fails with 401 Unauthorized

**Reason:** Auth middleware masih aktif di backend

**Fix:**
1. Edit `Backend STORA\src\routes\inventarisRoutes.js`
2. Comment out `authMiddleware`:
   ```javascript
   // router.post('/', authMiddleware, ...) // BEFORE
   router.post('/', inventarisValidationRules, ...) // AFTER
   ```
3. Restart backend

---

### Problem: Sync Fails with "Validation Error"

**Check Logcat:**
```bash
adb logcat -s ApiConfig | grep "ERROR BODY"
```

**Common Issues:**
- Missing required field
- Invalid Kondisi value
- Invalid data type

**Fix:**
Check `InventoryApiModels.kt` → `toApiRequest()`:
```kotlin
fun InventoryItem.toApiRequest(): InventoryRequest {
    return InventoryRequest(
        namaBarang = this.name,        // Required
        kodeBarang = this.noinv,       // Required
        jumlah = this.quantity,        // Required, must be Int
        kategori = this.category,      // Required
        lokasi = this.location,        // Required
        kondisi = this.condition,      // Must be: Baik, Rusak Ringan, Rusak Berat
        tanggalPengadaan = this.date,  // Required, format: YYYY-MM-DD
        idUser = 1
    )
}
```

---

### Problem: "Cannot connect to server"

**Check:**
1. Backend running?
   ```bash
   curl http://localhost:3000/api/v1/inventaris
   ```
2. Emulator can reach localhost?
   ```bash
   adb shell
   ping 10.0.2.2
   ```
3. Firewall blocking?

**Fix BASE_URL:**
- Emulator: `http://10.0.2.2:3000/api/v1/`
- Physical device: `http://[YOUR_IP]:3000/api/v1/`

---

## 📊 Monitoring

### Real-time Logcat Monitoring

**Terminal 1: App Logs**
```bash
adb logcat -s InventoryViewModel InventoryRepository ApiConfig | grep -E "sync|Sync|SYNC"
```

**Terminal 2: HTTP Traffic**
```bash
adb logcat -s OkHttp | grep -E "POST|PUT|DELETE|GET"
```

**Terminal 3: Errors Only**
```bash
adb logcat *:E
```

---

## ✅ Success Criteria

After all tests, you should have:

- [ ] Items created offline saved in Room
- [ ] Items synced to server with serverId
- [ ] Items from server downloaded to Room
- [ ] Updates synced to server
- [ ] Deletes synced to server
- [ ] Badge counter works correctly
- [ ] Visual indicators (orange/yellow bar) work
- [ ] Online/Offline indicator accurate
- [ ] Error handling works
- [ ] No app crashes
- [ ] All data in MySQL database

---

## 🎯 Performance Benchmarks

**Normal Sync Performance:**
- Create 1 item: < 500ms
- Sync 1 item: < 1000ms
- Sync 10 items: < 5000ms
- Fetch from server: < 2000ms

**Check with:**
```bash
adb logcat -s ApiConfig | grep "RESPONSE.*ms"
```

---

## 🐛 Common Issues & Solutions

### Issue: Date Format Error

**Error:** "Invalid date format"

**Cause:** Android sends "dd/MM/yyyy", backend expects "YYYY-MM-DD"

**Fix:** Update date format before sending:
```kotlin
fun formatDateForApi(dateString: String): String {
    // Convert from "16/10/2025" to "2025-10-16"
    val parts = dateString.split("/")
    return "${parts[2]}-${parts[1]}-${parts[0]}"
}
```

---

### Issue: All Items Show "Unsynced"

**Cause:** `isSynced` flag not updated

**Fix:**
```bash
# Clear Room database
adb shell
cd /data/data/com.example.stora/databases
rm stora_database*
exit

# Restart app
adb shell am force-stop com.example.stora
adb shell am start -n com.example.stora/.MainActivity
```

---

## 📈 Test Results Template

```
Date: _____________
Tester: ___________
Build: ____________

| Test Case | Status | Notes |
|-----------|--------|-------|
| Add Item Offline | ☐ Pass ☐ Fail | |
| Sync to Server | ☐ Pass ☐ Fail | |
| Batch Sync | ☐ Pass ☐ Fail | |
| Update Sync | ☐ Pass ☐ Fail | |
| Delete Sync | ☐ Pass ☐ Fail | |
| Download from Server | ☐ Pass ☐ Fail | |
| Error Handling | ☐ Pass ☐ Fail | |
| Performance | ☐ Pass ☐ Fail | |

Overall: ☐ PASS ☐ FAIL

Issues Found:
1. 
2. 
3. 

Notes:

```

---

## 🚀 Quick Test Command

Run this to see full sync flow:
```bash
# Monitor all sync activities
adb logcat -s InventoryViewModel InventoryRepository ApiConfig OkHttp \
  | grep -E "sync|Sync|SYNC|POST|PUT|DELETE|✓|✗"
```

---

## 📞 Need Help?

If sync masih tidak berfungsi:

1. **Check Logs:** Semua log ada di Logcat
2. **Check Network:** Pastikan backend accessible
3. **Check Data:** Verify di MySQL database
4. **Check Code:** Review InventoryRepository.kt line 170-315
5. **Check Backend:** Review inventarisController.js line 103-165

**Debug Mode:**
Set breakpoint di:
- `InventoryRepository.syncToServer()` line 187
- `inventarisController.createInventaris()` line 103

---

**Happy Testing! 🎉**