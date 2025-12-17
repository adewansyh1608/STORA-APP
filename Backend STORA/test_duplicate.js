const http = require('http');

// Login first
const loginData = JSON.stringify({
    email: "d.andreanms@gmail.com",
    password: "Andrean29"
});

const loginOptions = {
    hostname: 'localhost',
    port: 3000,
    path: '/api/auth/login',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': loginData.length
    }
};

const loginReq = http.request(loginOptions, (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
        const loginResponse = JSON.parse(data);
        if (!loginResponse.success || !loginResponse.data?.token) {
            console.log('Login failed:', loginResponse);
            return;
        }

        const token = loginResponse.data.token;
        console.log('✓ Login success, token:', token.substring(0, 30) + '...');

        // Now test duplicate inventory
        testDuplicateInventory(token);
    });
});

loginReq.on('error', (e) => console.error('Login error:', e.message));
loginReq.write(loginData);
loginReq.end();

function testDuplicateInventory(token) {
    // Try to create inventory with a code that might already exist
    const invData = JSON.stringify({
        Nama_Barang: "Test Duplikat",
        Kode_Barang: "HMSI/XX/2025/01",  // This should match existing items
        Jumlah: 1,
        Kategori: "TestKategori",
        Lokasi: "TestLokasi",
        Kondisi: "Baik",
        Tanggal_Pengadaan: "2025-01-01",
        Deskripsi: "Test deskripsi"
    });

    const options = {
        hostname: 'localhost',
        port: 3000,
        path: '/api/inventaris',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'Content-Length': invData.length
        }
    };

    console.log('\n📤 Testing duplicate with Kode_Barang: "HMSI/XX/2025/01"');

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
            console.log('Status:', res.statusCode);
            console.log('Response:', data);

            if (res.statusCode === 409 || (res.statusCode === 400 && data.includes('DUPLICATE'))) {
                console.log('\n✅ DUPLICATE DETECTION WORKING! Server correctly rejected duplicate.');
            } else if (res.statusCode === 201 || res.statusCode === 200) {
                console.log('\n⚠️ Item was created - may not have existing duplicate in database');
            } else {
                console.log('\n❌ Unexpected response');
            }
        });
    });

    req.on('error', (e) => console.error('Create error:', e.message));
    req.write(invData);
    req.end();
}
