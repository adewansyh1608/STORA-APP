const express = require('express');
const router = express.Router();

// Import route modules
const authRoutes = require('./authRoutes');
const inventarisRoutes = require('./inventarisRoutes');

// Health check endpoint
router.get('/health', (req, res) => {
  res.json({
    success: true,
    message: 'STORA API is running',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
  });
});

// Mount auth routes
router.use('/', authRoutes);

// Mount inventaris routes
router.use('/inventaris', inventarisRoutes);

module.exports = router;
