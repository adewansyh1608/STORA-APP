const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

let firebaseInitialized = false;

const initializeFirebase = () => {
    if (firebaseInitialized) {
        return admin;
    }

    const serviceAccountPath = path.join(__dirname, '../../config/firebase-service-account.json');

    if (!fs.existsSync(serviceAccountPath)) {
        console.warn('⚠️ Firebase service account not found. Push notifications will be disabled.');
        console.warn(`Expected path: ${serviceAccountPath}`);
        return null;
    }

    try {
        const serviceAccount = require(serviceAccountPath);

        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        });

        firebaseInitialized = true;
        console.log('✓ Firebase Admin SDK initialized successfully');
        return admin;
    } catch (error) {
        console.error('✗ Failed to initialize Firebase Admin SDK:', error.message);
        return null;
    }
};

const sendPushNotification = async (fcmToken, title, body, data = {}) => {
    if (!firebaseInitialized) {
        console.warn('Firebase not initialized, skipping notification');
        return { success: false, error: 'Firebase not initialized' };
    }

    try {
        const message = {
            notification: {
                title,
                body,
            },
            data: {
                ...data,
                click_action: 'FLUTTER_NOTIFICATION_CLICK',
            },
            token: fcmToken,
            android: {
                priority: 'high',
                notification: {
                    channelId: 'inventory_reminders',
                    priority: 'high',
                    defaultSound: true,
                    defaultVibrateTimings: true,
                },
            },
        };

        const response = await admin.messaging().send(message);
        console.log('✓ Notification sent successfully:', response);
        return { success: true, messageId: response };
    } catch (error) {
        console.error('✗ Error sending notification:', error.message);
        return { success: false, error: error.message };
    }
};

const sendMultipleNotifications = async (tokens, title, body, data = {}) => {
    if (!firebaseInitialized) {
        console.warn('Firebase not initialized, skipping notifications');
        return { success: false, error: 'Firebase not initialized' };
    }

    if (!tokens || tokens.length === 0) {
        return { success: true, successCount: 0, failureCount: 0 };
    }

    try {
        const message = {
            notification: {
                title,
                body,
            },
            data: {
                ...data,
                click_action: 'FLUTTER_NOTIFICATION_CLICK',
            },
            tokens: tokens,
            android: {
                priority: 'high',
                notification: {
                    channelId: 'inventory_reminders',
                    priority: 'high',
                    defaultSound: true,
                    defaultVibrateTimings: true,
                },
            },
        };

        const response = await admin.messaging().sendEachForMulticast(message);
        console.log(`✓ Notifications sent: ${response.successCount} success, ${response.failureCount} failed`);
        return {
            success: true,
            successCount: response.successCount,
            failureCount: response.failureCount,
            responses: response.responses,
        };
    } catch (error) {
        console.error('✗ Error sending multiple notifications:', error.message);
        return { success: false, error: error.message };
    }
};

module.exports = {
    initializeFirebase,
    sendPushNotification,
    sendMultipleNotifications,
    getAdmin: () => (firebaseInitialized ? admin : null),
};
