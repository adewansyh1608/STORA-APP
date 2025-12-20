const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db');

const Notifikasi = sequelize.define(
  'Notifikasi',
  {
    ID_Notifikasi: {
      type: DataTypes.INTEGER,
      primaryKey: true,
      autoIncrement: true,
      field: 'ID_Notifikasi',
    },
    Judul: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Judul',
    },
    Pesan: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Pesan',
    },
    Tanggal: {
      type: DataTypes.DATE, // Changed from DATEONLY to DATE to include time
      allowNull: true,
      field: 'Tanggal',
    },
    Status: {
      type: DataTypes.STRING(50),
      allowNull: true,
      defaultValue: 'Terkirim', // Standardize default status
      field: 'Status',
    },
    ID_Reminder: {
      type: DataTypes.INTEGER,
      allowNull: true,
      field: 'ID_Reminder',
      references: {
        model: 'reminder_settings',
        key: 'ID_Reminder',
      },
    },
    ID_User: {
      type: DataTypes.INTEGER,
      allowNull: true,
      field: 'ID_User',
      references: {
        model: 'users',
        key: 'ID_User',
      },
    },
    ID_Peminjaman: {
      type: DataTypes.INTEGER,
      allowNull: true,
      field: 'ID_Peminjaman',
      references: {
        model: 'peminjaman',
        key: 'ID_Peminjaman',
      },
    },
    isSynced: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      field: 'isSynced',
    },
  },
  {
    tableName: 'notifikasi',
    timestamps: false,
  }
);

module.exports = Notifikasi;
