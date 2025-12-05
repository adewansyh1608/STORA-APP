# STORA API - Postman Testing Guide

## Base URL
```
http://localhost:3000/api/v1
```

## Authentication
Untuk endpoint yang memerlukan authentication, gunakan Bearer Token di header:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

---

## 📋 **API Endpoints Collection**

### 1. **Health Check**
**GET** `/health`
```
GET http://localhost:3000/api/v1/health
```

**Response:**
```json
{
  "success": true,
  "message": "STORA API is running",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "version": "1.0.0"
}
```

---

### 2. **Authentication Endpoints**

#### **A. User Signup/Register**
**POST** `/auth/signup`
```
POST http://localhost:3000/api/v1/auth/signup
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "password_confirmation": "password123"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "email_verified_at": null,
    "created_at": "2024-01-01T00:00:00.000Z",
    "updated_at": "2024-01-01T00:00:00.000Z"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### **B. User Login**
**POST** `/auth/login`
```
POST http://localhost:3000/api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "email_verified_at": null,
    "created_at": "2024-01-01T00:00:00.000Z",
    "updated_at": "2024-01-01T00:00:00.000Z"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### **C. User Logout**
**POST** `/auth/logout`
```
POST http://localhost:3000/api/v1/auth/logout
Authorization: Bearer YOUR_JWT_TOKEN
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null,
  "token": null
}
```

#### **D. Get User Profile**
**GET** `/auth/profile`
```
GET http://localhost:3000/api/v1/auth/profile
Authorization: Bearer YOUR_JWT_TOKEN
```

**Response:**
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "email_verified_at": null,
    "created_at": "2024-01-01T00:00:00.000Z",
    "updated_at": "2024-01-01T00:00:00.000Z"
  },
  "token": null
}
```

---

### 3. **User Management Endpoints**

#### **A. Get All Users**
**GET** `/users`
```
GET http://localhost:3000/api/v1/users?page=1&limit=10&search=john
```

#### **B. Get User by ID**
**GET** `/users/:id`
```
GET http://localhost:3000/api/v1/users/1
```

#### **C. Create User**
**POST** `/users`
```
POST http://localhost:3000/api/v1/users
Content-Type: application/json

{
  "Nama_User": "Jane Smith",
  "Email": "jane@example.com",
  "Password": "password123"
}
```

---

### 4. **Inventaris Endpoints**

#### **A. Get All Inventaris**
**GET** `/inventaris`
```
GET http://localhost:3000/api/v1/inventaris?page=1&limit=10&search=laptop&kategori=Electronics&kondisi=Baik
```

#### **B. Get Inventaris by ID**
**GET** `/inventaris/:id`
```
GET http://localhost:3000/api/v1/inventaris/1
```

#### **C. Create Inventaris**
**POST** `/inventaris`
```
POST http://localhost:3000/api/v1/inventaris
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "Nama_Barang": "Laptop Dell",
  "Kode_Barang": "LT001",
  "Jumlah": 5,
  "Kategori": "Electronics",
  "Lokasi": "Room A1",
  "Kondisi": "Baik",
  "Tanggal_Pengadaan": "2024-01-01",
  "ID_User": 1
}
```

#### **D. Get Inventaris Statistics**
**GET** `/inventaris/stats`
```
GET http://localhost:3000/api/v1/inventaris/stats
```

---

### 5. **Peminjaman Endpoints**

#### **A. Get All Peminjaman**
**GET** `/peminjaman`
```
GET http://localhost:3000/api/v1/peminjaman?page=1&limit=10&status=Menunggu&search=john
```

#### **B. Get Peminjaman by ID**
**GET** `/peminjaman/:id`
```
GET http://localhost:3000/api/v1/peminjaman/1
```

#### **C. Create Peminjaman**
**POST** `/peminjaman`
```
POST http://localhost:3000/api/v1/peminjaman
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "Nama_Peminjam": "Jane Smith",
  "NoHP_Peminjam": "081234567890",
  "Tanggal_Pinjam": "2024-01-15",
  "Tanggal_Kembali": "2024-01-20",
  "ID_User": 1,
  "barangList": [
    {
      "ID_Inventaris": 1,
      "Jumlah": 2
    },
    {
      "ID_Inventaris": 2,
      "Jumlah": 1
    }
  ]
}
```

#### **D. Update Peminjaman Status**
**PATCH** `/peminjaman/:id/status`
```
PATCH http://localhost:3000/api/v1/peminjaman/1/status
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "Status": "Dipinjam"
}
```

#### **E. Get Peminjaman Statistics**
**GET** `/peminjaman/stats`
```
GET http://localhost:3000/api/v1/peminjaman/stats
```

---

## 🔧 **Postman Setup Instructions**

### **Step 1: Create New Collection**
1. Open Postman
2. Click "New" → "Collection"
3. Name it "STORA API"

### **Step 2: Set Environment Variables**
1. Click "Environments" → "Create Environment"
2. Name it "STORA Local"
3. Add variables:
   - `base_url`: `http://localhost:3000/api/v1`
   - `token`: (will be set after login)

### **Step 3: Create Requests**
1. Create folders for each endpoint group:
   - Authentication
   - Users
   - Inventaris
   - Peminjaman

### **Step 4: Set Authorization**
For protected endpoints:
1. Go to "Authorization" tab
2. Select "Bearer Token"
3. Token: `{{token}}`

### **Step 5: Test Authentication Flow**
1. **First:** Test signup/login to get token
2. **Copy token** from response
3. **Set token** in environment variables
4. **Test protected endpoints**

---

## 📝 **Testing Workflow**

### **1. Start Server**
```bash
npm run dev:3000
```

### **2. Test Health Check**
```
GET {{base_url}}/health
```

### **3. Register New User**
```
POST {{base_url}}/auth/signup
```

### **4. Login User**
```
POST {{base_url}}/auth/login
```
**Copy the token from response!**

### **5. Set Token in Environment**
Paste the token in your environment variable `token`

### **6. Test Protected Endpoints**
Now you can test all endpoints that require authentication.

---

## ⚠️ **Common Issues & Solutions**

### **Issue 1: Connection Refused**
- **Solution:** Make sure server is running on port 3000
- **Check:** `npm run dev:3000`

### **Issue 2: 401 Unauthorized**
- **Solution:** Make sure you're using valid JWT token
- **Check:** Token format should be `Bearer YOUR_TOKEN`

### **Issue 3: 404 Not Found**
- **Solution:** Check endpoint URL spelling
- **Check:** Base URL should be `http://localhost:3000/api/v1`

### **Issue 4: 500 Internal Server Error**
- **Solution:** Check server logs for database connection
- **Check:** Make sure MySQL is running and database exists

---

## 🚀 **Quick Start Commands**

```bash
# Start server
npm run dev:3000

# Test health check
curl http://localhost:3000/api/v1/health

# Test signup
curl -X POST http://localhost:3000/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"password123","password_confirmation":"password123"}'
```

Happy testing! 🎉
