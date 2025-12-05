const express = require('express');
const router = express.Router();
const peminjamanController = require('../controllers/peminjamanController');
const { body } = require('express-validator');
const authMiddleware = require('../middleware/authMiddleware');

// Validation rules for peminjaman creation
const peminjamanValidationRules = [
  body('Nama_Peminjam')
    .notEmpty()
    .withMessage('Nama peminjam is required'),
  body('NoHP_Peminjam')
    .notEmpty()
    .withMessage('Nomor HP peminjam is required'),
  body('Tanggal_Pinjam')
    .isISO8601()
    .withMessage('Tanggal pinjam must be a valid date'),
  body('Tanggal_Kembali')
    .isISO8601()
    .withMessage('Tanggal kembali must be a valid date'),
  body('ID_User')
    .isInt()
    .withMessage('ID_User must be a valid integer'),
  body('barangList')
    .isArray({ min: 1 })
    .withMessage('At least one item must be borrowed'),
  body('barangList.*.ID_Inventaris')
    .isInt()
    .withMessage('ID_Inventaris must be a valid integer'),
  body('barangList.*.Jumlah')
    .isInt({ min: 1 })
    .withMessage('Jumlah must be at least 1')
];

// Status update validation
const statusValidationRules = [
  body('Status')
    .isIn(['Menunggu', 'Dipinjam', 'Selesai', 'Terlambat', 'Ditolak'])
    .withMessage('Status must be one of: Menunggu, Dipinjam, Selesai, Terlambat, Ditolak')
];

// Routes
router.get('/', peminjamanController.getAllPeminjaman);
router.get('/stats', peminjamanController.getPeminjamanStats);
router.get('/:id', peminjamanController.getPeminjamanById);
router.post('/', authMiddleware, peminjamanValidationRules, peminjamanController.createPeminjaman);
router.patch('/:id/status', authMiddleware, statusValidationRules, peminjamanController.updatePeminjamanStatus);

module.exports = router;
