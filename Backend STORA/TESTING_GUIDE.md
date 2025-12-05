# Testing Guide for STORA Backend

## 🎯 Overview

This guide explains how to test the STORA Backend API to ensure the database sync and all endpoints are working correctly.

## 📋 Prerequisites

1. **MySQL Database Running**
   - Database name: `stora_db`
   - Username: `root`
   - Password: (empty)
   - Host: `localhost`
   - Port: `3306`

2. **Node.js and Dependencies Installed**
   ```bash
   npm install
   ```

3. **Database Tables Created**
   - The tables should already exist in `stora_db`
   - If not, import the SQL schema first

## 🧪 Test Options

### Option 1: Database Model Tests (Recommended First)

This tests the Sequelize models directly without starting the HTTP server.

```bash
node test-inventaris.js
```

**What it tests:**
- ✅ Database connection
- ✅ Model loading (User, Inventaris, etc.)
- ✅ CRUD operations (Create, Read, Update, Delete)
- ✅ Model associations (User → Inventaris)
- ✅ Query methods (findAll, findByPk, findAndCountAll)
- ✅ Aggregate functions (COUNT, GROUP BY)

**Expected Output:**
```
========================================
🧪 TESTING INVENTARIS API
========================================

Test 1: Database Connection
✓ Database connected successfully

Test 2: Check Models
✓ Inventaris model: function
✓ User model: function
...

========================================
✅ ALL TESTS PASSED
========================================
```

### Option 2: API Endpoint Tests

This tests the actual HTTP API endpoints.

**Step 1: Start the Server**
```bash
npm start
```

Wait until you see:
```
🚀 Server running in development mode on port 3000
✅ Database connection established successfully.
📊 Database models synchronized.
```

**Step 2: Run API Tests (in a new terminal)**
```bash
node test-api.js
```

**What it tests:**
- ✅ Health check endpoint
- ✅ User signup
- ✅ User login
- ✅ Create inventaris (with authentication)
- ✅ Get all inventaris
- ✅ Get inventaris by ID
- ✅ Update inventaris
- ✅ Get statistics
- ✅ Delete inventaris
- ✅ Verify deletion

**Expected Output:**
```
========================================
🚀 STORA API ENDPOINT TESTS
========================================
📍 Testing: http://localhost:3000/api/v1
⏰ Started: 12/4/2025, 4:30:00 PM

📋 Test 1: Health Check
✅ Health check passed
   Version: 1.0.0

📋 Test 2: User Signup
✅ Signup successful
   User ID: 1
   Email: test.1701234567890@example.com

...

========================================
📊 TEST SUMMARY
========================================
✅ Passed: 9
❌ Failed: 0
⚠️  Skipped: 0
📈 Total: 9
⏰ Finished: 12/4/2025, 4:30:15 PM
========================================

🎉 ALL TESTS PASSED! 🎉
```

### Option 3: Manual Testing with Mobile App

**Step 1: Start the Server**
```bash
npm start
```

**Step 2: Configure Mobile App**
Make sure your mobile app points to the correct backend URL:
- If testing on physical device: `http://YOUR_IP_ADDRESS:3000/api/v1`
- If testing on emulator/simulator: `http://localhost:3000/api/v1`

**Step 3: Test Features**
1. **Signup**: Create a new account
2. **Login**: Login with credentials
3. **Create Inventory**: Add a new item
4. **View Inventory**: Check if item appears in list
5. **Update Inventory**: Edit an item
6. **Delete Inventory**: Remove an item

## 🔧 Troubleshooting

### Problem: "Cannot read properties of undefined"

**Solution:** The models are not loading properly. Check:
1. Is `src/models/index.js` exporting all models?
2. Are all model files in `src/models/` directory?
3. Run: `node -e "const models = require('./src/models'); console.log(Object.keys(models));"`

### Problem: "Unknown column 'createdAt'"

**Solution:** Timestamp mismatch. All models should have `timestamps: false`.

Check each model file:
```javascript
{
  tableName: 'inventaris',
  timestamps: false,  // ← Must be false
}
```

### Problem: "Cannot connect to database"

**Solution:** Check MySQL connection:
1. Is MySQL running? Check with: `mysql -u root -e "SELECT 1"`
2. Does database exist? Check with: `mysql -u root -e "SHOW DATABASES LIKE 'stora_db'"`
3. Are credentials correct in `config/db.js`?

### Problem: "Port 3000 already in use"

**Solution:** Kill the existing process:
```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID_NUMBER> /F

# Linux/Mac
lsof -ti:3000 | xargs kill -9
```

Or change the port in `.env`:
```
PORT=3001
```

### Problem: API returns 500 errors

**Solution:** Check server logs for detailed error messages.
Common causes:
- Model not exported from `src/models/index.js`
- Wrong table name in model definition
- Missing required fields in request body

## ✅ Verification Checklist

After running tests, verify:

- [ ] Database connection successful
- [ ] Models loaded without errors
- [ ] User signup and login working
- [ ] Inventaris CRUD operations working
- [ ] Data persists in database
- [ ] Associations (User → Inventaris) working
- [ ] No console errors in server logs

## 📝 What Was Fixed

### Database Sync Issue (RESOLVED ✅)

**Problems:**
1. Models not exported properly
2. Timestamp mismatch with database
3. Table name case sensitivity issues
4. Models not initialized in app

**Solutions Applied:**
1. ✅ Fixed `src/models/index.js` to export all models
2. ✅ Set `timestamps: false` in all models
3. ✅ Changed table names to lowercase
4. ✅ Added model initialization in `app.js`

See `SYNC_FIX_NOTES.md` for detailed technical documentation.

## 🎯 Next Steps

Now that database sync is fixed, address remaining issues:

### Issue 2: User Inventory Isolation
**Problem:** Inventory from one user appears when another user logs in.

**To Fix (Frontend):**
- Store user ID in AsyncStorage
- Filter inventory by current user ID
- Clear inventory state on logout

### Issue 3: Persistent Login
**Problem:** App bypasses login after logout.

**To Fix (Frontend):**
- Implement proper logout that clears token
- Check token validity on app start
- Show login screen if token missing/invalid

## 📚 Additional Resources

- **API Documentation:** Check `src/routes/` for available endpoints
- **Model Definitions:** Check `src/models/` for database schema
- **Environment Config:** Check `.env` for configuration options

## 🆘 Need Help?

If tests still fail after following this guide:

1. Check server logs for detailed errors
2. Verify database structure matches model definitions
3. Ensure all dependencies are installed (`npm install`)
4. Try deleting `node_modules` and reinstalling

---

**Last Updated:** 2025-12-04  
**Status:** Database sync ✅ FIXED and TESTED