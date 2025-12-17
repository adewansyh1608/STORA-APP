const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api/v1';

async function runInventorySyncTest() {
    console.log('============================================');
    console.log('      INVENTORY SYNC TEST SUITE');
    console.log('============================================\n');

    try {
        // 1. Register/Login User
        console.log('1. Registering/Logging in User...');
        const email = `test_sync_${Date.now()}@test.com`;
        const password = 'password123';

        try {
            await axios.post(`${BASE_URL}/signup`, {
                name: 'Test Sync User',
                email: email,
                password: password,
                password_confirmation: password
            });
            console.log('   ✓ Registered new user.');
        } catch (e) {
            if (e.response && e.response.status === 400) {
                console.log('   User already exists, proceeding to login.');
            } else {
                throw e;
            }
        }

        const loginRes = await axios.post(`${BASE_URL}/login`, {
            email: email,
            password: password
        });
        const token = loginRes.data.token;
        const userId = loginRes.data.data.ID_User || loginRes.data.data.id;
        console.log(`   ✓ Logged in. UserID: ${userId}\n`);

        const authHeader = { Authorization: `Bearer ${token}` };

        // 2. Test CREATE - Create inventory item (online scenario)
        console.log('2. Testing CREATE (Online Scenario)...');
        const testItem = {
            Nama_Barang: `Test Item ${Date.now()}`,
            Kode_Barang: `SYNC-${Date.now()}`,
            Jumlah: 10,
            Kategori: 'Elektronik',
            Kondisi: 'Baik',
            Lokasi: 'Gudang A',
            Tanggal_Pengadaan: '2024-01-01',
            Deskripsi: 'Test item for sync verification'
        };

        const createRes = await axios.post(`${BASE_URL}/inventaris`, testItem, {
            headers: authHeader
        });

        if (createRes.data.success) {
            const createdItem = createRes.data.data;
            console.log(`   ✓ Item created successfully!`);
            console.log(`     - ID: ${createdItem.ID_Inventaris}`);
            console.log(`     - Name: ${createdItem.Nama_Barang}`);
            console.log(`     - Code: ${createdItem.Kode_Barang}\n`);

            // 3. Test READ - Get all items
            console.log('3. Testing READ (Get All Items)...');
            const getAllRes = await axios.get(`${BASE_URL}/inventaris`, {
                headers: authHeader
            });

            if (getAllRes.data.success) {
                console.log(`   ✓ Found ${getAllRes.data.data.length} items`);
                console.log(`   ✓ Pagination: ${JSON.stringify(getAllRes.data.pagination)}\n`);
            }

            // 4. Test SYNC Endpoint
            console.log('4. Testing SYNC Endpoint...');
            const syncRes = await axios.get(`${BASE_URL}/inventaris/sync`, {
                headers: authHeader
            });

            if (syncRes.data.success) {
                console.log(`   ✓ Sync endpoint working!`);
                console.log(`     - Items: ${syncRes.data.data.length}`);
                console.log(`     - Sync timestamp: ${syncRes.data.syncTimestamp}\n`);
            }

            // 5. Test UPDATE
            console.log('5. Testing UPDATE...');
            const updateRes = await axios.put(`${BASE_URL}/inventaris/${createdItem.ID_Inventaris}`, {
                Nama_Barang: `Updated ${testItem.Nama_Barang}`,
                Jumlah: 15,
                Kondisi: 'Rusak Ringan'
            }, {
                headers: authHeader
            });

            if (updateRes.data.success) {
                console.log(`   ✓ Item updated successfully!`);
                console.log(`     - New Name: ${updateRes.data.data.Nama_Barang}`);
                console.log(`     - New Quantity: ${updateRes.data.data.Jumlah}`);
                console.log(`     - New Condition: ${updateRes.data.data.Kondisi}\n`);
            }

            // 6. Test DELETE
            console.log('6. Testing DELETE...');
            const deleteRes = await axios.delete(`${BASE_URL}/inventaris/${createdItem.ID_Inventaris}`, {
                headers: authHeader
            });

            if (deleteRes.data.success) {
                console.log(`   ✓ Item deleted successfully!\n`);
            }

            // 7. Verify deletion
            console.log('7. Verifying Deletion...');
            try {
                await axios.get(`${BASE_URL}/inventaris/${createdItem.ID_Inventaris}`, {
                    headers: authHeader
                });
                console.log('   ✗ FAILED: Item still exists!\n');
            } catch (e) {
                if (e.response && e.response.status === 404) {
                    console.log('   ✓ Item not found (404) as expected.\n');
                }
            }

        } else {
            console.log('   ✗ Create failed:', createRes.data.message);
        }

        console.log('============================================');
        console.log('     ALL TESTS COMPLETED SUCCESSFULLY!');
        console.log('============================================');

    } catch (err) {
        if (err.code === 'ECONNREFUSED') {
            console.error('\n✗ FAILED: Connection refused.');
            console.error('  Make sure the server is running on port 3000.');
            console.error('  Run: npm run dev:3000');
        } else {
            console.error('\nTEST FAILED:', err.response ? JSON.stringify(err.response.data, null, 2) : err.message);
        }
        process.exit(1);
    }
}

runInventorySyncTest();
