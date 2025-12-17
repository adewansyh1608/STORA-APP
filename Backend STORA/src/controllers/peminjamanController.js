const { Peminjaman, PeminjamanBarang, Inventaris, User, FotoPeminjaman, Notifikasi, sequelize } = require('../models');
const { validationResult } = require('express-validator');
const { Op } = require('sequelize');

class PeminjamanController {
  // Get all peminjaman
  async getAllPeminjaman(req, res) {
    try {
      console.log('===== GET ALL PEMINJAMAN REQUEST =====');
      console.log('Query params:', req.query);
      console.log('User from token:', req.user);

      const { page = 1, limit = 10, status = '', search = '' } = req.query;
      const offset = (page - 1) * limit;

      let whereClause = {};

      // Filter by user ID - each user only sees their own loans
      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
        console.log(`Filtering by user ID: ${req.user.id}`);
      } else {
        console.log('WARNING: No user ID found in request!');
      }

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
        order: [['ID_Peminjaman', 'DESC']],
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
              },
              {
                association: 'foto',
                attributes: ['ID_Foto_Peminjaman', 'Foto_Peminjaman', 'Foto_Pengembalian']
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
      console.error('✗ Error getting peminjaman:', error.message);
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

      // Build where clause including user ownership check
      const whereClause = {
        ID_Peminjaman: id
      };

      // Only allow access to user's own loans
      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      const peminjaman = await Peminjaman.findOne({
        where: whereClause,
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
              },
              {
                association: 'foto',
                attributes: ['ID_Foto_Peminjaman', 'Foto_Peminjaman', 'Foto_Pengembalian', 'Foto_Barang']
              }
            ]
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
          message: 'Peminjaman not found or you do not have permission to access it'
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

      // Create peminjaman barang entries and store created items for photo association
      let createdBarangItems = [];
      if (barangList && barangList.length > 0) {
        const peminjamanBarangData = barangList.map(item => ({
          ID_Peminjaman: newPeminjaman.ID_Peminjaman,
          ID_Inventaris: item.ID_Inventaris,
          Jumlah: item.Jumlah
        }));

        createdBarangItems = await PeminjamanBarang.bulkCreate(peminjamanBarangData, {
          transaction,
          returning: true
        });
      }

      // Handle photo upload if files are present - link to peminjaman_barang items
      if (req.files && req.files.length > 0 && createdBarangItems.length > 0) {
        const photoData = req.files.map((file, index) => ({
          ID_Peminjaman_Barang: createdBarangItems[index % createdBarangItems.length].ID_Peminjaman_Barang,
          Foto_Peminjaman: `/uploads/peminjaman/${file.filename}`,
          isSynced: true
        }));
        await FotoPeminjaman.bulkCreate(photoData, { transaction });
      } else if (req.file && createdBarangItems.length > 0) {
        // Single file upload - link to first item
        await FotoPeminjaman.create({
          ID_Peminjaman_Barang: createdBarangItems[0].ID_Peminjaman_Barang,
          Foto_Peminjaman: `/uploads/peminjaman/${req.file.filename}`,
          isSynced: true
        }, { transaction });
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
              },
              {
                association: 'foto',
                attributes: ['ID_Foto_Peminjaman', 'Foto_Peminjaman', 'Foto_Pengembalian']
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

  // Create new peminjaman with photos (multipart/form-data)
  async createPeminjamanWithPhotos(req, res) {
    const transaction = await sequelize.transaction();

    try {
      console.log('=== CREATE PEMINJAMAN WITH PHOTOS ===');
      console.log('Body:', req.body);
      console.log('Files:', req.files);

      // Parse body data (comes as form-data strings)
      const peminjamanData = {
        Nama_Peminjam: req.body.Nama_Peminjam,
        NoHP_Peminjam: req.body.NoHP_Peminjam,
        Tanggal_Pinjam: req.body.Tanggal_Pinjam,
        Tanggal_Kembali: req.body.Tanggal_Kembali,
        ID_User: parseInt(req.body.ID_User),
        Status: 'Dipinjam'
      };

      // Parse barangList from JSON string
      const barangList = req.body.barangList ? JSON.parse(req.body.barangList) : [];

      // Create peminjaman
      const newPeminjaman = await Peminjaman.create(peminjamanData, { transaction });

      // Create peminjaman barang entries and store created items
      let createdBarangItems = [];
      if (barangList && barangList.length > 0) {
        const peminjamanBarangData = barangList.map(item => ({
          ID_Peminjaman: newPeminjaman.ID_Peminjaman,
          ID_Inventaris: item.ID_Inventaris,
          Jumlah: item.Jumlah
        }));

        createdBarangItems = await PeminjamanBarang.bulkCreate(peminjamanBarangData, {
          transaction,
          returning: true
        });
      }

      // Handle photo uploads - link to peminjaman_barang items
      if (req.files && req.files.length > 0 && createdBarangItems.length > 0) {
        const photoData = req.files.map((file, index) => ({
          ID_Peminjaman_Barang: createdBarangItems[index % createdBarangItems.length].ID_Peminjaman_Barang,
          Foto_Peminjaman: `/uploads/peminjaman/${file.filename}`,
          isSynced: true
        }));
        await FotoPeminjaman.bulkCreate(photoData, { transaction });
        console.log(`✓ ${req.files.length} photos uploaded and linked to barang items`);
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
              },
              {
                association: 'foto',
                attributes: ['ID_Foto_Peminjaman', 'Foto_Peminjaman', 'Foto_Pengembalian']
              }
            ]
          }
        ]
      });

      console.log('✓ Peminjaman with photos created:', newPeminjaman.ID_Peminjaman);

      res.status(201).json({
        success: true,
        message: 'Peminjaman created successfully with photos',
        data: completePeminjaman
      });
    } catch (error) {
      await transaction.rollback();
      console.error('✗ Error creating peminjaman with photos:', error);
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
      const { Status, Tanggal_Dikembalikan } = req.body;

      // Build where clause including user ownership check
      const whereClause = {
        ID_Peminjaman: id
      };

      // Only allow update of user's own loans
      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      const updateData = { Status };
      if (Tanggal_Dikembalikan) {
        updateData.Tanggal_Dikembalikan = Tanggal_Dikembalikan;
      }

      const [updatedRowsCount] = await Peminjaman.update(
        updateData,
        { where: whereClause }
      );

      if (updatedRowsCount === 0) {
        return res.status(404).json({
          success: false,
          message: 'Peminjaman not found or you do not have permission to update it'
        });
      }

      const updatedPeminjaman = await Peminjaman.findOne({ where: whereClause });

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

  // Delete peminjaman (and all related data via CASCADE)
  async deletePeminjaman(req, res) {
    const transaction = await sequelize.transaction();

    try {
      const { id } = req.params;

      // Build where clause including user ownership check
      const whereClause = {
        ID_Peminjaman: id
      };

      // Only allow deletion of user's own loans
      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      const peminjaman = await Peminjaman.findOne({ where: whereClause });
      if (!peminjaman) {
        await transaction.rollback();
        return res.status(404).json({
          success: false,
          message: 'Peminjaman not found or you do not have permission to delete it'
        });
      }

      // Delete related peminjaman_barang first (CASCADE should handle this but being explicit)
      await PeminjamanBarang.destroy({
        where: { ID_Peminjaman: id },
        transaction
      });

      // Delete the peminjaman
      await peminjaman.destroy({ transaction });

      await transaction.commit();

      res.status(200).json({
        success: true,
        message: 'Peminjaman deleted successfully'
      });
    } catch (error) {
      await transaction.rollback();
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Upload return photos for peminjaman items
  async uploadReturnPhotos(req, res) {
    const transaction = await sequelize.transaction();

    try {
      const { id } = req.params;
      console.log('=== UPLOAD RETURN PHOTOS ===');
      console.log('Peminjaman ID:', id);
      console.log('Files:', req.files);
      console.log('Body:', req.body);

      const peminjaman = await Peminjaman.findByPk(id, {
        include: [{
          association: 'barang',
          include: [{
            association: 'foto'
          }]
        }]
      });

      if (!peminjaman) {
        await transaction.rollback();
        return res.status(404).json({
          success: false,
          message: 'Peminjaman not found'
        });
      }

      // Process uploaded files
      // Files are expected to have fieldname like 'photo_0', 'photo_1', etc.
      // Or via returnPhotos array with ID_Peminjaman_Barang mapping
      if (req.files && req.files.length > 0) {
        // Parse photoMapping from body if available
        const photoMapping = req.body.photoMapping ? JSON.parse(req.body.photoMapping) : null;

        for (let i = 0; i < req.files.length; i++) {
          const file = req.files[i];
          const photoPath = `/uploads/peminjaman/${file.filename}`;

          // Get the ID_Peminjaman_Barang from mapping
          let idPeminjamanBarang = null;
          if (photoMapping && photoMapping[i]) {
            idPeminjamanBarang = photoMapping[i].ID_Peminjaman_Barang;
          } else if (peminjaman.barang && peminjaman.barang[i]) {
            idPeminjamanBarang = peminjaman.barang[i].ID_Peminjaman_Barang;
          }

          if (idPeminjamanBarang) {
            // Check if foto_peminjaman entry exists for this item
            const existingFoto = await FotoPeminjaman.findOne({
              where: { ID_Peminjaman_Barang: idPeminjamanBarang },
              transaction
            });

            if (existingFoto) {
              // Update existing entry with return photo
              await existingFoto.update({
                Foto_Pengembalian: photoPath
              }, { transaction });
            } else {
              // Create new entry with return photo
              await FotoPeminjaman.create({
                ID_Peminjaman_Barang: idPeminjamanBarang,
                Foto_Pengembalian: photoPath,
                isSynced: true
              }, { transaction });
            }
          }
        }
      }

      await transaction.commit();

      // Fetch updated peminjaman
      const updatedPeminjaman = await Peminjaman.findByPk(id, {
        include: [{
          association: 'barang',
          include: [{
            association: 'inventaris',
            attributes: ['Nama_Barang', 'Kode_Barang']
          }, {
            association: 'foto',
            attributes: ['ID_Foto_Peminjaman', 'Foto_Peminjaman', 'Foto_Pengembalian']
          }]
        }]
      });

      console.log('✓ Return photos uploaded successfully');

      res.status(200).json({
        success: true,
        message: 'Return photos uploaded successfully',
        data: updatedPeminjaman
      });
    } catch (error) {
      await transaction.rollback();
      console.error('✗ Error uploading return photos:', error);
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }
}

module.exports = new PeminjamanController();
