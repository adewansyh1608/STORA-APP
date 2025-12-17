'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        await queryInterface.addColumn('foto_peminjaman', 'createdAt', {
            type: Sequelize.DATE,
            allowNull: true,
        });

        await queryInterface.addColumn('foto_peminjaman', 'updatedAt', {
            type: Sequelize.DATE,
            allowNull: true,
        });
    },

    async down(queryInterface, Sequelize) {
        await queryInterface.removeColumn('foto_peminjaman', 'createdAt');
        await queryInterface.removeColumn('foto_peminjaman', 'updatedAt');
    },
};
