const { Peminjaman, PeminjamanBarang, Inventaris, User, FotoPeminjaman, Notifikasi, sequelize } = require('../models');
const { validationResult } = require('express-validator');
const { Op } = require('sequelize');

class PeminjamanController {
  // Get all peminjaman
  async getAllPeminjaman(req, res) {
    try {
      const { page = 1, limit = 10, status = '', search = '' } = req.query;
      const offset = (page - 1) * limit;

      let whereClause = {};
      if (status) {
        whereClause.Status = status;
      }
      if (search) {
        whereClause[Op.or] = [
          { Nama_Peminjam: { [Op.like]: `%${search}%` } },
          { NoHP_Peminjam: { [Op.like]: `%${search}%` } }
        ];
      }

      const { count, rows } = await Peminjaman.findAndCountAll({
        where: whereClause,
        limit: parseInt(limit),
        offset: parseInt(offset),
        order: [['createdAt', 'DESC']],
        include: [
          {
            association: 'user',
            attributes: ['ID_User', 'Nama_User']
          },
          {
            association: 'barang',
            include: [
              {
                association: 'inventaris',
                attributes: ['Nama_Barang', 'Kode_Barang']
              }
            ]
          }
        ]
      });

      res.status(200).json({
        success: true,
        data: rows,
        pagination: {
          currentPage: parseInt(page),
          totalPages: Math.ceil(count / limit),
          totalItems: count,
          hasNext: page < Math.ceil(count / limit),
          hasPrev: page > 1
        }
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Get peminjaman by ID
  async getPeminjamanById(req, res) {
    try {
      const { id } = req.params;
      const peminjaman = await Peminjaman.findByPk(id, {
        include: [
          {
            association: 'user',
            attributes: ['ID_User', 'Nama_User', 'Email']
          },
          {
            association: 'barang',
            include: [
              {
                association: 'inventaris',
                attributes: ['ID_Inventaris', 'Nama_Barang', 'Kode_Barang', 'Kondisi']
              }
            ]
          },
          {
            association: 'foto',
            attributes: ['ID_Foto_Peminjaman', 'Foto_Peminjaman', 'Foto_Pengembalian', 'Foto_Barang']
          },
          {
            association: 'notifikasi',
            attributes: ['ID_Notifikasi', 'Pesan', 'Tanggal', 'Status']
          }
        ]
      });
      
      if (!peminjaman) {
        return res.status(404).json({
          success: false,
          message: 'Peminjaman not found'
        });
      }

      res.status(200).json({
        success: true,
        data: peminjaman
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Create new peminjaman
  async createPeminjaman(req, res) {
    const transaction = await sequelize.transaction();
    
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        await transaction.rollback();
        return res.status(400).json({
          success: false,
          message: 'Validation errors',
          errors: errors.array()
        });
      }

      const { barangList, ...peminjamanData } = req.body;
      
      // Create peminjaman
      const newPeminjaman = await Peminjaman.create(peminjamanData, { transaction });
      
      // Create peminjaman barang entries
      if (barangList && barangList.length > 0) {
        const peminjamanBarangData = barangList.map(item => ({
          ID_Peminjaman: newPeminjaman.ID_Peminjaman,
          ID_Inventaris: item.ID_Inventaris,
          Jumlah: item.Jumlah
        }));
        
        await PeminjamanBarang.bulkCreate(peminjamanBarangData, { transaction });
      }
      
      await transaction.commit();
      
      // Fetch the complete peminjaman data
      const completePeminjaman = await Peminjaman.findByPk(newPeminjaman.ID_Peminjaman, {
        include: [
          {
            association: 'barang',
            include: [
              {
                association: 'inventaris',
                attributes: ['Nama_Barang', 'Kode_Barang']
              }
            ]
          }
        ]
      });
      
      res.status(201).json({
        success: true,
        message: 'Peminjaman created successfully',
        data: completePeminjaman
      });
    } catch (error) {
      await transaction.rollback();
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Update peminjaman status
  async updatePeminjamanStatus(req, res) {
    try {
      const { id } = req.params;
      const { Status } = req.body;
      
      const [updatedRowsCount] = await Peminjaman.update(
        { Status },
        { where: { ID_Peminjaman: id } }
      );
      
      if (updatedRowsCount === 0) {
        return res.status(404).json({
          success: false,
          message: 'Peminjaman not found'
        });
      }

      const updatedPeminjaman = await Peminjaman.findByPk(id);

      res.status(200).json({
        success: true,
        message: 'Peminjaman status updated successfully',
        data: updatedPeminjaman
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Get peminjaman statistics
  async getPeminjamanStats(req, res) {
    try {
      const totalPeminjaman = await Peminjaman.count();
      const peminjamanByStatus = await Peminjaman.findAll({
        attributes: [
          'Status',
          [sequelize.fn('COUNT', sequelize.col('ID_Peminjaman')), 'count']
        ],
        group: ['Status']
      });
      
      const overdueCount = await Peminjaman.count({
        where: {
          Status: 'Dipinjam',
          Tanggal_Kembali: {
            [Op.lt]: new Date()
          }
        }
      });

      res.status(200).json({
        success: true,
        data: {
          totalPeminjaman,
          peminjamanByStatus,
          overdueCount
        }
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }
}

module.exports = new PeminjamanController();
