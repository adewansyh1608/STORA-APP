const express = require('express');
const router = express.Router();
const inventarisController = require('../controllers/inventarisController');
const { body } = require('express-validator');
const authMiddleware = require('../middleware/authMiddleware');
const upload = require('../middleware/upload');

// Validation rules for inventaris creation
const inventarisValidationRules = [
  body('Nama_Barang').notEmpty().withMessage('Nama barang is required'),
  body('Kode_Barang').notEmpty().withMessage('Kode barang is required'),
  body('Jumlah')
    .isInt({ min: 0 })
    .withMessage('Jumlah must be a positive integer'),
  body('Kategori').notEmpty().withMessage('Kategori is required'),
  body('Kondisi')
    .isIn(['Baik', 'Rusak Ringan', 'Rusak Berat'])
    .withMessage('Kondisi must be one of: Baik, Rusak Ringan, Rusak Berat'),
];

// Routes
router.get('/', authMiddleware, inventarisController.getAllInventaris);
router.get('/stats', authMiddleware, inventarisController.getInventarisStats);
router.get('/:id', authMiddleware, inventarisController.getInventarisById);
router.post(
  '/',
  authMiddleware,
  upload.single('foto'),  // Handle single file upload with field name 'foto'
  inventarisValidationRules,
  inventarisController.createInventaris
);
router.put('/:id', authMiddleware, upload.single('foto'), inventarisController.updateInventaris);
router.delete('/:id', authMiddleware, inventarisController.deleteInventaris);

module.exports = router;
