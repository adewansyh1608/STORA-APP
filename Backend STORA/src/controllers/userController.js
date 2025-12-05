const { User } = require('../models');
const { validationResult } = require('express-validator');
const { Op } = require('sequelize');

class UserController {
  // Get all users
  async getAllUsers(req, res) {
    try {
      const { page = 1, limit = 10, search = '' } = req.query;
      const offset = (page - 1) * limit;

      let whereClause = {};
      if (search) {
        whereClause = {
          [Op.or]: [
            { Nama_User: { [Op.like]: `%${search}%` } },
            { Email: { [Op.like]: `%${search}%` } }
          ]
        };
      }

      const { count, rows } = await User.findAndCountAll({
        where: whereClause,
        limit: parseInt(limit),
        offset: parseInt(offset),
        order: [['createdAt', 'DESC']],
        attributes: { exclude: ['Password'] }
      });

      res.status(200).json({
        success: true,
        data: rows,
        pagination: {
          currentPage: parseInt(page),
          totalPages: Math.ceil(count / limit),
          totalUsers: count,
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

  // Get user by ID
  async getUserById(req, res) {
    try {
      const { id } = req.params;
      const user = await User.findByPk(id, {
        attributes: { exclude: ['Password'] },
        include: [
          {
            association: 'inventaris',
            attributes: ['ID_Inventaris', 'Nama_Barang', 'Kode_Barang', 'Jumlah']
          },
          {
            association: 'peminjaman',
            attributes: ['ID_Peminjaman', 'Nama_Peminjam', 'Status', 'Tanggal_Pinjam']
          }
        ]
      });
      
      if (!user) {
        return res.status(404).json({
          success: false,
          message: 'User not found'
        });
      }

      res.status(200).json({
        success: true,
        data: user
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Create new user
  async createUser(req, res) {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          success: false,
          message: 'Validation errors',
          errors: errors.array()
        });
      }

      const userData = req.body;
      const newUser = await User.create(userData);
      
      res.status(201).json({
        success: true,
        message: 'User created successfully',
        data: newUser
      });
    } catch (error) {
      if (error.name === 'SequelizeUniqueConstraintError') {
        return res.status(400).json({
          success: false,
          message: 'Email already exists'
        });
      }
      
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Update user
  async updateUser(req, res) {
    try {
      const { id } = req.params;
      const updateData = req.body;
      
      const [updatedRowsCount] = await User.update(updateData, {
        where: { ID_User: id }
      });
      
      if (updatedRowsCount === 0) {
        return res.status(404).json({
          success: false,
          message: 'User not found'
        });
      }

      const updatedUser = await User.findByPk(id, {
        attributes: { exclude: ['Password'] }
      });

      res.status(200).json({
        success: true,
        message: 'User updated successfully',
        data: updatedUser
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Delete user (soft delete by setting isActive to false)
  async deleteUser(req, res) {
    try {
      const { id } = req.params;
      const [updatedRowsCount] = await User.update(
        { isLoggedIn: false },
        { where: { ID_User: id } }
      );
      
      if (updatedRowsCount === 0) {
        return res.status(404).json({
          success: false,
          message: 'User not found'
        });
      }

      res.status(200).json({
        success: true,
        message: 'User deactivated successfully'
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: error.message
      });
    }
  }

  // Login user
  async loginUser(req, res) {
    try {
      const { Email, Password } = req.body;
      
      const user = await User.findOne({ 
        where: { Email },
        attributes: ['ID_User', 'Nama_User', 'Email', 'Password', 'isLoggedIn']
      });

      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'Invalid email or password'
        });
      }

      const isPasswordValid = await user.comparePassword(Password);
      if (!isPasswordValid) {
        return res.status(401).json({
          success: false,
          message: 'Invalid email or password'
        });
      }

      // Update login status
      await user.update({ isLoggedIn: true });

      res.status(200).json({
        success: true,
        message: 'Login successful',
        data: {
          ID_User: user.ID_User,
          Nama_User: user.Nama_User,
          Email: user.Email,
          isLoggedIn: true
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

module.exports = new UserController();
