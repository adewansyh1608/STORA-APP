const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db');

const Peminjaman = sequelize.define(
  'Peminjaman',
  {
    ID_Peminjaman: {
      type: DataTypes.INTEGER,
      primaryKey: true,
      autoIncrement: true,
      field: 'ID_Peminjaman',
    },
    Nama_Peminjam: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'Nama_Peminjam',
    },
    NoHP_Peminjam: {
      type: DataTypes.STRING(255),
      allowNull: true,
      field: 'NoHP_Peminjam',
    },
    Tanggal_Pinjam: {
      type: DataTypes.DATE,
      allowNull: true,
      field: 'Tanggal_Pinjam',
    },
    Tanggal_Kembali: {
      type: DataTypes.DATE,
      allowNull: true,
      field: 'Tanggal_Kembali',
    },
    Tanggal_Dikembalikan: {
      type: DataTypes.DATE,
      allowNull: true,
      field: 'Tanggal_Dikembalikan',
    },
    Status: {
      type: DataTypes.ENUM(
        'Menunggu',
        'Dipinjam',
        'Selesai',
        'Terlambat',
        'Ditolak'
      ),
      defaultValue: 'Menunggu',
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
    isSynced: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      field: 'isSynced',
    },
  },
  {
    tableName: 'peminjaman',
    timestamps: false,
  }
);

module.exports = Peminjaman;
