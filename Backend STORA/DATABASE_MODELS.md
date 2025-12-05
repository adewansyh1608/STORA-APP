# STORA Database Models Documentation

## Overview
This document describes the Sequelize models created for the STORA inventory management system. All models are based on the MySQL database schema provided.

## Models Structure

### 1. User Model (`src/models/User.js`)
Represents system users who can manage inventory and borrowing.

**Fields:**
- `ID_User` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
- `Nama_User` (STRING(255))
- `Email` (STRING(255), UNIQUE)
- `Password` (STRING(255)) - Automatically hashed using bcrypt
- `isSynced` (BOOLEAN, DEFAULT: false)
- `isLoggedIn` (BOOLEAN, DEFAULT: false)

**Features:**
- Password hashing with bcrypt (salt rounds: 12)
- Password comparison method
- JSON serialization excludes password
- Email validation

### 2. Inventaris Model (`src/models/Inventaris.js`)
Represents inventory items in the system.

**Fields:**
- `ID_Inventaris` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
- `Nama_Barang` (STRING(255))
- `Kode_Barang` (STRING(255))
- `Jumlah` (INTEGER, MIN: 0)
- `Kategori` (STRING(100))
- `Lokasi` (STRING(255))
- `Kondisi` (ENUM: 'Baik', 'Rusak Ringan', 'Rusak Berat', DEFAULT: 'Baik')
- `Tanggal_Pengadaan` (DATEONLY)
- `ID_User` (INTEGER, FOREIGN KEY → User.ID_User)
- `isSynced` (BOOLEAN, DEFAULT: false)

### 3. FotoInventaris Model (`src/models/FotoInventaris.js`)
Stores photos related to inventory items.

**Fields:**
- `ID_Foto_Inventaris` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
- `ID_Inventaris` (INTEGER, FOREIGN KEY → Inventaris.ID_Inventaris)
- `Foto` (STRING(255))
- `isSynced` (BOOLEAN, DEFAULT: false)

### 4. Peminjaman Model (`src/models/Peminjaman.js`)
Represents borrowing transactions.

**Fields:**
- `ID_Peminjaman` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
- `Nama_Peminjam` (STRING(255))
- `NoHP_Peminjam` (STRING(255))
- `Tanggal_Pinjam` (DATEONLY)
- `Tanggal_Kembali` (DATEONLY)
- `Status` (ENUM: 'Menunggu', 'Dipinjam', 'Selesai', 'Terlambat', 'Ditolak', DEFAULT: 'Menunggu')
- `ID_User` (INTEGER, FOREIGN KEY → User.ID_User)
- `isSynced` (BOOLEAN, DEFAULT: false)

### 5. PeminjamanBarang Model (`src/models/PeminjamanBarang.js`)
Junction table linking borrowing transactions with inventory items.

**Fields:**
- `ID_Peminjaman_Barang` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
- `ID_Peminjaman` (INTEGER, FOREIGN KEY → Peminjaman.ID_Peminjaman)
- `ID_Inventaris` (INTEGER, FOREIGN KEY → Inventaris.ID_Inventaris)
- `Jumlah` (INTEGER, MIN: 1)
- `isSynced` (BOOLEAN, DEFAULT: false)

### 6. FotoPeminjaman Model (`src/models/FotoPeminjaman.js`)
Stores photos related to borrowing transactions.

**Fields:**
- `ID_Foto_Peminjaman` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
- `ID_Peminjaman` (INTEGER, FOREIGN KEY → Peminjaman.ID_Peminjaman)
- `Foto_Peminjaman` (STRING(255))
- `Foto_Pengembalian` (STRING(255))
- `Foto_Barang` (STRING(255))
- `isSynced` (BOOLEAN, DEFAULT: false)

### 7. Notifikasi Model (`src/models/Notifikasi.js`)
Stores notifications related to borrowing activities.

**Fields:**
- `ID_Notifikasi` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
- `Pesan` (STRING(255))
- `Tanggal` (DATEONLY)
- `Status` (STRING(50))
- `ID_Peminjaman` (INTEGER, FOREIGN KEY → Peminjaman.ID_Peminjaman)
- `isSynced` (BOOLEAN, DEFAULT: false)

## Model Associations

### User Associations
- `User.hasMany(Inventaris)` - One user can have many inventory items
- `User.hasMany(Peminjaman)` - One user can handle many borrowing transactions

### Inventaris Associations
- `Inventaris.belongsTo(User)` - Each inventory item belongs to a user
- `Inventaris.hasMany(FotoInventaris)` - One inventory item can have many photos
- `Inventaris.hasMany(PeminjamanBarang)` - One inventory item can be in many borrowing transactions

### Peminjaman Associations
- `Peminjaman.belongsTo(User)` - Each borrowing transaction is handled by a user
- `Peminjaman.hasMany(PeminjamanBarang)` - One borrowing can include many items
- `Peminjaman.hasMany(FotoPeminjaman)` - One borrowing can have many photos
- `Peminjaman.hasMany(Notifikasi)` - One borrowing can have many notifications

### Junction Table Associations
- `PeminjamanBarang.belongsTo(Peminjaman)`
- `PeminjamanBarang.belongsTo(Inventaris)`
- `FotoInventaris.belongsTo(Inventaris)`
- `FotoPeminjaman.belongsTo(Peminjaman)`
- `Notifikasi.belongsTo(Peminjaman)`

## API Endpoints

### User Endpoints
- `GET /api/v1/users` - Get all users (with pagination and search)
- `GET /api/v1/users/:id` - Get user by ID (with related data)
- `POST /api/v1/users` - Create new user
- `POST /api/v1/users/login` - User login
- `PUT /api/v1/users/:id` - Update user
- `DELETE /api/v1/users/:id` - Deactivate user

### Inventaris Endpoints
- `GET /api/v1/inventaris` - Get all inventory items (with filters)
- `GET /api/v1/inventaris/stats` - Get inventory statistics
- `GET /api/v1/inventaris/:id` - Get inventory item by ID
- `POST /api/v1/inventaris` - Create new inventory item
- `PUT /api/v1/inventaris/:id` - Update inventory item
- `DELETE /api/v1/inventaris/:id` - Delete inventory item

### Peminjaman Endpoints
- `GET /api/v1/peminjaman` - Get all borrowing transactions (with filters)
- `GET /api/v1/peminjaman/stats` - Get borrowing statistics
- `GET /api/v1/peminjaman/:id` - Get borrowing transaction by ID
- `POST /api/v1/peminjaman` - Create new borrowing transaction
- `PATCH /api/v1/peminjaman/:id/status` - Update borrowing status

## Usage Examples

### Creating a User
```javascript
const newUser = await User.create({
  Nama_User: 'John Doe',
  Email: 'john@example.com',
  Password: 'password123'
});
```

### Creating Inventory Item
```javascript
const inventaris = await Inventaris.create({
  Nama_Barang: 'Laptop Dell',
  Kode_Barang: 'LT001',
  Jumlah: 5,
  Kategori: 'Electronics',
  Lokasi: 'Room A1',
  Kondisi: 'Baik',
  ID_User: 1
});
```

### Creating Borrowing Transaction
```javascript
const peminjaman = await Peminjaman.create({
  Nama_Peminjam: 'Jane Smith',
  NoHP_Peminjam: '081234567890',
  Tanggal_Pinjam: '2024-01-15',
  Tanggal_Kembali: '2024-01-20',
  ID_User: 1
});

// Add borrowed items
await PeminjamanBarang.create({
  ID_Peminjaman: peminjaman.ID_Peminjaman,
  ID_Inventaris: 1,
  Jumlah: 2
});
```

## Database Configuration

The database connection is configured in `config/db.js`:
- **Host:** localhost
- **Database:** stora_db
- **Username:** root
- **Password:** (empty)
- **Dialect:** mysql

## Features

1. **Automatic Timestamps:** All models include `createdAt` and `updatedAt` fields
2. **Password Security:** User passwords are automatically hashed using bcrypt
3. **Data Validation:** Built-in validation for email formats, number ranges, etc.
4. **Relationships:** Proper foreign key relationships with cascade options
5. **Sync Support:** All models include `isSynced` field for offline synchronization
6. **Enum Values:** Proper enum constraints for status and condition fields

## Installation & Setup

1. Install dependencies:
```bash
npm install sequelize mysql2 bcryptjs
```

2. Configure database in `config/db.js`

3. Run the application:
```bash
npm run dev
```

The models will automatically sync with the database in development mode.
