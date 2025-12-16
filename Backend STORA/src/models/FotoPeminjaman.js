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
        ID_Peminjaman_Barang: {
            type: DataTypes.INTEGER,
            allowNull: true,
            field: 'ID_Peminjaman_Barang',
            references: {
                model: 'peminjaman_barang',
                key: 'ID_Peminjaman_Barang',
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
            defaultValue: true,
            field: 'isSynced',
        },
    },
    {
        tableName: 'foto_peminjaman',
        timestamps: true,
        underscored: false,
    }
);

module.exports = FotoPeminjaman;
