const { User } = require('../models');
const { validationResult } = require('express-validator');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Op } = require('sequelize');

class AuthController {
  // Register/Signup
  async signup(req, res) {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          success: false,
          message: 'Validation errors',
          errors: errors.array().reduce((acc, error) => {
            if (!acc[error.path]) {
              acc[error.path] = [];
            }
            acc[error.path].push(error.msg);
            return acc;
          }, {})
        });
      }

      const { name, email, password, password_confirmation } = req.body;

      // Check if passwords match
      if (password !== password_confirmation) {
        return res.status(400).json({
          success: false,
          message: 'Password confirmation does not match',
          errors: {
            password_confirmation: ['Password confirmation does not match password']
          }
        });
      }

      // Check if user already exists
      const existingUser = await User.findOne({ where: { Email: email } });
      if (existingUser) {
        return res.status(400).json({
          success: false,
          message: 'User already exists',
          errors: {
            email: ['Email already exists']
          }
        });
      }

      // Create new user
      const newUser = await User.create({
        Nama_User: name,
        Email: email,
        Password: password
      });

      // Generate JWT token
      const token = jwt.sign(
        { 
          id: newUser.ID_User, 
          email: newUser.Email 
        },
        process.env.JWT_SECRET || 'your-secret-key',
        { expiresIn: process.env.JWT_EXPIRE || '7d' }
      );

      res.status(201).json({
        success: true,
        message: 'Registration successful',
        data: {
          id: newUser.ID_User,
          name: newUser.Nama_User,
          email: newUser.Email,
          email_verified_at: null,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        },
        token: token
      });
    } catch (error) {
      console.error('Signup error:', error);
      res.status(500).json({
        success: false,
        message: 'Internal server error',
        errors: null
      });
    }
  }

  // Login
  async login(req, res) {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          success: false,
          message: 'Validation errors',
          errors: errors.array().reduce((acc, error) => {
            if (!acc[error.path]) {
              acc[error.path] = [];
            }
            acc[error.path].push(error.msg);
            return acc;
          }, {})
        });
      }

      const { email, password } = req.body;

      // Find user by email
      const user = await User.findOne({ 
        where: { Email: email },
        attributes: ['ID_User', 'Nama_User', 'Email', 'Password']
      });

      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'Invalid credentials',
          data: null,
          token: null
        });
      }

      // Check password
      const isPasswordValid = await user.comparePassword(password);
      if (!isPasswordValid) {
        return res.status(401).json({
          success: false,
          message: 'Invalid credentials',
          data: null,
          token: null
        });
      }

      // Update login status
      await user.update({ isLoggedIn: true });

      // Generate JWT token
      const token = jwt.sign(
        { 
          id: user.ID_User, 
          email: user.Email 
        },
        process.env.JWT_SECRET || 'your-secret-key',
        { expiresIn: process.env.JWT_EXPIRE || '7d' }
      );

      res.status(200).json({
        success: true,
        message: 'Login successful',
        data: {
          id: user.ID_User,
          name: user.Nama_User,
          email: user.Email,
          email_verified_at: null,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        },
        token: token
      });
    } catch (error) {
      console.error('Login error:', error);
      res.status(500).json({
        success: false,
        message: 'Internal server error',
        data: null,
        token: null
      });
    }
  }

  // Logout
  async logout(req, res) {
    try {
      // Get user from token (assuming middleware sets req.user)
      if (req.user) {
        await User.update(
          { isLoggedIn: false },
          { where: { ID_User: req.user.id } }
        );
      }

      res.status(200).json({
        success: true,
        message: 'Logout successful',
        data: null,
        token: null
      });
    } catch (error) {
      console.error('Logout error:', error);
      res.status(500).json({
        success: false,
        message: 'Internal server error',
        data: null,
        token: null
      });
    }
  }

  // Get current user profile
  async getProfile(req, res) {
    try {
      const user = await User.findByPk(req.user.id, {
        attributes: ['ID_User', 'Nama_User', 'Email']
      });

      if (!user) {
        return res.status(404).json({
          success: false,
          message: 'User not found',
          data: null,
          token: null
        });
      }

      res.status(200).json({
        success: true,
        message: 'Profile retrieved successfully',
        data: {
          id: user.ID_User,
          name: user.Nama_User,
          email: user.Email,
          email_verified_at: null,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        },
        token: null
      });
    } catch (error) {
      console.error('Get profile error:', error);
      res.status(500).json({
        success: false,
        message: 'Internal server error',
        data: null,
        token: null
      });
    }
  }

  // Update profile
  async updateProfile(req, res) {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          success: false,
          message: 'Validation errors',
          errors: errors.array().reduce((acc, error) => {
            if (!acc[error.path]) {
              acc[error.path] = [];
            }
            acc[error.path].push(error.msg);
            return acc;
          }, {})
        });
      }

      const { name, email } = req.body;
      const userId = req.user.id;

      // Check if email is already taken by another user
      if (email) {
        const existingUser = await User.findOne({ 
          where: { 
            Email: email,
            ID_User: { [Op.ne]: userId }
          } 
        });
        
        if (existingUser) {
          return res.status(400).json({
            success: false,
            message: 'Email already exists',
            errors: {
              email: ['Email already exists']
            }
          });
        }
      }

      // Update user
      const updateData = {};
      if (name) updateData.Nama_User = name;
      if (email) updateData.Email = email;

      await User.update(updateData, {
        where: { ID_User: userId }
      });

      // Get updated user
      const updatedUser = await User.findByPk(userId, {
        attributes: ['ID_User', 'Nama_User', 'Email']
      });

      res.status(200).json({
        success: true,
        message: 'Profile updated successfully',
        data: {
          id: updatedUser.ID_User,
          name: updatedUser.Nama_User,
          email: updatedUser.Email,
          email_verified_at: null,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        },
        token: null
      });
    } catch (error) {
      console.error('Update profile error:', error);
      res.status(500).json({
        success: false,
        message: 'Internal server error',
        data: null,
        token: null
      });
    }
  }
}

module.exports = new AuthController();
