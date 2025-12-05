'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        await queryInterface.createTable('notifikasi', {
            ID_Notifikasi: {
                allowNull: false,
                autoIncrement: true,
                primaryKey: true,
                type: Sequelize.INTEGER
            },
            Pesan: {
                type: Sequelize.TEXT,
                allowNull: false
            },
            Tanggal: {
                type: Sequelize.DATE,
                allowNull: false,
                defaultValue: Sequelize.literal('CURRENT_TIMESTAMP')
            },
            Status: {
                type: Sequelize.ENUM('Dibaca', 'Belum Dibaca'),
                allowNull: false,
                defaultValue: 'Belum Dibaca'
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
            ID_Peminjaman: {
                type: Sequelize.INTEGER,
                allowNull: true,
                references: {
                    model: 'peminjaman',
                    key: 'ID_Peminjaman'
                },
                onUpdate: 'CASCADE',
                onDelete: 'SET NULL'
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

        await queryInterface.addIndex('notifikasi', ['ID_User']);
        await queryInterface.addIndex('notifikasi', ['ID_Peminjaman']);
    },

    async down(queryInterface, Sequelize) {
        await queryInterface.dropTable('notifikasi');
    }
};
