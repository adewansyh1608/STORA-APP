const { DataTypes } = require('sequelize');
const bcrypt = require('bcryptjs');
const { sequelize } = require('../../config/db');

const User = sequelize.define(
  'User',
  {
    ID_User: {
      type: DataTypes.INTEGER,
      primaryKey: true,
      autoIncrement: true,
      field: 'ID_User',
    },
    Nama_User: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Nama_User',
    },
    Email: {
      type: DataTypes.STRING(255),
      allowNull: true,
      unique: true,
      field: 'Email',
      validate: {
        isEmail: {
          msg: 'Please provide a valid email address',
        },
      },
    },
    Password: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Password',
    },
    isSynced: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      field: 'isSynced',
    },
    isLoggedIn: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      field: 'isLoggedIn',
    },
    Foto_Profile: {
      type: DataTypes.TEXT,
      allowNull: true,
      field: 'Foto_Profile',
    },
    FCM_Token: {
      type: DataTypes.TEXT,
      allowNull: true,
      field: 'FCM_Token',
    },
  },
  {
    tableName: 'users',
    timestamps: false,
    hooks: {
      beforeCreate: async (user) => {
        if (user.Password) {
          const salt = await bcrypt.genSalt(12);
          user.Password = await bcrypt.hash(user.Password, salt);
        }
      },
      beforeUpdate: async (user) => {
        if (user.changed('Password') && user.Password) {
          const salt = await bcrypt.genSalt(12);
          user.Password = await bcrypt.hash(user.Password, salt);
        }
      },
    },
  }
);

// Instance method to compare password
User.prototype.comparePassword = async function (candidatePassword) {
  if (!this.Password) return false;
  return await bcrypt.compare(candidatePassword, this.Password);
};

// Instance method to hide password in JSON
User.prototype.toJSON = function () {
  const values = { ...this.get() };
  delete values.Password;
  return values;
};

module.exports = User;
