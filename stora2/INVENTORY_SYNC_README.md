# Inventory Sync Implementation - STORA App

## 📋 Overview

Implementasi sistem inventory dengan sinkronisasi data antara aplikasi Android dan Backend Server. Sistem ini menggunakan Room Database untuk penyimpanan lokal dan REST API untuk komunikasi dengan server.

## ✨ Fitur Utama

### 1. **Offline-First Architecture**
- Data disimpan di Room Database (SQLite) di perangkat
- Aplikasi tetap berfungsi penuh tanpa koneksi internet
- Data lokal selalu tersedia dan cepat diakses

### 2. **Auto Sync**
- Sinkronisasi otomatis saat aplikasi dibuka (jika online)
- Sinkronisasi manual melalui tombol sync
- Badge notifikasi menampilkan jumlah data yang belum tersinkronisasi
- Indikator visual untuk item yang belum disinkronkan

### 3. **Bidirectional Sync**
- **To Server**: Upload data baru/perubahan dari lokal ke server
- **From Server**: Download data terbaru dari server ke lokal
- Conflict resolution otomatis berdasarkan timestamp

### 4. **Smart Data Management**
- Soft delete untuk item yang dihapus
- Tracking status sinkronisasi per item
- Server ID mapping untuk setiap item lokal
- Timestamp untuk tracking perubahan terakhir

## 🏗️ Arsitektur

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Kotlin)                  │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Presentation Layer (Compose)            │  │
│  │  - InventoryScreen                                │  │
│  │  - AddItemScreen                                  │  │
│  │  - EditInventoryScreen                            │  │
│  │  - DetailInventoryScreen                          │  │
│  └──────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌──────────────────────────────────────────────────┐  │
│  │           ViewModel Layer                         │  │
│  │  - InventoryViewModel                             │  │
│  │    • State Management                             │  │
│  │    • Business Logic                               │  │
│  │    • Sync Orchestration                           │  │
│  └──────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Repository Layer                        │  │
│  │  - InventoryRepository                            │  │
│  │    • Local + Remote Data Source                   │  │
│  │    • Sync Logic                                   │  │
│  └──────────────────────────────────────────────────┘  │
│                         │                                │
│          ┌──────────────┴──────────────┐                │
│          ▼                              ▼                │
│  ┌──────────────┐              ┌──────────────┐        │
│  │ Room Database│              │  API Service │        │
│  │              │              │  (Retrofit)  │        │
│  │ - InventoryDao              └──────────────┘        │
│  │ - AppDatabase│                      │                │
│  └──────────────┘                      │                │
│                                        │                │
└────────────────────────────────────────┼────────────────┘
                                         │
                                         ▼
                         ┌────────────────────────────┐
                         │    Backend Server (Node.js) │
                         │    - Express + Sequelize    │
                         │    - MySQL Database         │
                         └────────────────────────────┘
```

## 📦 Komponen Utama

### Android App

#### 1. **Data Layer**

**InventoryItem.kt** - Entity untuk Room Database
```kotlin
@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val noinv: String,
    val quantity: Int,
    val category: String,
    val condition: String,
    val location: String,
    val description: String,
    val date: String,
    val photoUri: String?,
    val serverId: Int?,          // ID dari server
    val isSynced: Boolean,       // Status sinkronisasi
    val isDeleted: Boolean,      // Soft delete flag
    val lastModified: Long,      // Timestamp perubahan
    val needsSync: Boolean       // Perlu disinkronkan
)
```

**InventoryDao.kt** - Data Access Object
- CRUD operations untuk inventory
- Query untuk mendapatkan unsynced items
- Soft delete implementation
- Search dan filter functionality

**AppDatabase.kt** - Room Database Configuration
- Database singleton instance
- Migration strategy

**InventoryApiModels.kt** - API Data Models
- Request/Response models
- Mapping functions untuk konversi data

#### 2. **Network Layer**

**ApiService.kt** - Retrofit Interface
```kotlin
interface ApiService {
    // Inventory endpoints
    @GET("inventaris")
    suspend fun getAllInventory(...)
    
    @POST("inventaris")
    suspend fun createInventory(...)
    
    @PUT("inventaris/{id}")
    suspend fun updateInventory(...)
    
    @DELETE("inventaris/{id}")
    suspend fun deleteInventory(...)
}
```

**ApiConfig.kt** - Retrofit Configuration
- Base URL: `http://10.0.2.2:3000/api/v1/` (Android Emulator)
- Untuk device fisik: ganti dengan IP lokal komputer

#### 3. **Repository Layer**

**InventoryRepository.kt** - Data Management
```kotlin
class InventoryRepository {
    // Local operations
    fun getAllInventoryItems(): Flow<List<InventoryItem>>
    suspend fun insertInventoryItem(item: InventoryItem)
    suspend fun updateInventoryItem(item: InventoryItem)
    suspend fun deleteInventoryItem(id: String)
    
    // Sync operations
    suspend fun syncToServer(): Result<Int>
    suspend fun syncFromServer(): Result<Int>
    suspend fun performFullSync(): Result<Pair<Int, Int>>
}
```

**Fitur Sync:**
- `syncToServer()`: Upload perubahan lokal ke server
- `syncFromServer()`: Download data dari server
- `performFullSync()`: Sinkronisasi bidirectional lengkap

#### 4. **ViewModel Layer**

**InventoryViewModel.kt** - UI State Management
```kotlin
class InventoryViewModel : AndroidViewModel {
    val inventoryItems: StateFlow<List<InventoryItem>>
    val isLoading: StateFlow<Boolean>
    val isSyncing: StateFlow<Boolean>
    val syncStatus: StateFlow<String?>
    val unsyncedCount: StateFlow<Int>
    
    fun addInventoryItem(item: InventoryItem)
    fun updateInventoryItem(item: InventoryItem)
    fun deleteInventoryItem(id: String)
    fun syncData()
}
```

#### 5. **UI Layer**

**InventoryScreen.kt**
- Menampilkan daftar inventory
- Search functionality
- Sync button dengan badge counter
- Online/Offline indicator
- Visual feedback untuk unsynced items

**AddItemScreen.kt**
- Form tambah item baru
- Photo upload
- Validasi input
- Auto save ke Room Database

**EditInventoryScreen.kt**
- Edit item existing
- Update dengan sync flag

**DetailInventoryScreen.kt**
- Detail lengkap item
- Delete dengan konfirmasi

### Backend Server

#### API Endpoints

**Base URL:** `http://localhost:3000/api/v1/`

1. **GET /inventaris**
   - Get all inventory items
   - Query params: `page`, `limit`, `search`, `kategori`, `kondisi`
   - Response: List of inventory items with pagination

2. **GET /inventaris/:id**
   - Get single inventory item
   - Response: Single inventory item detail

3. **POST /inventaris**
   - Create new inventory item
   - Body: InventoryRequest
   - Response: Created item with ID

4. **PUT /inventaris/:id**
   - Update existing inventory item
   - Body: InventoryRequest
   - Response: Updated item

5. **DELETE /inventaris/:id**
   - Delete inventory item
   - Response: Success message

#### Database Schema

**Tabel: Inventaris**
```sql
CREATE TABLE Inventaris (
    ID_Inventaris INT PRIMARY KEY AUTO_INCREMENT,
    Nama_Barang VARCHAR(255),
    Kode_Barang VARCHAR(255),
    Jumlah INT,
    Kategori VARCHAR(100),
    Lokasi VARCHAR(255),
    Kondisi ENUM('Baik', 'Rusak Ringan', 'Rusak Berat'),
    Tanggal_Pengadaan DATE,
    ID_User INT,
    isSynced BOOLEAN DEFAULT FALSE,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP
);
```

## 🔄 Flow Sinkronisasi

### 1. Create New Item (Offline)
```
User Input → InventoryViewModel.addInventoryItem()
          ↓
Set needsSync = true, isSynced = false
          ↓
Save to Room Database
          ↓
UI Updated (Item shown with orange indicator)
          ↓
When Online: Auto sync atau manual sync
          ↓
POST to /inventaris → Get serverId
          ↓
Update local item with serverId, isSynced = true
```

### 2. Sync from Server
```
User clicks Sync (or Auto on app start)
          ↓
InventoryViewModel.syncData()
          ↓
Repository.performFullSync()
          ↓
1. syncToServer() - Upload local changes
   - Get unsynced items
   - For each item:
     • If serverId exists → PUT /inventaris/:id
     • If no serverId → POST /inventaris
   - Update local items dengan serverId
          ↓
2. syncFromServer() - Download server data
   - GET /inventaris?limit=1000
   - For each server item:
     • Check if exists locally (by serverId)
     • Insert or Update in Room
          ↓
UI Updated with synced data
```

### 3. Delete Item
```
User clicks Delete
          ↓
InventoryViewModel.deleteInventoryItem(id)
          ↓
Repository.deleteInventoryItem(id)
          ↓
Soft Delete: isDeleted = true, needsSync = true
          ↓
When Sync:
- If serverId exists → DELETE /inventaris/:id
- Then hard delete from Room
```

## 🚀 Setup & Installation

### Prerequisites
- Android Studio (latest version)
- Node.js v14+
- MySQL Database
- JDK 11+

### Android App Setup

1. **Clone repository**
   ```bash
   cd "STORA APP/stora2"
   ```

2. **Update API Base URL**
   Edit `ApiConfig.kt`:
   ```kotlin
   // Untuk Emulator
   private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"
   
   // Untuk Device Fisik (ganti dengan IP komputer)
   private const val BASE_URL = "http://192.168.1.100:3000/api/v1/"
   ```

3. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Atau run dari Android Studio

### Backend Setup

1. **Install Dependencies**
   ```bash
   cd "STORA APP/Backend STORA"
   npm install
   ```

2. **Setup Database**
   - Create MySQL database
   - Update `config/db.js` dengan credentials
   - Run migrations:
     ```bash
     npm run migrate
     ```

3. **Start Server**
   ```bash
   npm start
   ```
   Server will run on `http://localhost:3000`

## 📱 Cara Menggunakan

### Menambah Item Baru (Offline)
1. Buka aplikasi
2. Klik tombol **+** di InventoryScreen
3. Isi form (nama, kode, qty, kategori, dll)
4. Klik **Save**
5. Item tersimpan di Room Database
6. Badge muncul di tombol sync (menunjukkan ada data belum sync)
7. Saat online, klik tombol **Sync** atau biarkan auto-sync

### Sinkronisasi Manual
1. Di InventoryScreen, lihat indicator Online/Offline
2. Jika ada badge di tombol sync (angka merah), klik tombol tersebut
3. Loading indicator muncul
4. Snackbar menampilkan status sync
5. Badge hilang jika semua data berhasil sync

### Melihat Status Sync
- **Orange bar** di kiri card = Item belum disinkronkan
- **Yellow bar** di kiri card = Item sudah disinkronkan
- **Cloud icon** di pojok kanan card = Status sync
- **Badge merah** di tombol sync = Jumlah item belum sync

### Edit & Delete (Offline)
1. Klik item untuk detail
2. Klik icon **Edit** atau **Delete**
3. Perubahan disimpan lokal dengan flag `needsSync`
4. Saat sync, perubahan dikirim ke server

## 🔧 Troubleshooting

### Connection Issues

**Problem:** Cannot connect to backend
```
Solution:
1. Pastikan backend server running (npm start)
2. Cek BASE_URL di ApiConfig.kt
3. Untuk emulator: gunakan 10.0.2.2
4. Untuk device fisik: 
   - Pastikan di network yang sama
   - Gunakan IP address komputer
   - Nonaktifkan firewall jika perlu
```

### Sync Tidak Berfungsi

**Problem:** Data tidak tersinkronisasi
```
Solution:
1. Cek koneksi internet (lihat indicator Online/Offline)
2. Cek log di Logcat dengan filter "InventoryRepository"
3. Pastikan token auth valid
4. Cek response API di backend logs
```

### Database Conflict

**Problem:** Data duplikat atau konflik
```
Solution:
1. Clear app data:
   Settings → Apps → STORA → Clear Data
2. Atau hapus database manual:
   adb shell
   cd /data/data/com.example.stora/databases
   rm stora_database*
3. Restart app (akan download ulang dari server)
```

## 📊 Monitoring & Logging

### Android Logs
```bash
adb logcat -s InventoryViewModel InventoryRepository
```

Key log messages:
- `"Loaded X items from database"`
- `"Starting full sync..."`
- `"Sync completed successfully"`
- `"Sync to server completed: X items"`
- `"Sync from server completed: X items"`

### Backend Logs
Server logs menampilkan:
- Incoming requests
- Database operations
- Sync activities

## 🎯 Best Practices

### Untuk Development

1. **Always Test Offline First**
   - Disable network di emulator
   - Tambah data offline
   - Enable network
   - Test sync

2. **Monitor Sync Status**
   - Perhatikan badge counter
   - Cek visual indicators
   - Baca sync messages

3. **Handle Edge Cases**
   - Item deleted di server
   - Concurrent updates
   - Network timeout

### Untuk Production

1. **Authentication**
   - Simpan token di SharedPreferences
   - Refresh token jika expired
   - Handle unauthorized response

2. **Error Handling**
   - Retry failed syncs
   - Queue failed operations
   - User-friendly error messages

3. **Performance**
   - Batch sync operations
   - Lazy loading untuk list besar
   - Background sync dengan WorkManager

## 🔮 Future Enhancements

### Planned Features

1. **Background Sync Worker**
   ```kotlin
   // Periodic sync menggunakan WorkManager
   class SyncWorker : CoroutineWorker() {
       override suspend fun doWork(): Result {
           repository.performFullSync()
           return Result.success()
       }
   }
   ```

2. **Conflict Resolution UI**
   - Show conflicting changes
   - Let user choose version
   - Merge changes intelligently

3. **Delta Sync**
   - Only sync changed fields
   - Reduce bandwidth usage
   - Faster sync times

4. **Photo Sync**
   - Upload images to server
   - Compress before upload
   - Thumbnail generation

5. **Real-time Sync**
   - WebSocket connection
   - Push notifications
   - Instant updates

## 📄 API Mapping

### Android → Backend Field Mapping

| Android (Room)    | Backend (MySQL)      | Type    |
|-------------------|----------------------|---------|
| id                | -                    | String  |
| serverId          | ID_Inventaris        | Int     |
| name              | Nama_Barang          | String  |
| noinv             | Kode_Barang          | String  |
| quantity          | Jumlah               | Int     |
| category          | Kategori             | String  |
| location          | Lokasi               | String  |
| condition         | Kondisi              | Enum    |
| date              | Tanggal_Pengadaan    | Date    |
| description       | -                    | String  |
| photoUri          | -                    | String  |
| isSynced          | isSynced             | Boolean |
| needsSync         | -                    | Boolean |
| isDeleted         | -                    | Boolean |
| lastModified      | updatedAt            | Long    |

## 🤝 Contributing

Saat menambahkan fitur baru:
1. Update InventoryItem entity jika perlu field baru
2. Update API models (InventoryApiModels.kt)
3. Update backend schema
4. Test offline & online scenarios
5. Update dokumentasi

## 📞 Support

Jika ada pertanyaan atau issue:
1. Cek dokumentasi ini
2. Cek log aplikasi
3. Cek backend logs
4. Review Troubleshooting section

## 🎉 Summary

Sistem inventory sync sudah lengkap dengan:
- ✅ Room Database untuk storage lokal
- ✅ REST API untuk komunikasi server
- ✅ Bidirectional sync (to/from server)
- ✅ Offline-first architecture
- ✅ Visual sync indicators
- ✅ Auto & manual sync
- ✅ Soft delete support
- ✅ Conflict handling

**Semua fungsionalitas inventory sudah terintegrasi dan berfungsi tanpa error!** 🚀