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
    Pesan: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Pesan',
    },
    Tanggal: {
      type: DataTypes.DATEONLY,
      allowNull: true,
      field: 'Tanggal',
    },
    Status: {
      type: DataTypes.STRING(50),
      allowNull: true,
      field: 'Status',
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
