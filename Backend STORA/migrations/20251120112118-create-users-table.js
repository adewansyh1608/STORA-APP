'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up (queryInterface, Sequelize) {
    await queryInterface.createTable('users', {
      ID_User: {
        type: Sequelize.INTEGER,
        primaryKey: true,
        autoIncrement: true,
        allowNull: false
      },
      Nama_User: {
        type: Sequelize.STRING(255),
        allowNull: true
      },
      Email: {
        type: Sequelize.STRING(255),
        allowNull: true,
        unique: true
      },
      Password: {
        type: Sequelize.STRING(255),
        allowNull: true
      },
      isSynced: {
        type: Sequelize.BOOLEAN,
        defaultValue: false,
        allowNull: false
      },
      isLoggedIn: {
        type: Sequelize.BOOLEAN,
        defaultValue: false,
        allowNull: false
      },
      createdAt: {
        type: Sequelize.DATE,
        allowNull: false,
        defaultValue: Sequelize.literal('CURRENT_TIMESTAMP')
      },
      updatedAt: {
        type: Sequelize.DATE,
        allowNull: false,
        defaultValue: Sequelize.literal('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')
      }
    });
  },

  async down (queryInterface, Sequelize) {
    await queryInterface.dropTable('users');
  }
};
