const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api/v1';

let authToken = '';
let userId = '';

async function testAPI() {
  console.log('=== STORA API TEST ===\n');

  try {
    // 1. Test Signup
    console.log('1. Testing Signup...');
    const signupData = {
      name: 'Test User 1',
      email: `testuser1_${Date.now()}@test.com`,
      password: 'password123',
      password_confirmation: 'password123'
    };
    
    const signupResponse = await axios.post(`${BASE_URL}/signup`, signupData);
    console.log('✅ Signup successful:', signupResponse.data.message);
    authToken = signupResponse.data.token;
    userId = signupResponse.data.data.id;
    console.log('Token:', authToken.substring(0, 20) + '...');
    console.log('User ID:', userId);
    console.log();

    // 2. Test Create Inventory
    console.log('2. Testing Create Inventory...');
    const inventoryData = {
      Nama_Barang: 'Laptop Dell',
      Kode_Barang: 'LPT001',
      Jumlah: 5,
      Kategori: 'Elektronik',
      Lokasi: 'Ruang IT',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2024-01-01'
    };
    
    const createResponse = await axios.post(`${BASE_URL}/inventaris`, inventoryData, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    console.log('✅ Inventory created:', createResponse.data.data.Nama_Barang);
    const inventoryId = createResponse.data.data.ID_Inventaris;
    console.log('Inventory ID:', inventoryId);
    console.log();

    // 3. Test Get All Inventory (should only show user's inventory)
    console.log('3. Testing Get All Inventory...');
    const getAllResponse = await axios.get(`${BASE_URL}/inventaris`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    console.log('✅ Inventory list retrieved:', getAllResponse.data.data.length, 'items');
    console.log('Items:', getAllResponse.data.data.map(item => ({
      id: item.ID_Inventaris,
      name: item.Nama_Barang,
      userId: item.ID_User
    })));
    console.log();

    // 4. Test Login with Second User
    console.log('4. Testing Second User Signup...');
    const signup2Data = {
      name: 'Test User 2',
      email: `testuser2_${Date.now()}@test.com`,
      password: 'password123',
      password_confirmation: 'password123'
    };
    
    const signup2Response = await axios.post(`${BASE_URL}/signup`, signup2Data);
    console.log('✅ Second user signup successful');
    const authToken2 = signup2Response.data.token;
    const userId2 = signup2Response.data.data.id;
    console.log('User 2 ID:', userId2);
    console.log();

    // 5. Test Get All Inventory with Second User (should be empty)
    console.log('5. Testing Get All Inventory with Second User...');
    const getAll2Response = await axios.get(`${BASE_URL}/inventaris`, {
      headers: { Authorization: `Bearer ${authToken2}` }
    });
    console.log('✅ Second user inventory list:', getAll2Response.data.data.length, 'items');
    if (getAll2Response.data.data.length === 0) {
      console.log('✅ CORRECT: Second user has no inventory');
    } else {
      console.log('❌ ERROR: Second user can see first user\'s inventory!');
      console.log('Items:', getAll2Response.data.data.map(item => ({
        id: item.ID_Inventaris,
        name: item.Nama_Barang,
        userId: item.ID_User
      })));
    }
    console.log();

    // 6. Test Create Inventory with Second User
    console.log('6. Testing Create Inventory with Second User...');
    const inventory2Data = {
      Nama_Barang: 'Mouse Logitech',
      Kode_Barang: 'MOU001',
      Jumlah: 10,
      Kategori: 'Elektronik',
      Lokasi: 'Ruang Admin',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2024-01-02'
    };
    
    const create2Response = await axios.post(`${BASE_URL}/inventaris`, inventory2Data, {
      headers: { Authorization: `Bearer ${authToken2}` }
    });
    console.log('✅ Second user inventory created:', create2Response.data.data.Nama_Barang);
    console.log();

    // 7. Verify First User Still Only Sees Their Own Inventory
    console.log('7. Verifying First User Can Only See Their Own Inventory...');
    const getAll3Response = await axios.get(`${BASE_URL}/inventaris`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    console.log('✅ First user inventory list:', getAll3Response.data.data.length, 'items');
    const hasOnlyOwnItems = getAll3Response.data.data.every(item => item.ID_User === userId);
    if (hasOnlyOwnItems) {
      console.log('✅ CORRECT: First user can only see their own inventory');
    } else {
      console.log('❌ ERROR: First user can see other users\' inventory!');
    }
    console.log();

    // 8. Verify Second User Still Only Sees Their Own Inventory
    console.log('8. Verifying Second User Can Only See Their Own Inventory...');
    const getAll4Response = await axios.get(`${BASE_URL}/inventaris`, {
      headers: { Authorization: `Bearer ${authToken2}` }
    });
    console.log('✅ Second user inventory list:', getAll4Response.data.data.length, 'items');
    const hasOnlyOwnItems2 = getAll4Response.data.data.every(item => item.ID_User === userId2);
    if (hasOnlyOwnItems2) {
      console.log('✅ CORRECT: Second user can only see their own inventory');
    } else {
      console.log('❌ ERROR: Second user can see other users\' inventory!');
    }
    console.log();

    console.log('=== ALL TESTS COMPLETED ===');

  } catch (error) {
    console.error('❌ Test failed:', error.response?.data || error.message);
    console.error('Status:', error.response?.status);
  }
}

testAPI();
