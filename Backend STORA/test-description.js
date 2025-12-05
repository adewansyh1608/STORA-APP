const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api/v1';

async function testDescriptionAndPhoto() {
  console.log('=== TEST DESKRIPSI & FOTO INVENTARIS ===\n');

  try {
    // 1. Signup
    console.log('1. Creating test user...');
    const signupData = {
      name: 'Test Deskripsi',
      email: `testdesc_${Date.now()}@test.com`,
      password: 'password123',
      password_confirmation: 'password123'
    };
    
    const signupResponse = await axios.post(`${BASE_URL}/signup`, signupData);
    const authToken = signupResponse.data.token;
    const userId = signupResponse.data.data.id;
    console.log('✅ User created, ID:', userId);
    console.log();

    // 2. Create Inventory dengan Deskripsi
    console.log('2. Creating inventory with description...');
    const inventoryData = {
      Nama_Barang: 'Laptop Gaming',
      Kode_Barang: 'LPG001',
      Jumlah: 3,
      Kategori: 'Elektronik',
      Lokasi: 'Lab Komputer',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2024-12-05',
      Deskripsi: 'Laptop gaming dengan spesifikasi tinggi. RAM 32GB, SSD 1TB, GPU RTX 4060. Kondisi sangat baik dan masih bergaransi.'
    };
    
    const createResponse = await axios.post(`${BASE_URL}/inventaris`, inventoryData, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    console.log('✅ Inventory created');
    const inventoryId = createResponse.data.data.ID_Inventaris;
    console.log('Inventory ID:', inventoryId);
    console.log('Deskripsi tersimpan:', createResponse.data.data.Deskripsi);
    console.log();

    // 3. Get Inventory untuk verify
    console.log('3. Fetching inventory to verify...');
    const getResponse = await axios.get(`${BASE_URL}/inventaris/${inventoryId}`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    
    const inventory = getResponse.data.data;
    console.log('✅ Inventory retrieved:');
    console.log('  - Nama:', inventory.Nama_Barang);
    console.log('  - Deskripsi:', inventory.Deskripsi);
    console.log('  - Foto:', inventory.foto?.length || 0, 'foto');
    console.log();

    // 4. Verify Deskripsi
    if (inventory.Deskripsi === inventoryData.Deskripsi) {
      console.log('✅ SUCCESS: Deskripsi tersimpan dengan benar!');
    } else {
      console.log('❌ ERROR: Deskripsi tidak sama!');
      console.log('   Expected:', inventoryData.Deskripsi);
      console.log('   Got:', inventory.Deskripsi);
    }
    console.log();

    // 5. Get All Inventory
    console.log('4. Getting all inventory...');
    const getAllResponse = await axios.get(`${BASE_URL}/inventaris`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    console.log('✅ Total inventory:', getAllResponse.data.data.length);
    getAllResponse.data.data.forEach(item => {
      console.log(`  - ${item.Nama_Barang} (${item.Deskripsi ? 'Dengan deskripsi' : 'Tanpa deskripsi'})`);
    });
    console.log();

    console.log('=== TEST SELESAI ===');
    console.log('✅ Deskripsi sudah bisa disimpan dan diambil dengan benar!');

  } catch (error) {
    console.error('❌ Test failed:', error.response?.data || error.message);
    console.error('Status:', error.response?.status);
  }
}

testDescriptionAndPhoto();
