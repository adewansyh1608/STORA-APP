// Test script to check reminder API response format
const http = require('http');

// First login to get token
const loginData = JSON.stringify({
    email: 'admin@stora.com',
    password: 'admin123'
});

const loginOptions = {
    hostname: 'localhost',
    port: 3000,
    path: '/api/v1/auth/login',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': loginData.length
    }
};

console.log('Logging in...');

const loginReq = http.request(loginOptions, (res) => {
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => {
        try {
            const loginResponse = JSON.parse(data);
            if (loginResponse.token) {
                console.log('Login successful!');
                getReminders(loginResponse.token);
            } else {
                console.log('Login failed:', loginResponse);
            }
        } catch (e) {
            console.log('Parse error:', e);
        }
    });
});

loginReq.on('error', (e) => console.error('Login error:', e));
loginReq.write(loginData);
loginReq.end();

function getReminders(token) {
    console.log('\nGetting reminders...');

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
        res.on('data', (chunk) => data += chunk);
        res.on('end', () => {
            try {
                const response = JSON.parse(data);
                console.log('\n=== REMINDER API RESPONSE ===');
                console.log(JSON.stringify(response, null, 2));

                if (response.data && response.data.length > 0) {
                    console.log('\n=== CHECKING DATETIME FIELDS ===');
                    response.data.forEach((r, i) => {
                        console.log(`\nReminder ${i + 1}: ${r.title}`);
                        console.log(`  scheduled_datetime: ${r.scheduled_datetime} (type: ${typeof r.scheduled_datetime})`);
                        console.log(`  last_notified: ${r.last_notified} (type: ${typeof r.last_notified})`);
                        console.log(`  created_at: ${r.created_at}`);
                        console.log(`  reminder_type: ${r.reminder_type}`);
                    });
                }
            } catch (e) {
                console.log('Parse error:', e);
                console.log('Raw data:', data);
            }
        });
    });

    req.on('error', (e) => console.error('Request error:', e));
    req.end();
}
