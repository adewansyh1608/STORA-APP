'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        await queryInterface.createTable('inventaris', {
            ID_Inventaris: {
                allowNull: false,
                autoIncrement: true,
                primaryKey: true,
                type: Sequelize.INTEGER
            },
            Nama_Barang: {
                type: Sequelize.STRING(100),
                allowNull: false
            },
            Kode_Barang: {
                type: Sequelize.STRING(50),
                allowNull: false,
                unique: true
            },
            Jumlah: {
                type: Sequelize.INTEGER,
                allowNull: false,
                defaultValue: 0
            },
            Kategori: {
                type: Sequelize.STRING(50),
                allowNull: true
            },
            Lokasi: {
                type: Sequelize.STRING(100),
                allowNull: true
            },
            Kondisi: {
                type: Sequelize.ENUM('Baik', 'Rusak Ringan', 'Rusak Berat'),
                allowNull: true,
                defaultValue: 'Baik'
            },
            Tanggal_Pengadaan: {
                type: Sequelize.DATE,
                allowNull: true
            },
            Deskripsi: {
                type: Sequelize.TEXT,
                allowNull: true
            },
            ID_User: {
                type: Sequelize.INTEGER,
                allowNull: false,
                references: {
                    model: 'users',
                    key: 'ID_User'
                },
                onUpdate: 'CASCADE',
                onDelete: 'CASCADE'
            },
            isSynced: {
                type: Sequelize.BOOLEAN,
                allowNull: true,
                defaultValue: true
            },
            createdAt: {
                allowNull: false,
                type: Sequelize.DATE,
                defaultValue: Sequelize.literal('CURRENT_TIMESTAMP')
            },
            updatedAt: {
                allowNull: false,
                type: Sequelize.DATE,
                defaultValue: Sequelize.literal('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')
            }
        });

        // Add index for faster queries
        await queryInterface.addIndex('inventaris', ['ID_User']);
        await queryInterface.addIndex('inventaris', ['Kode_Barang']);
    },

    async down(queryInterface, Sequelize) {
        await queryInterface.dropTable('inventaris');
    }
};
