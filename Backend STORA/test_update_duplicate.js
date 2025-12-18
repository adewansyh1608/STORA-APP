const http = require('http');

// Login first
const loginData = JSON.stringify({
    email: "andre@mail.com",
    password: "andre"
});

const loginOptions = {
    hostname: 'localhost',
    port: 3000,
    path: '/api/v1/login',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': loginData.length
    }
};

console.log('=== TESTING EDIT DUPLICATE DETECTION ===\n');

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
        console.log('✓ Login success\n');

        // First, get existing inventory
        getExistingInventory(token);
    });
});

loginReq.on('error', (e) => console.error('Login error:', e.message));
loginReq.write(loginData);
loginReq.end();

function getExistingInventory(token) {
    const options = {
        hostname: 'localhost',
        port: 3000,
        path: '/api/v1/inventaris?page=1&limit=10',
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    };

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
            const result = JSON.parse(data);
            if (result.success && result.data && result.data.length > 0) {
                const firstItem = result.data[0];
                console.log(`Found existing item: ID=${firstItem.ID_Inventaris}, Kode="${firstItem.Kode_Barang}"`);

                if (result.data.length > 1) {
                    const secondItem = result.data[1];
                    console.log(`Second item: ID=${secondItem.ID_Inventaris}, Kode="${secondItem.Kode_Barang}"`);

                    // Try to update first item with second item's Kode_Barang - should fail
                    testDuplicateUpdate(token, firstItem.ID_Inventaris, secondItem.Kode_Barang);
                } else {
                    // Only one item, try to create a second one first
                    console.log('\nNeed at least 2 items to test. Creating a test item first...');
                    createTestItem(token, firstItem);
                }
            } else {
                console.log('No inventory items found');
            }
        });
    });

    req.on('error', (e) => console.error('Get inventory error:', e.message));
    req.end();
}

function createTestItem(token, existingItem) {
    const testData = JSON.stringify({
        Nama_Barang: "Test Item for Duplicate Check",
        Kode_Barang: "TEST/DUPCHECK/001",
        Jumlah: 1,
        Kategori: "Test",
        Lokasi: "Test",
        Kondisi: "Baik",
        Tanggal_Pengadaan: "2025-01-01",
        Deskripsi: "Test item"
    });

    const options = {
        hostname: 'localhost',
        port: 3000,
        path: '/api/v1/inventaris',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'Content-Length': testData.length
        }
    };

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
            const result = JSON.parse(data);
            if (result.success) {
                console.log(`✓ Created test item with ID: ${result.data?.ID_Inventaris}`);
                testDuplicateUpdate(token, result.data.ID_Inventaris, existingItem.Kode_Barang);
            } else {
                console.log('Failed to create test item:', result.message);
            }
        });
    });

    req.on('error', (e) => console.error('Create error:', e.message));
    req.write(testData);
    req.end();
}

function testDuplicateUpdate(token, itemId, duplicateKode) {
    console.log(`\n📝 Testing: Update item ${itemId} with duplicate Kode_Barang: "${duplicateKode}"`);

    const updateData = JSON.stringify({
        Kode_Barang: duplicateKode
    });

    const options = {
        hostname: 'localhost',
        port: 3000,
        path: `/api/v1/inventaris/${itemId}`,
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'Content-Length': updateData.length
        }
    };

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
            console.log(`\nStatus: ${res.statusCode}`);
            console.log('Response:', data);

            if (res.statusCode === 409) {
                console.log('\n✅ SUCCESS! Server correctly rejected duplicate Kode_Barang on update!');
            } else if (res.statusCode === 200) {
                console.log('\n❌ FAILED! Server allowed duplicate Kode_Barang - this is a bug!');
            } else {
                console.log('\n⚠️ Unexpected response');
            }
        });
    });

    req.on('error', (e) => console.error('Update error:', e.message));
    req.write(updateData);
    req.end();
}
