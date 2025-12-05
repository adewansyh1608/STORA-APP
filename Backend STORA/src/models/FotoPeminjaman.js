const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db');

const FotoPeminjaman = sequelize.define(
  'FotoPeminjaman',
  {
    ID_Foto_Peminjaman: {
      type: DataTypes.INTEGER,
      primaryKey: true,
      autoIncrement: true,
      field: 'ID_Foto_Peminjaman',
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
    Foto_Peminjaman: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Foto_Peminjaman',
    },
    Foto_Pengembalian: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Foto_Pengembalian',
    },
    Foto_Barang: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Foto_Barang',
    },
    isSynced: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      field: 'isSynced',
    },
  },
  {
    tableName: 'foto_peminjaman',
    timestamps: false,
  }
);

module.exports = FotoPeminjaman;
