'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        await queryInterface.createTable('peminjaman_barang', {
            ID_Peminjaman_Barang: {
                allowNull: false,
                autoIncrement: true,
                primaryKey: true,
                type: Sequelize.INTEGER
            },
            ID_Peminjaman: {
                type: Sequelize.INTEGER,
                allowNull: false,
                references: {
                    model: 'peminjaman',
                    key: 'ID_Peminjaman'
                },
                onUpdate: 'CASCADE',
                onDelete: 'CASCADE'
            },
            ID_Inventaris: {
                type: Sequelize.INTEGER,
                allowNull: false,
                references: {
                    model: 'inventaris',
                    key: 'ID_Inventaris'
                },
                onUpdate: 'CASCADE',
                onDelete: 'CASCADE'
            },
            Jumlah: {
                type: Sequelize.INTEGER,
                allowNull: false,
                defaultValue: 1
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

        await queryInterface.addIndex('peminjaman_barang', ['ID_Peminjaman']);
        await queryInterface.addIndex('peminjaman_barang', ['ID_Inventaris']);
    },

    async down(queryInterface, Sequelize) {
        await queryInterface.dropTable('peminjaman_barang');
    }
};
