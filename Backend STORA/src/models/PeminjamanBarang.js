const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db');

const PeminjamanBarang = sequelize.define(
  'PeminjamanBarang',
  {
    ID_Peminjaman_Barang: {
      type: DataTypes.INTEGER,
      primaryKey: true,
      autoIncrement: true,
      field: 'ID_Peminjaman_Barang',
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
    ID_Inventaris: {
      type: DataTypes.INTEGER,
      allowNull: true,
      field: 'ID_Inventaris',
      references: {
        model: 'inventaris',
        key: 'ID_Inventaris',
      },
    },
    Jumlah: {
      type: DataTypes.INTEGER,
      allowNull: true,
      field: 'Jumlah',
      validate: {
        min: 1,
      },
    },
    isSynced: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      field: 'isSynced',
    },
  },
  {
    tableName: 'peminjaman_barang',
    timestamps: false,
  }
);

module.exports = PeminjamanBarang;
