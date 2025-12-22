'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
    async up(queryInterface, Sequelize) {
        // Add Tanggal_Dikembalikan column if it doesn't exist
        const tableDescription = await queryInterface.describeTable('peminjaman');

        if (!tableDescription.Tanggal_Dikembalikan) {
            await queryInterface.addColumn('peminjaman', 'Tanggal_Dikembalikan', {
                type: Sequelize.DATE,
                allowNull: true,
                after: 'Tanggal_Kembali'
            });
            console.log('Added Tanggal_Dikembalikan column to peminjaman table');
        } else {
            console.log('Tanggal_Dikembalikan column already exists');
        }
    },

    async down(queryInterface, Sequelize) {
        const tableDescription = await queryInterface.describeTable('peminjaman');

        if (tableDescription.Tanggal_Dikembalikan) {
            await queryInterface.removeColumn('peminjaman', 'Tanggal_Dikembalikan');
        }
    }
};
