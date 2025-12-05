'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        await queryInterface.createTable('foto_peminjaman', {
            ID_Foto_Peminjaman: {
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
            Foto_Peminjaman: {
                type: Sequelize.STRING(255),
                allowNull: true
            },
            Foto_Pengembalian: {
                type: Sequelize.STRING(255),
                allowNull: true
            },
            Foto_Barang: {
                type: Sequelize.STRING(255),
                allowNull: true
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

        await queryInterface.addIndex('foto_peminjaman', ['ID_Peminjaman']);
    },

    async down(queryInterface, Sequelize) {
        await queryInterface.dropTable('foto_peminjaman');
    }
};
