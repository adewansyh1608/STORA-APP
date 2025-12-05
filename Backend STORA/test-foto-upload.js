const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

const BASE_URL = 'http://localhost:3000/api/v1';

async function testFotoUpload() {
  console.log('=== TEST UPLOAD FOTO INVENTARIS ===\n');

  try {
    // 1. Signup user
    console.log('1. Creating test user...');
    const signupData = {
      name: 'Test Foto User',
      email: `testfoto_${Date.now()}@test.com`,
      password: 'password123',
      password_confirmation: 'password123'
    };
    
    const signupResponse = await axios.post(`${BASE_URL}/signup`, signupData);
    const authToken = signupResponse.data.token;
    const userId = signupResponse.data.data.id;
    console.log('✅ User created, ID:', userId);
    console.log();

    // 2. Create test image file
    console.log('2. Creating test image...');
    const testImagePath = path.join(__dirname, 'test-image.jpg');
    
    // Create a simple test image if it doesn't exist
    if (!fs.existsSync(testImagePath)) {
      // Create a minimal JPG file (1x1 pixel red image)
      const jpegBuffer = Buffer.from([
        0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
        0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0xFF, 0xDB, 0x00, 0x43,
        0x00, 0x08, 0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07, 0x07, 0x09,
        0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12,
        0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20,
        0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29,
        0x2C, 0x30, 0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32,
        0x3C, 0x2E, 0x33, 0x34, 0x32, 0xFF, 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01,
        0x00, 0x01, 0x01, 0x01, 0x11, 0x00, 0xFF, 0xC4, 0x00, 0x14, 0x00, 0x01,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x03, 0xFF, 0xC4, 0x00, 0x14, 0x10, 0x01, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0xFF, 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3F, 0x00,
        0x37, 0xFF, 0xD9
      ]);
      fs.writeFileSync(testImagePath, jpegBuffer);
    }
    console.log('✅ Test image created');
    console.log();

    // 3. Create inventory dengan foto menggunakan multipart
    console.log('3. Creating inventory with photo (multipart)...');
    const formData = new FormData();
    formData.append('Nama_Barang', 'Laptop dengan Foto');
    formData.append('Kode_Barang', 'LPF001');
    formData.append('Jumlah', '2');
    formData.append('Kategori', 'Elektronik');
    formData.append('Lokasi', 'Lab IT');
    formData.append('Kondisi', 'Baik');
    formData.append('Tanggal_Pengadaan', '2025-12-05');
    formData.append('Deskripsi', 'Laptop gaming dengan foto produk lengkap');
    formData.append('foto', fs.createReadStream(testImagePath));

    const createResponse = await axios.post(`${BASE_URL}/inventaris`, formData, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        ...formData.getHeaders()
      }
    });

    console.log('✅ Inventory created');
    const inventoryId = createResponse.data.data.ID_Inventaris;
    const fotoData = createResponse.data.data.foto;
    console.log('Inventory ID:', inventoryId);
    console.log('Deskripsi:', createResponse.data.data.Deskripsi);
    console.log('Foto data:', fotoData);
    console.log();

    // 4. Verify di database
    console.log('4. Verifying in database...');
    const connection = await mysql.createConnection({
      host: 'localhost',
      user: 'root',
      password: '',
      database: 'stora_db'
    });

    // Check inventaris table
    const [inventaris] = await connection.query(
      'SELECT * FROM inventaris WHERE ID_Inventaris = ?',
      [inventoryId]
    );
    console.log('✅ Inventaris in database:');
    console.log('  - Nama:', inventaris[0].Nama_Barang);
    console.log('  - Deskripsi:', inventaris[0].Deskripsi);
    console.log();

    // Check foto_inventaris table
    const [fotos] = await connection.query(
      'SELECT * FROM foto_inventaris WHERE ID_Inventaris = ?',
      [inventoryId]
    );
    
    if (fotos.length > 0) {
      console.log('✅ SUCCESS: Foto tersimpan di database!');
      console.log('  - ID Foto:', fotos[0].ID_Foto_Inventaris);
      console.log('  - Path Foto:', fotos[0].Foto);
      console.log('  - isSynced:', fotos[0].isSynced);
      
      // Check if file exists
      const fotoPath = path.join(__dirname, 'public', fotos[0].Foto);
      if (fs.existsSync(fotoPath)) {
        const stats = fs.statSync(fotoPath);
        console.log('  - File exists: YES');
        console.log('  - File size:', stats.size, 'bytes');
      } else {
        console.log('  - File exists: NO (Path:', fotoPath, ')');
      }
    } else {
      console.log('❌ ERROR: Foto TIDAK tersimpan di database!');
    }
    console.log();

    await connection.end();

    // 5. Get inventory to verify foto returned in API
    console.log('5. Getting inventory via API...');
    const getResponse = await axios.get(`${BASE_URL}/inventaris/${inventoryId}`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    
    const inventory = getResponse.data.data;
    console.log('✅ API Response:');
    console.log('  - Nama:', inventory.Nama_Barang);
    console.log('  - Deskripsi:', inventory.Deskripsi);
    console.log('  - Foto count:', inventory.foto?.length || 0);
    if (inventory.foto && inventory.foto.length > 0) {
      console.log('  - Foto URL:', inventory.foto[0].Foto);
    }
    console.log();

    console.log('=== TEST SELESAI ===');
    if (fotos.length > 0) {
      console.log('✅✅✅ SEMUA TEST PASSED! Foto berhasil tersimpan! ✅✅✅');
    } else {
      console.log('❌❌❌ TEST FAILED! Foto tidak tersimpan! ❌❌❌');
    }

  } catch (error) {
    console.error('❌ Test failed:', error.response?.data || error.message);
    console.error('Status:', error.response?.status);
    if (error.response?.data) {
      console.error('Error detail:', JSON.stringify(error.response.data, null, 2));
    }
  }
}

testFotoUpload();
