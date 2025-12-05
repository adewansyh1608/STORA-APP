'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        await queryInterface.createTable('peminjaman', {
            ID_Peminjaman: {
                allowNull: false,
                autoIncrement: true,
                primaryKey: true,
                type: Sequelize.INTEGER
            },
            Nama_Peminjam: {
                type: Sequelize.STRING(100),
                allowNull: false
            },
            NoHP_Peminjam: {
                type: Sequelize.STRING(20),
                allowNull: true
            },
            Tanggal_Pinjam: {
                type: Sequelize.DATEONLY,
                allowNull: false
            },
            Tanggal_Kembali: {
                type: Sequelize.DATEONLY,
                allowNull: false
            },
            Status: {
                type: Sequelize.ENUM('Menunggu', 'Dipinjam', 'Selesai', 'Terlambat', 'Ditolak'),
                allowNull: false,
                defaultValue: 'Dipinjam'
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

        await queryInterface.addIndex('peminjaman', ['ID_User']);
        await queryInterface.addIndex('peminjaman', ['Status']);
    },

    async down(queryInterface, Sequelize) {
        await queryInterface.dropTable('peminjaman');
    }
};
