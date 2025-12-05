'use strict';

module.exports = {
    async up(queryInterface, Sequelize) {
        await queryInterface.createTable('foto_inventaris', {
            ID_Foto_Inventaris: {
                allowNull: false,
                autoIncrement: true,
                primaryKey: true,
                type: Sequelize.INTEGER
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
            Foto: {
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

        await queryInterface.addIndex('foto_inventaris', ['ID_Inventaris']);
    },

    async down(queryInterface, Sequelize) {
        await queryInterface.dropTable('foto_inventaris');
    }
};
