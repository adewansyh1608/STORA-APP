'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
    async up(queryInterface, Sequelize) {
        // Modify Tanggal_Pinjam from DATE to DATETIME
        await queryInterface.changeColumn('peminjaman', 'Tanggal_Pinjam', {
            type: Sequelize.DATE,
            allowNull: true
        });

        // Modify Tanggal_Kembali from DATE to DATETIME
        await queryInterface.changeColumn('peminjaman', 'Tanggal_Kembali', {
            type: Sequelize.DATE,
            allowNull: true
        });

        // Modify Tanggal_Dikembalikan from DATE to DATETIME
        await queryInterface.changeColumn('peminjaman', 'Tanggal_Dikembalikan', {
            type: Sequelize.DATE,
            allowNull: true
        });
    },

    async down(queryInterface, Sequelize) {
        // Revert back to DATEONLY
        await queryInterface.changeColumn('peminjaman', 'Tanggal_Pinjam', {
            type: Sequelize.DATEONLY,
            allowNull: true
        });

        await queryInterface.changeColumn('peminjaman', 'Tanggal_Kembali', {
            type: Sequelize.DATEONLY,
            allowNull: true
        });

        await queryInterface.changeColumn('peminjaman', 'Tanggal_Dikembalikan', {
            type: Sequelize.DATEONLY,
            allowNull: true
        });
    }
};
