'use strict';

module.exports = {
    up: async (queryInterface, Sequelize) => {
        await queryInterface.createTable('reminder_settings', {
            ID_Reminder: {
                type: Sequelize.INTEGER,
                primaryKey: true,
                autoIncrement: true,
                allowNull: false,
            },
            ID_User: {
                type: Sequelize.INTEGER,
                allowNull: false,
                references: {
                    model: 'users',
                    key: 'ID_User',
                },
                onUpdate: 'CASCADE',
                onDelete: 'CASCADE',
            },
            reminder_type: {
                type: Sequelize.ENUM('periodic', 'custom'),
                allowNull: false,
            },
            title: {
                type: Sequelize.STRING(100),
                allowNull: true,
                defaultValue: 'Pengingat Pengecekan Inventory',
            },
            periodic_months: {
                type: Sequelize.INTEGER,
                allowNull: true,
                defaultValue: 3,
            },
            scheduled_datetime: {
                type: Sequelize.DATE,
                allowNull: true,
            },
            fcm_token: {
                type: Sequelize.STRING(500),
                allowNull: true,
            },
            is_active: {
                type: Sequelize.BOOLEAN,
                defaultValue: true,
            },
            last_notified: {
                type: Sequelize.DATE,
                allowNull: true,
            },
            created_at: {
                type: Sequelize.DATE,
                allowNull: false,
                defaultValue: Sequelize.literal('CURRENT_TIMESTAMP'),
            },
            updated_at: {
                type: Sequelize.DATE,
                allowNull: false,
                defaultValue: Sequelize.literal('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'),
            },
        });

        // Add index for faster queries
        await queryInterface.addIndex('reminder_settings', ['ID_User']);
        await queryInterface.addIndex('reminder_settings', ['reminder_type']);
        await queryInterface.addIndex('reminder_settings', ['is_active']);
    },

    down: async (queryInterface, Sequelize) => {
        await queryInterface.dropTable('reminder_settings');
    },
};
