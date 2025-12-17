const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api/v1';

async function runTest() {
    try {
        console.log('1. Registering/Logging in User...');
        const email = `test_del_${Date.now()}@test.com`;
        const password = 'password123';

        // Auto-register
        try {
            await axios.post(`${BASE_URL}/signup`, {
                name: 'Test Delete User',
                email: email,
                password: password,
                password_confirmation: password
            });
            console.log('   Registered new user.');
        } catch (e) {
            if (e.response && e.response.status === 400 && e.response.data.message.includes('already exists')) {
                console.log('   User already exists, proceeding to login.');
            } else {
                console.log('   Register warning:', e.message, e.response?.data);
            }
        }

        // Login
        const loginRes = await axios.post(`${BASE_URL}/login`, {
            email: email,
            password: password
        });
        const token = loginRes.data.token;
        const userId = loginRes.data.data.ID_User || loginRes.data.data.id;
        console.log(`   Logged in. UserID: ${userId}`);

        console.log('2. Getting Inventory Item...');
        let invRes = await axios.get(`${BASE_URL}/inventaris`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        let items = invRes.data.data;
        if (items.length === 0) {
            console.log('   No inventory items found. Creating test item...');
            try {
                await axios.post(`${BASE_URL}/inventaris`, {
                    Nama_Barang: 'Test Item',
                    Kode_Barang: `TEST-${Date.now()}`,
                    Jumlah: 10,
                    Kategori: 'Elektronik',
                    Kondisi: 'Baik'
                }, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                console.log('   Created test item. Fetching again...');
                invRes = await axios.get(`${BASE_URL}/inventaris`, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                items = invRes.data.data;
            } catch (e) {
                console.error('   Failed to create inventory item:', e.message, e.response?.data);
                return;
            }
        }

        if (items.length === 0) {
            console.error('   Still no inventory items. Cannot proceed.');
            return;
        }

        const item = items[0];
        console.log(`   Using Item: ${item.Nama_Barang} (ID: ${item.ID_Inventaris})`);

        console.log('3. Creating Loan...');
        const today = new Date().toISOString().split('T')[0];
        const loanPayload = {
            Nama_Peminjam: 'Test Delete',
            NoHP_Peminjam: '08123456789',
            Tanggal_Pinjam: today,
            Tanggal_Kembali: today,
            ID_User: userId,
            barangList: [{
                ID_Inventaris: item.ID_Inventaris,
                Jumlah: 1
            }]
        };

        const createRes = await axios.post(`${BASE_URL}/peminjaman`, loanPayload, {
            headers: { Authorization: `Bearer ${token}` }
        });
        const loanId = createRes.data.data.ID_Peminjaman;
        console.log(`   Loan Created. ID: ${loanId}`);

        console.log('4. Verifying Loan Exists...');
        const getRes = await axios.get(`${BASE_URL}/peminjaman/${loanId}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (getRes.status === 200) console.log(`   Loan ${loanId} found in backend.`);

        console.log('5. Deleting Loan...');
        const deleteRes = await axios.delete(`${BASE_URL}/peminjaman/${loanId}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        console.log(`   Delete Response: ${deleteRes.status} ${deleteRes.data.message}`);

        console.log('6. Verifying Loan Deleted...');
        try {
            await axios.get(`${BASE_URL}/peminjaman/${loanId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            console.error('   FAILED: Loan still exists!');
        } catch (e) {
            if (e.response && e.response.status === 404) {
                console.log('   SUCCESS: Loan not found (404) as expected.');
            } else {
                console.error(`   Unexpected error: ${e.message}`);
            }
        }

    } catch (err) {
        if (err.code === 'ECONNREFUSED') {
            console.error('FAILED: Connection refused. Is the server running on port 3000?');
        } else {
            console.error('TEST FAILED:', err.response ? JSON.stringify(err.response.data) : err.message);
        }
    }
}

runTest();
