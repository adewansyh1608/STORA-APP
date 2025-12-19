'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
    async up(queryInterface, Sequelize) {
        // Change Tanggal column from DATE to DATETIME to include time
        await queryInterface.changeColumn('notifikasi', 'Tanggal', {
            type: Sequelize.DATE, // DATETIME in MySQL
            allowNull: true,
        });

        // Add ID_Reminder column for deduplication
        await queryInterface.addColumn('notifikasi', 'ID_Reminder', {
            type: Sequelize.INTEGER,
            allowNull: true,
            references: {
                model: 'reminder_settings',
                key: 'ID_Reminder',
            },
            onUpdate: 'CASCADE',
            onDelete: 'SET NULL',
        });

        // Update existing Status values to be consistent
        await queryInterface.sequelize.query(`
      UPDATE notifikasi SET Status = 'Terkirim' WHERE Status = 'sent' OR Status = 'Sent'
    `);
    },

    async down(queryInterface, Sequelize) {
        // Revert Tanggal to DATEONLY
        await queryInterface.changeColumn('notifikasi', 'Tanggal', {
            type: Sequelize.DATEONLY,
            allowNull: true,
        });

        // Remove ID_Reminder column
        await queryInterface.removeColumn('notifikasi', 'ID_Reminder');
    }
};
