const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db');

const Inventaris = sequelize.define(
  'Inventaris',
  {
    ID_Inventaris: {
      type: DataTypes.INTEGER,
      primaryKey: true,
      autoIncrement: true,
      field: 'ID_Inventaris',
    },
    Nama_Barang: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Nama_Barang',
    },
    Kode_Barang: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Kode_Barang',
    },
    Jumlah: {
      type: DataTypes.INTEGER,
      allowNull: true,
      field: 'Jumlah',
      validate: {
        min: 0,
      },
    },
    Kategori: {
      type: DataTypes.STRING(100),
      allowNull: true,
      field: 'Kategori',
    },
    Lokasi: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Lokasi',
    },
    Kondisi: {
      type: DataTypes.ENUM('Baik', 'Rusak Ringan', 'Rusak Berat'),
      defaultValue: 'Baik',
      field: 'Kondisi',
    },
    Tanggal_Pengadaan: {
      type: DataTypes.DATEONLY,
      allowNull: true,
      field: 'Tanggal_Pengadaan',
    },
    Deskripsi: {
      type: DataTypes.TEXT,
      allowNull: true,
      field: 'Deskripsi',
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
    isSynced: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      field: 'isSynced',
    },
  },
  {
    tableName: 'inventaris',
    timestamps: false,
  }
);

module.exports = Inventaris;
