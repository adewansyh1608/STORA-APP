const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api/v1';

let authToken = null;
let userId = null;

// Helper function to log
function log(title, data) {
  console.log('\n' + '='.repeat(60));
  console.log(title);
  console.log('='.repeat(60));
  console.log(JSON.stringify(data, null, 2));
}

// Helper function to log error
function logError(title, error) {
  console.log('\n' + '='.repeat(60));
  console.log('❌ ' + title);
  console.log('='.repeat(60));
  if (error.response) {
    console.log('Status:', error.response.status);
    console.log('Data:', JSON.stringify(error.response.data, null, 2));
  } else {
    console.log('Error:', error.message);
  }
}

// Test 1: Login
async function testLogin() {
  try {
    console.log('\n🔐 Testing Login...');
    const response = await axios.post(`${BASE_URL}/login`, {
      email: 'test@example.com',
      password: 'password123',
    });

    if (response.data.success && response.data.token) {
      authToken = response.data.token;
      userId = response.data.data?.ID_User;
      log('✅ Login Successful', {
        token: authToken.substring(0, 30) + '...',
        userId: userId,
        userName: response.data.data?.Nama_User,
      });
      return true;
    } else {
      log('❌ Login Failed', response.data);
      return false;
    }
  } catch (error) {
    logError('Login Failed', error);
    return false;
  }
}

// Test 2: Get All Inventory (without auth)
async function testGetInventoryNoAuth() {
  try {
    console.log('\n📋 Testing GET Inventory (No Auth)...');
    const response = await axios.get(`${BASE_URL}/inventaris`, {
      params: {
        page: 1,
        limit: 10,
      },
    });

    log('✅ GET Inventory Successful (No Auth)', {
      success: response.data.success,
      itemCount: response.data.data?.length || 0,
      pagination: response.data.pagination,
    });
    return true;
  } catch (error) {
    logError('GET Inventory Failed (No Auth)', error);
    return false;
  }
}

// Test 3: Get All Inventory (with auth)
async function testGetInventoryWithAuth() {
  try {
    console.log('\n📋 Testing GET Inventory (With Auth)...');
    const response = await axios.get(`${BASE_URL}/inventaris`, {
      params: {
        page: 1,
        limit: 1000,
      },
      headers: {
        Authorization: `Bearer ${authToken}`,
      },
    });

    log('✅ GET Inventory Successful (With Auth)', {
      success: response.data.success,
      itemCount: response.data.data?.length || 0,
      pagination: response.data.pagination,
    });

    if (response.data.data && response.data.data.length > 0) {
      console.log('\nSample item:');
      console.log(JSON.stringify(response.data.data[0], null, 2));
    }

    return true;
  } catch (error) {
    logError('GET Inventory Failed (With Auth)', error);
    return false;
  }
}

// Test 4: Create Inventory (with auth)
async function testCreateInventory() {
  try {
    console.log('\n➕ Testing CREATE Inventory...');

    const testItem = {
      Nama_Barang: 'Test Item ' + Date.now(),
      Kode_Barang: 'TEST-' + Date.now(),
      Jumlah: 10,
      Kategori: 'Elektronik',
      Lokasi: 'Gudang A',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2024-01-15',
    };

    console.log('Request body:', JSON.stringify(testItem, null, 2));
    console.log('Auth token:', authToken.substring(0, 30) + '...');

    const response = await axios.post(`${BASE_URL}/inventaris`, testItem, {
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
    });

    log('✅ CREATE Inventory Successful', {
      success: response.data.success,
      message: response.data.message,
      itemId: response.data.data?.ID_Inventaris,
      itemName: response.data.data?.Nama_Barang,
    });

    return response.data.data?.ID_Inventaris;
  } catch (error) {
    logError('CREATE Inventory Failed', error);
    return null;
  }
}

// Test 5: Create Inventory (without auth) - should fail
async function testCreateInventoryNoAuth() {
  try {
    console.log('\n➕ Testing CREATE Inventory (No Auth - Should Fail)...');

    const testItem = {
      Nama_Barang: 'Test Item ' + Date.now(),
      Kode_Barang: 'TEST-' + Date.now(),
      Jumlah: 10,
      Kategori: 'Elektronik',
      Lokasi: 'Gudang A',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2024-01-15',
    };

    const response = await axios.post(`${BASE_URL}/inventaris`, testItem);

    log('❌ CREATE Inventory Should Have Failed But Succeeded', response.data);
    return false;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      log('✅ CREATE Inventory Correctly Rejected (No Auth)', {
        status: error.response.status,
        message: error.response.data.message,
      });
      return true;
    }
    logError('CREATE Inventory Unexpected Error', error);
    return false;
  }
}

// Test 6: Update Inventory
async function testUpdateInventory(itemId) {
  if (!itemId) {
    console.log('\n⚠️ Skipping UPDATE test - no item ID');
    return false;
  }

  try {
    console.log('\n✏️ Testing UPDATE Inventory...');

    const updateData = {
      Nama_Barang: 'Updated Test Item ' + Date.now(),
      Kode_Barang: 'UPDATED-' + Date.now(),
      Jumlah: 20,
      Kategori: 'Elektronik',
      Lokasi: 'Gudang B',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2024-01-20',
    };

    const response = await axios.put(
      `${BASE_URL}/inventaris/${itemId}`,
      updateData,
      {
        headers: {
          Authorization: `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
      }
    );

    log('✅ UPDATE Inventory Successful', {
      success: response.data.success,
      message: response.data.message,
      itemName: response.data.data?.Nama_Barang,
    });

    return true;
  } catch (error) {
    logError('UPDATE Inventory Failed', error);
    return false;
  }
}

// Test 7: Delete Inventory
async function testDeleteInventory(itemId) {
  if (!itemId) {
    console.log('\n⚠️ Skipping DELETE test - no item ID');
    return false;
  }

  try {
    console.log('\n🗑️ Testing DELETE Inventory...');

    const response = await axios.delete(`${BASE_URL}/inventaris/${itemId}`, {
      headers: {
        Authorization: `Bearer ${authToken}`,
      },
    });

    log('✅ DELETE Inventory Successful', {
      success: response.data.success,
      message: response.data.message,
    });

    return true;
  } catch (error) {
    logError('DELETE Inventory Failed', error);
    return false;
  }
}

// Run all tests
async function runAllTests() {
  console.log('\n');
  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║          STORA BACKEND SYNC API TEST SUITE                ║');
  console.log('╚════════════════════════════════════════════════════════════╝');
  console.log(`\nBase URL: ${BASE_URL}`);
  console.log('Time:', new Date().toISOString());

  let results = {
    total: 0,
    passed: 0,
    failed: 0,
  };

  // Test 1: Login
  results.total++;
  const loginSuccess = await testLogin();
  if (loginSuccess) results.passed++;
  else results.failed++;

  if (!loginSuccess) {
    console.log('\n❌ Cannot continue without login. Please check:');
    console.log('1. Backend server is running (npm start)');
    console.log('2. Database is connected');
    console.log('3. User test@example.com exists with password: password123');
    console.log('\nTo create test user, run:');
    console.log('POST /api/v1/signup with body:');
    console.log(
      JSON.stringify(
        {
          Nama_User: 'Test User',
          Email: 'test@example.com',
          Password: 'password123',
          No_Telp: '08123456789',
        },
        null,
        2
      )
    );
    return;
  }

  // Test 2: GET without auth
  results.total++;
  if (await testGetInventoryNoAuth()) results.passed++;
  else results.failed++;

  // Test 3: GET with auth
  results.total++;
  if (await testGetInventoryWithAuth()) results.passed++;
  else results.failed++;

  // Test 4: CREATE without auth (should fail)
  results.total++;
  if (await testCreateInventoryNoAuth()) results.passed++;
  else results.failed++;

  // Test 5: CREATE with auth
  results.total++;
  const createdItemId = await testCreateInventory();
  if (createdItemId) results.passed++;
  else results.failed++;

  // Test 6: UPDATE with auth
  if (createdItemId) {
    results.total++;
    if (await testUpdateInventory(createdItemId)) results.passed++;
    else results.failed++;
  }

  // Test 7: DELETE with auth
  if (createdItemId) {
    results.total++;
    if (await testDeleteInventory(createdItemId)) results.passed++;
    else results.failed++;
  }

  // Summary
  console.log('\n');
  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║                      TEST SUMMARY                          ║');
  console.log('╚════════════════════════════════════════════════════════════╝');
  console.log(`Total Tests: ${results.total}`);
  console.log(`✅ Passed: ${results.passed}`);
  console.log(`❌ Failed: ${results.failed}`);
  console.log(
    `Success Rate: ${((results.passed / results.total) * 100).toFixed(1)}%`
  );
  console.log('');

  if (results.failed === 0) {
    console.log('🎉 All tests passed! Backend is working correctly.');
  } else {
    console.log('⚠️ Some tests failed. Check the logs above for details.');
  }
}

// Run tests
runAllTests().catch((error) => {
  console.error('\n❌ Test suite crashed:', error.message);
  process.exit(1);
});
