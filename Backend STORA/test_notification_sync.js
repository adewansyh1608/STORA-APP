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

console.log('=== TESTING NOTIFICATION SYNC ENDPOINTS ===\n');

const loginReq = http.request(loginOptions, (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
        const loginResponse = JSON.parse(data);
        if (!loginResponse.success || !loginResponse.data?.token) {
            console.log('Login failed:', loginResponse.message || 'Unknown error');
            return;
        }

        const token = loginResponse.data.token;
        console.log('✓ Login success\n');

        // Test 1: Create notification history
        testCreateNotificationHistory(token);
    });
});

loginReq.on('error', (e) => console.error('Login error:', e.message));
loginReq.write(loginData);
loginReq.end();

function testCreateNotificationHistory(token) {
    console.log('📝 Test: POST /notifications/history');

    const notifData = JSON.stringify({
        Judul: "Test Offline Notification",
        Pesan: "This is a test notification created from local sync",
        Tanggal: new Date().toISOString().split('T')[0],
        Status: "sent"
    });

    const options = {
        hostname: 'localhost',
        port: 3000,
        path: '/api/v1/notifications/history',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'Content-Length': notifData.length
        }
    };

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
            console.log(`Status: ${res.statusCode}`);
            console.log('Response:', data);

            if (res.statusCode === 201) {
                console.log('\n✅ POST /notifications/history SUCCESS!');
            } else {
                console.log('\n❌ POST /notifications/history FAILED!');
            }

            // Test 2: Get notification history
            testGetNotificationHistory(token);
        });
    });

    req.on('error', (e) => console.error('Create notification error:', e.message));
    req.write(notifData);
    req.end();
}

function testGetNotificationHistory(token) {
    console.log('\n📝 Test: GET /notifications/history');

    const options = {
        hostname: 'localhost',
        port: 3000,
        path: '/api/v1/notifications/history',
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    };

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
            console.log(`Status: ${res.statusCode}`);
            const parsed = JSON.parse(data);
            console.log(`Notifications count: ${parsed.data?.length || 0}`);

            if (res.statusCode === 200) {
                console.log('\n✅ GET /notifications/history SUCCESS!');
            } else {
                console.log('\n❌ GET /notifications/history FAILED!');
            }

            // Test 3: Get reminders
            testGetReminders(token);
        });
    });

    req.on('error', (e) => console.error('Get notification error:', e.message));
    req.end();
}

function testGetReminders(token) {
    console.log('\n📝 Test: GET /notifications/reminders');

    const options = {
        hostname: 'localhost',
        port: 3000,
        path: '/api/v1/notifications/reminders',
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    };

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
            console.log(`Status: ${res.statusCode}`);
            const parsed = JSON.parse(data);
            console.log(`Reminders count: ${parsed.data?.length || 0}`);

            if (res.statusCode === 200) {
                console.log('\n✅ GET /notifications/reminders SUCCESS!');
            } else {
                console.log('\n❌ GET /notifications/reminders FAILED!');
            }

            console.log('\n=== ALL NOTIFICATION ENDPOINT TESTS COMPLETE ===');
        });
    });

    req.on('error', (e) => console.error('Get reminders error:', e.message));
    req.end();
}
