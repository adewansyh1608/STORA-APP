const http = require('http');

// Configuration
const BASE_URL = 'http://localhost:3000';
const API_BASE = '/api/v1';

// Test data
let authToken = '';
let userId = null;
let inventarisId = null;

// Helper function to make HTTP requests
function makeRequest(method, path, data = null, token = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(BASE_URL + API_BASE + path);

    const options = {
      method: method,
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      headers: {
        'Content-Type': 'application/json',
      },
    };

    if (token) {
      options.headers['Authorization'] = `Bearer ${token}`;
    }

    const req = http.request(options, (res) => {
      let body = '';

      res.on('data', (chunk) => {
        body += chunk;
      });

      res.on('end', () => {
        try {
          const response = {
            status: res.statusCode,
            headers: res.headers,
            body: body ? JSON.parse(body) : null,
          };
          resolve(response);
        } catch (e) {
          resolve({
            status: res.statusCode,
            headers: res.headers,
            body: body,
          });
        }
      });
    });

    req.on('error', (error) => {
      reject(error);
    });

    if (data) {
      req.write(JSON.stringify(data));
    }

    req.end();
  });
}

// Test functions
async function testHealthCheck() {
  console.log('\n📋 Test 1: Health Check');
  try {
    const response = await makeRequest('GET', '/health');
    if (response.status === 200 && response.body.success) {
      console.log('✅ Health check passed');
      console.log(`   Version: ${response.body.version}`);
      return true;
    } else {
      console.log('❌ Health check failed');
      console.log(`   Status: ${response.status}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Health check error:', error.message);
    return false;
  }
}

async function testSignup() {
  console.log('\n📋 Test 2: User Signup');
  const userData = {
    Nama_User: 'API Test User',
    Email: `test.${Date.now()}@example.com`,
    Password: 'Test@12345',
  };

  try {
    const response = await makeRequest('POST', '/signup', userData);
    if (response.status === 201 && response.body.success) {
      userId = response.body.user.ID_User;
      console.log('✅ Signup successful');
      console.log(`   User ID: ${userId}`);
      console.log(`   Email: ${userData.Email}`);
      return { success: true, email: userData.Email, password: userData.Password };
    } else {
      console.log('❌ Signup failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return { success: false };
    }
  } catch (error) {
    console.log('❌ Signup error:', error.message);
    return { success: false };
  }
}

async function testLogin(email, password) {
  console.log('\n📋 Test 3: User Login');
  const loginData = { Email: email, Password: password };

  try {
    const response = await makeRequest('POST', '/login', loginData);
    if (response.status === 200 && response.body.success) {
      authToken = response.body.token;
      userId = response.body.user.ID_User;
      console.log('✅ Login successful');
      console.log(`   Token: ${authToken.substring(0, 30)}...`);
      console.log(`   User ID: ${userId}`);
      return true;
    } else {
      console.log('❌ Login failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Login error:', error.message);
    return false;
  }
}

async function testCreateInventaris() {
  console.log('\n📋 Test 4: Create Inventaris');
  const inventarisData = {
    Nama_Barang: 'Laptop Dell XPS 15',
    Kode_Barang: 'HMSI/ELK/TEST001',
    Jumlah: 5,
    Kategori: 'Elektronik',
    Lokasi: 'Ruang Sekretariat',
    Kondisi: 'Baik',
    Tanggal_Pengadaan: new Date().toISOString().split('T')[0],
  };

  try {
    const response = await makeRequest('POST', '/inventaris', inventarisData, authToken);
    if (response.status === 201 && response.body.success) {
      inventarisId = response.body.data.ID_Inventaris;
      console.log('✅ Create inventaris successful');
      console.log(`   Inventaris ID: ${inventarisId}`);
      console.log(`   Nama: ${response.body.data.Nama_Barang}`);
      console.log(`   Kode: ${response.body.data.Kode_Barang}`);
      return true;
    } else {
      console.log('❌ Create inventaris failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Create inventaris error:', error.message);
    return false;
  }
}

async function testGetAllInventaris() {
  console.log('\n📋 Test 5: Get All Inventaris');

  try {
    const response = await makeRequest('GET', '/inventaris?page=1&limit=10');
    if (response.status === 200 && response.body.success) {
      console.log('✅ Get all inventaris successful');
      console.log(`   Total items: ${response.body.pagination.totalItems}`);
      console.log(`   Items returned: ${response.body.data.length}`);
      console.log(`   Current page: ${response.body.pagination.currentPage}`);
      return true;
    } else {
      console.log('❌ Get all inventaris failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Get all inventaris error:', error.message);
    return false;
  }
}

async function testGetInventarisById() {
  console.log('\n📋 Test 6: Get Inventaris by ID');

  if (!inventarisId) {
    console.log('⚠️  Skipped: No inventaris ID available');
    return true;
  }

  try {
    const response = await makeRequest('GET', `/inventaris/${inventarisId}`);
    if (response.status === 200 && response.body.success) {
      console.log('✅ Get inventaris by ID successful');
      console.log(`   ID: ${response.body.data.ID_Inventaris}`);
      console.log(`   Nama: ${response.body.data.Nama_Barang}`);
      console.log(`   User: ${response.body.data.user?.Nama_User || 'N/A'}`);
      return true;
    } else {
      console.log('❌ Get inventaris by ID failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Get inventaris by ID error:', error.message);
    return false;
  }
}

async function testUpdateInventaris() {
  console.log('\n📋 Test 7: Update Inventaris');

  if (!inventarisId) {
    console.log('⚠️  Skipped: No inventaris ID available');
    return true;
  }

  const updateData = {
    Jumlah: 10,
    Kondisi: 'Baik',
  };

  try {
    const response = await makeRequest('PUT', `/inventaris/${inventarisId}`, updateData, authToken);
    if (response.status === 200 && response.body.success) {
      console.log('✅ Update inventaris successful');
      console.log(`   New Jumlah: ${response.body.data.Jumlah}`);
      console.log(`   New Kondisi: ${response.body.data.Kondisi}`);
      return true;
    } else {
      console.log('❌ Update inventaris failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Update inventaris error:', error.message);
    return false;
  }
}

async function testGetInventarisStats() {
  console.log('\n📋 Test 8: Get Inventaris Statistics');

  try {
    const response = await makeRequest('GET', '/inventaris/stats/summary');
    if (response.status === 200 && response.body.success) {
      console.log('✅ Get statistics successful');
      console.log(`   Total items: ${response.body.data.totalItems}`);
      return true;
    } else {
      console.log('❌ Get statistics failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Get statistics error:', error.message);
    return false;
  }
}

async function testDeleteInventaris() {
  console.log('\n📋 Test 9: Delete Inventaris');

  if (!inventarisId) {
    console.log('⚠️  Skipped: No inventaris ID available');
    return true;
  }

  try {
    const response = await makeRequest('DELETE', `/inventaris/${inventarisId}`, null, authToken);
    if (response.status === 200 && response.body.success) {
      console.log('✅ Delete inventaris successful');
      return true;
    } else {
      console.log('❌ Delete inventaris failed');
      console.log(`   Status: ${response.status}`);
      console.log(`   Message: ${response.body?.message}`);
      return false;
    }
  } catch (error) {
    console.log('❌ Delete inventaris error:', error.message);
    return false;
  }
}

async function testVerifyDeletion() {
  console.log('\n📋 Test 10: Verify Deletion');

  if (!inventarisId) {
    console.log('⚠️  Skipped: No inventaris ID available');
    return true;
  }

  try {
    const response = await makeRequest('GET', `/inventaris/${inventarisId}`);
    if (response.status === 404) {
      console.log('✅ Deletion verified - Item not found (as expected)');
      return true;
    } else {
      console.log('❌ Deletion verification failed - Item still exists');
      return false;
    }
  } catch (error) {
    console.log('❌ Verify deletion error:', error.message);
    return false;
  }
}

// Main test runner
async function runTests() {
  console.log('========================================');
  console.log('🚀 STORA API ENDPOINT TESTS');
  console.log('========================================');
  console.log(`📍 Testing: ${BASE_URL}${API_BASE}`);
  console.log(`⏰ Started: ${new Date().toLocaleString()}`);

  const results = {
    passed: 0,
    failed: 0,
    skipped: 0,
  };

  // Wait for server to be ready
  console.log('\n⏳ Waiting for server to be ready...');
  let serverReady = false;
  for (let i = 0; i < 10; i++) {
    try {
      await makeRequest('GET', '/health');
      serverReady = true;
      break;
    } catch (error) {
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }

  if (!serverReady) {
    console.log('\n❌ Server is not running!');
    console.log('Please start the server with: npm start');
    process.exit(1);
  }

  // Run tests sequentially
  const tests = [
    testHealthCheck,
    async () => {
      const signupResult = await testSignup();
      if (signupResult.success) {
        return await testLogin(signupResult.email, signupResult.password);
      }
      return false;
    },
    testCreateInventaris,
    testGetAllInventaris,
    testGetInventarisById,
    testUpdateInventaris,
    testGetInventarisStats,
    testDeleteInventaris,
    testVerifyDeletion,
  ];

  for (const test of tests) {
    try {
      const result = await test();
      if (result === true) {
        results.passed++;
      } else if (result === 'skip') {
        results.skipped++;
      } else {
        results.failed++;
      }
      // Wait a bit between tests
      await new Promise(resolve => setTimeout(resolve, 500));
    } catch (error) {
      console.log(`\n❌ Test crashed: ${error.message}`);
      results.failed++;
    }
  }

  // Print summary
  console.log('\n========================================');
  console.log('📊 TEST SUMMARY');
  console.log('========================================');
  console.log(`✅ Passed: ${results.passed}`);
  console.log(`❌ Failed: ${results.failed}`);
  console.log(`⚠️  Skipped: ${results.skipped}`);
  console.log(`📈 Total: ${results.passed + results.failed + results.skipped}`);
  console.log(`⏰ Finished: ${new Date().toLocaleString()}`);
  console.log('========================================');

  if (results.failed === 0) {
    console.log('\n🎉 ALL TESTS PASSED! 🎉\n');
    process.exit(0);
  } else {
    console.log('\n⚠️  SOME TESTS FAILED\n');
    process.exit(1);
  }
}

// Handle ctrl+c
process.on('SIGINT', () => {
  console.log('\n\n⚠️  Tests interrupted by user');
  process.exit(1);
});

// Run the tests
runTests().catch((error) => {
  console.error('\n❌ Fatal error:', error);
  process.exit(1);
});
