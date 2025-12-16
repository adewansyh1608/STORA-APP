'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        // Step 1: Add new column ID_Peminjaman_Barang
        await queryInterface.addColumn('foto_peminjaman', 'ID_Peminjaman_Barang', {
            type: Sequelize.INTEGER,
            allowNull: true,
            references: {
                model: 'peminjaman_barang',
                key: 'ID_Peminjaman_Barang'
            },
            onUpdate: 'CASCADE',
            onDelete: 'CASCADE'
        });

        // Step 2: Remove old FK constraint and column
        await queryInterface.removeIndex('foto_peminjaman', ['ID_Peminjaman']);
        await queryInterface.removeColumn('foto_peminjaman', 'ID_Peminjaman');

        // Step 3: Add new index for the new FK
        await queryInterface.addIndex('foto_peminjaman', ['ID_Peminjaman_Barang']);
    },

    async down(queryInterface, Sequelize) {
        // Revert: Add back ID_Peminjaman column
        await queryInterface.addColumn('foto_peminjaman', 'ID_Peminjaman', {
            type: Sequelize.INTEGER,
            allowNull: true,
            references: {
                model: 'peminjaman',
                key: 'ID_Peminjaman'
            },
            onUpdate: 'CASCADE',
            onDelete: 'CASCADE'
        });

        // Remove the new column
        await queryInterface.removeIndex('foto_peminjaman', ['ID_Peminjaman_Barang']);
        await queryInterface.removeColumn('foto_peminjaman', 'ID_Peminjaman_Barang');

        // Add back old index
        await queryInterface.addIndex('foto_peminjaman', ['ID_Peminjaman']);
    }
};
