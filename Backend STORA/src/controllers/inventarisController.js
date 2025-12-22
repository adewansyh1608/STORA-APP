const { Inventaris, FotoInventaris, User, PeminjamanBarang, Peminjaman, sequelize } = require('../models');
const { validationResult } = require('express-validator');
const { Op } = require('sequelize');

class InventarisController {

  async getAllInventaris(req, res) {
    try {
      console.log('===== GET ALL INVENTARIS REQUEST =====');
      console.log('Query params:', req.query);
      console.log('User from token:', req.user);

      const {
        page = 1,
        limit = 10,
        search = '',
        kategori = '',
        kondisi = '',
      } = req.query;
      const offset = (page - 1) * limit;

      console.log(`Fetching page ${page}, limit ${limit}, offset ${offset}`);

      let whereClause = {};


      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
        console.log(`Filtering by user ID: ${req.user.id}`);
      } else {
        console.log('WARNING: No user ID found in request!');
      }
      console.log('Where clause:', JSON.stringify(whereClause));

      if (search) {
        whereClause[Op.or] = [
          { Nama_Barang: { [Op.like]: `%${search}%` } },
          { Kode_Barang: { [Op.like]: `%${search}%` } },
        ];
      }
      if (kategori) {
        whereClause.Kategori = kategori;
      }
      if (kondisi) {
        whereClause.Kondisi = kondisi;
      }

      const { count, rows } = await Inventaris.findAndCountAll({
        where: whereClause,
        limit: parseInt(limit),
        offset: parseInt(offset),
        order: [['ID_Inventaris', 'DESC']],
        include: [
          {
            association: 'user',
            attributes: ['ID_User', 'Nama_User'],
          },
          {
            association: 'foto',
            attributes: ['ID_Foto_Inventaris', 'Foto'],
          },
        ],
      });

      console.log(
        `✓ Found ${count} total items, returning ${rows.length} items`
      );
      console.log('=========================================');

      res.status(200).json({
        success: true,
        data: rows,
        pagination: {
          currentPage: parseInt(page),
          totalPages: Math.ceil(count / limit),
          totalItems: count,
          hasNext: page < Math.ceil(count / limit),
          hasPrev: page > 1,
        },
      });
    } catch (error) {
      console.error('✗ Error getting inventaris:', error.message);
      console.error('Error stack:', error.stack);
      console.log('=========================================');
      res.status(500).json({
        success: false,
        message: error.message,
      });
    }
  }


  async getInventarisById(req, res) {
    try {
      const { id } = req.params;


      const whereClause = {
        ID_Inventaris: id
      };


      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      const inventaris = await Inventaris.findOne({
        where: whereClause,
        include: [
          {
            association: 'user',
            attributes: ['ID_User', 'Nama_User', 'Email'],
          },
          {
            association: 'foto',
            attributes: ['ID_Foto_Inventaris', 'Foto'],
          },
        ],
      });

      if (!inventaris) {
        return res.status(404).json({
          success: false,
          message: 'Inventaris not found',
        });
      }

      res.status(200).json({
        success: true,
        data: inventaris,
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message,
      });
    }
  }


  async createInventaris(req, res) {
    try {
      console.log('===== CREATE INVENTARIS REQUEST =====');
      console.log('Request Body:', JSON.stringify(req.body, null, 2));
      console.log('File:', req.file);
      console.log('User from token:', req.user);

      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        console.error('Validation errors:', errors.array());
        return res.status(400).json({
          success: false,
          message: 'Validation errors',
          errors: errors.array(),
        });
      }

      const inventarisData = req.body;

      const requiredFields = ['Nama_Barang', 'Kode_Barang', 'Jumlah', 'Kategori', 'Kondisi'];
      const missingFields = requiredFields.filter(field => !inventarisData[field] || inventarisData[field].toString().trim() === '');

      if (missingFields.length > 0) {
        console.log('Missing required fields:', missingFields);
        return res.status(400).json({
          success: false,
          message: `Data tidak lengkap. Harap lengkapi: ${missingFields.join(', ')}`,
          missingFields: missingFields
        });
      }

      const normalizeKodeBarang = (kode) => {
        if (!kode) return '';
        const parts = kode.toString().toLowerCase().split(/[\\/\-_]/);
        const normalizedParts = parts.map(part => {
          if (/^\d+$/.test(part)) {
            return parseInt(part, 10).toString();
          }
          return part.trim();
        });
        return normalizedParts.join('/');
      };

      const kodeBarang = inventarisData.Kode_Barang.toString().trim();
      const normalizedInputKode = normalizeKodeBarang(kodeBarang);
      console.log(`Checking duplicate for Kode_Barang: "${kodeBarang}" (normalized: "${normalizedInputKode}")`);

      const userItems = await Inventaris.findAll({
        where: {
          ID_User: req.user.id
        },
        attributes: ['ID_Inventaris', 'Kode_Barang']
      });

      const existingItem = userItems.find(item => {
        const existingNormalized = normalizeKodeBarang(item.Kode_Barang);
        return existingNormalized === normalizedInputKode;
      });

      if (existingItem) {
        console.log(`✗ Duplicate Kode_Barang found: "${kodeBarang}" matches existing "${existingItem.Kode_Barang}"`);
        return res.status(409).json({
          success: false,
          message: `Nomor Inventaris "${kodeBarang}" sudah ada (sama dengan "${existingItem.Kode_Barang}"). Silakan gunakan nomor yang berbeda.`,
          code: 'DUPLICATE_KODE_BARANG'
        });
      }

      inventarisData.ID_User = req.user.id;

      console.log('Data to be saved:', JSON.stringify(inventarisData, null, 2));

      const newInventaris = await Inventaris.create(inventarisData);


      if (req.file) {
        const photoPath = `/uploads/inventaris/${req.file.filename}`;
        await FotoInventaris.create({
          ID_Inventaris: newInventaris.ID_Inventaris,
          Foto: photoPath,
          isSynced: true
        });
        console.log('✓ Photo uploaded:', photoPath);
      }


      const inventarisWithAssociations = await Inventaris.findByPk(
        newInventaris.ID_Inventaris,
        {
          include: [
            {
              association: 'user',
              attributes: ['ID_User', 'Nama_User'],
            },
            {
              association: 'foto',
              attributes: ['ID_Foto_Inventaris', 'Foto'],
            },
          ],
        }
      );

      console.log(
        '✓ Inventaris created successfully:',
        newInventaris.ID_Inventaris
      );
      console.log('=====================================');

      res.status(201).json({
        success: true,
        message: 'Inventaris created successfully',
        data: inventarisWithAssociations,
      });
    } catch (error) {
      console.error('✗ Error creating inventaris:', error);
      console.error('Error stack:', error.stack);
      console.log('=====================================');
      res.status(500).json({
        success: false,
        message: error.message,
      });
    }
  }


  async updateInventaris(req, res) {
    try {
      const { id } = req.params;
      const updateData = req.body;

      console.log(`===== UPDATE INVENTARIS REQUEST (ID: ${id}) =====`);
      console.log('Update Data:', JSON.stringify(updateData, null, 2));

      const whereClause = {
        ID_Inventaris: id
      };

      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      if (updateData.Kode_Barang) {
        const normalizeKodeBarang = (kode) => {
          if (!kode) return '';
          const parts = kode.toString().toLowerCase().split(/[\\/\-_]/);
          const normalizedParts = parts.map(part => {
            if (/^\d+$/.test(part)) {
              return parseInt(part, 10).toString();
            }
            return part.trim();
          });
          return normalizedParts.join('/');
        };

        const kodeBarang = updateData.Kode_Barang.toString().trim();
        const normalizedInputKode = normalizeKodeBarang(kodeBarang);
        console.log(`Checking duplicate for Kode_Barang: "${kodeBarang}" (normalized: "${normalizedInputKode}")`);

        const userItems = await Inventaris.findAll({
          where: {
            ID_User: req.user.id,
            ID_Inventaris: { [Op.ne]: parseInt(id) }
          },
          attributes: ['ID_Inventaris', 'Kode_Barang']
        });

        const existingItem = userItems.find(item => {
          const existingNormalized = normalizeKodeBarang(item.Kode_Barang);
          return existingNormalized === normalizedInputKode;
        });

        if (existingItem) {
          console.log(`✗ Duplicate Kode_Barang found: "${kodeBarang}" matches existing "${existingItem.Kode_Barang}"`);
          return res.status(409).json({
            success: false,
            message: `Nomor Inventaris "${kodeBarang}" sudah ada (sama dengan "${existingItem.Kode_Barang}"). Silakan gunakan nomor yang berbeda.`,
            code: 'DUPLICATE_KODE_BARANG'
          });
        }
      }

      const [updatedRowsCount] = await Inventaris.update(updateData, {
        where: whereClause,
      });

      if (updatedRowsCount === 0) {
        return res.status(404).json({
          success: false,
          message: 'Inventaris not found or you do not have permission to update it',
        });
      }

      const updatedInventaris = await Inventaris.findOne({
        where: whereClause,
        include: [
          {
            association: 'user',
            attributes: ['ID_User', 'Nama_User'],
          },
          {
            association: 'foto',
            attributes: ['ID_Foto_Inventaris', 'Foto'],
          },
        ],
      });

      res.status(200).json({
        success: true,
        message: 'Inventaris updated successfully',
        data: updatedInventaris,
      });
    } catch (error) {
      console.error('Error updating inventaris:', error);
      res.status(500).json({
        success: false,
        message: error.message,
      });
    }
  }


  async deleteInventaris(req, res) {
    const transaction = await sequelize.transaction();
    try {
      const { id } = req.params;
      const fs = require('fs');
      const path = require('path');

      const whereClause = {
        ID_Inventaris: id
      };

      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      const inventaris = await Inventaris.findOne({
        where: whereClause,
        include: [{
          association: 'foto',
          attributes: ['ID_Foto_Inventaris', 'Foto']
        }],
        transaction
      });

      if (!inventaris) {
        await transaction.rollback();
        return res.status(404).json({
          success: false,
          message: 'Inventaris not found or you do not have permission to delete it',
        });
      }

      console.log(`===== DELETE INVENTARIS REQUEST (ID: ${id}) =====`);
      console.log(`Found ${inventaris.foto ? inventaris.foto.length : 0} related photos`);

      if (inventaris.foto && inventaris.foto.length > 0) {
        for (const foto of inventaris.foto) {
          if (foto.Foto) {
            const relativePath = foto.Foto.startsWith('/') ? foto.Foto.substring(1) : foto.Foto;
            const filePath = path.join(__dirname, '../../', relativePath);

            try {
              if (fs.existsSync(filePath)) {
                fs.unlinkSync(filePath);
                console.log(`✓ Deleted photo file: ${filePath}`);
              } else {
                console.log(`Photo file not found (may have been deleted already): ${filePath}`);
              }
            } catch (fileError) {
              console.error(`✗ Error deleting photo file ${filePath}:`, fileError.message);
            }
          }
        }
      }

      const deletedPhotosCount = await FotoInventaris.destroy({
        where: { ID_Inventaris: id },
        transaction
      });
      console.log(`✓ Deleted ${deletedPhotosCount} photos from database`);

      await Inventaris.destroy({
        where: whereClause,
        transaction
      });
      console.log(`✓ Deleted inventaris ID: ${id}`);
      console.log('=================================================');

      await transaction.commit();

      res.status(200).json({
        success: true,
        message: 'Inventaris and related photos deleted successfully',
        deletedPhotos: deletedPhotosCount
      });
    } catch (error) {
      await transaction.rollback();
      console.error('✗ Error deleting inventaris:', error);
      res.status(500).json({
        success: false,
        message: error.message,
      });
    }
  }

  async getBorrowedQuantity(req, res) {
    try {
      const { id } = req.params;

      const whereClause = {
        ID_Inventaris: id
      };

      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      const inventaris = await Inventaris.findOne({
        where: whereClause
      });

      if (!inventaris) {
        return res.status(404).json({
          success: false,
          message: 'Inventaris not found'
        });
      }

      const borrowedQuantity = await PeminjamanBarang.sum('Jumlah', {
        where: {
          ID_Inventaris: id
        },
        include: [{
          model: Peminjaman,
          as: 'peminjaman',
          where: {
            Status: 'Dipinjam'
          },
          attributes: []
        }]
      });

      const totalQuantity = inventaris.Jumlah;
      const borrowed = borrowedQuantity || 0;
      const available = totalQuantity - borrowed;

      res.status(200).json({
        success: true,
        data: {
          totalQuantity,
          borrowedQuantity: borrowed,
          availableQuantity: available
        }
      });
    } catch (error) {
      console.error('Error getting borrowed quantity:', error);
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }


  async getInventarisStats(req, res) {
    try {

      const whereClause = {};
      if (req.user && req.user.id) {
        whereClause.ID_User = req.user.id;
      }

      const totalItems = await Inventaris.count({ where: whereClause });
      const itemsByKondisi = await Inventaris.findAll({
        attributes: [
          'Kondisi',
          [sequelize.fn('COUNT', sequelize.col('ID_Inventaris')), 'count'],
        ],
        where: whereClause,
        group: ['Kondisi'],
      });

      const itemsByKategori = await Inventaris.findAll({
        attributes: [
          'Kategori',
          [sequelize.fn('COUNT', sequelize.col('ID_Inventaris')), 'count'],
        ],
        where: whereClause,
        group: ['Kategori'],
      });

      res.status(200).json({
        success: true,
        data: {
          totalItems,
          itemsByKondisi,
          itemsByKategori,
        },
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message,
      });
    }
  }

  async getSyncData(req, res) {
    try {
      console.log('===== GET SYNC DATA REQUEST =====');
      console.log('Query params:', req.query);
      console.log('User from token:', req.user);

      if (!req.user || !req.user.id) {
        return res.status(401).json({
          success: false,
          message: 'User not authenticated'
        });
      }

      const whereClause = {
        ID_User: req.user.id
      };

      const items = await Inventaris.findAll({
        where: whereClause,
        order: [['ID_Inventaris', 'DESC']],
        include: [
          {
            association: 'user',
            attributes: ['ID_User', 'Nama_User'],
          },
          {
            association: 'foto',
            attributes: ['ID_Foto_Inventaris', 'Foto'],
          },
        ],
      });

      console.log(`✓ Found ${items.length} items for sync`);
      console.log('===================================');

      res.status(200).json({
        success: true,
        data: items,
        syncTimestamp: Date.now(),
        message: `Found ${items.length} items`
      });
    } catch (error) {
      console.error('✗ Error getting sync data:', error.message);
      res.status(500).json({
        success: false,
        message: error.message,
      });
    }
  }
}

module.exports = new InventarisController();
