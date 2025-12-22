const express = require('express');
const router = express.Router();

const authRoutes = require('./authRoutes');
const inventarisRoutes = require('./inventarisRoutes');
const peminjamanRoutes = require('./peminjamanRoutes');
const notificationRoutes = require('./notificationRoutes');

router.get('/health', (req, res) => {
  res.json({
    success: true,
    message: 'STORA API is running',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
  });
});

router.use('/', authRoutes);

router.use('/inventaris', inventarisRoutes);

router.use('/peminjaman', peminjamanRoutes);

router.use('/notifications', notificationRoutes);

module.exports = router;
