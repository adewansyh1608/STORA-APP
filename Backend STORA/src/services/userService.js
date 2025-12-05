const User = require('../models/UserModel');

class UserService {
  // Get all users with pagination
  async getAllUsers(page = 1, limit = 10, search = '') {
    try {
      const skip = (page - 1) * limit;
      
      let query = {};
      if (search) {
        query = {
          $or: [
            { name: { $regex: search, $options: 'i' } },
            { email: { $regex: search, $options: 'i' } }
          ]
        };
      }

      const users = await User.find(query)
        .select('-password')
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(parseInt(limit));

      const total = await User.countDocuments(query);

      return {
        users,
        pagination: {
          currentPage: parseInt(page),
          totalPages: Math.ceil(total / limit),
          totalUsers: total,
          hasNext: page < Math.ceil(total / limit),
          hasPrev: page > 1
        }
      };
    } catch (error) {
      throw new Error(`Error fetching users: ${error.message}`);
    }
  }

  // Get user by ID
  async getUserById(id) {
    try {
      const user = await User.findById(id).select('-password');
      return user;
    } catch (error) {
      throw new Error(`Error fetching user: ${error.message}`);
    }
  }

  // Get user by email
  async getUserByEmail(email) {
    try {
      const user = await User.findOne({ email }).select('+password');
      return user;
    } catch (error) {
      throw new Error(`Error fetching user by email: ${error.message}`);
    }
  }

  // Create new user
  async createUser(userData) {
    try {
      const existingUser = await User.findOne({ email: userData.email });
      if (existingUser) {
        throw new Error('User with this email already exists');
      }

      const user = new User(userData);
      await user.save();
      
      // Return user without password
      return await User.findById(user._id).select('-password');
    } catch (error) {
      throw new Error(`Error creating user: ${error.message}`);
    }
  }

  // Update user
  async updateUser(id, updateData) {
    try {
      // Remove password from update data if present (should be updated separately)
      const { password, ...safeUpdateData } = updateData;

      const user = await User.findByIdAndUpdate(
        id,
        safeUpdateData,
        { new: true, runValidators: true }
      ).select('-password');

      return user;
    } catch (error) {
      throw new Error(`Error updating user: ${error.message}`);
    }
  }

  // Update user password
  async updatePassword(id, newPassword) {
    try {
      const user = await User.findById(id);
      if (!user) {
        throw new Error('User not found');
      }

      user.password = newPassword;
      await user.save();

      return { message: 'Password updated successfully' };
    } catch (error) {
      throw new Error(`Error updating password: ${error.message}`);
    }
  }

  // Delete user (soft delete by setting isActive to false)
  async deleteUser(id) {
    try {
      const user = await User.findByIdAndUpdate(
        id,
        { isActive: false },
        { new: true }
      );
      return user;
    } catch (error) {
      throw new Error(`Error deleting user: ${error.message}`);
    }
  }

  // Permanently delete user
  async permanentlyDeleteUser(id) {
    try {
      const user = await User.findByIdAndDelete(id);
      return user;
    } catch (error) {
      throw new Error(`Error permanently deleting user: ${error.message}`);
    }
  }

  // Update last login
  async updateLastLogin(id) {
    try {
      await User.findByIdAndUpdate(id, { lastLogin: new Date() });
    } catch (error) {
      throw new Error(`Error updating last login: ${error.message}`);
    }
  }
}

module.exports = new UserService();
