const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db');

/**
 * UserDevice model - stores multiple FCM tokens per user
 * Each device that logs in gets its own entry
 */
const UserDevice = sequelize.define(
    'UserDevice',
    {
        ID_Device: {
            type: DataTypes.INTEGER,
            primaryKey: true,
            autoIncrement: true,
            field: 'ID_Device',
        },
        ID_User: {
            type: DataTypes.INTEGER,
            allowNull: false,
            field: 'ID_User',
            references: {
                model: 'users',
                key: 'ID_User',
            },
        },
        FCM_Token: {
            type: DataTypes.TEXT,
            allowNull: false,
            field: 'FCM_Token',
        },
        Device_Name: {
            type: DataTypes.STRING(255),
            allowNull: true,
            field: 'Device_Name',
        },
        Last_Active: {
            type: DataTypes.DATE,
            allowNull: true,
            field: 'Last_Active',
            defaultValue: DataTypes.NOW,
        },
        Is_Active: {
            type: DataTypes.BOOLEAN,
            defaultValue: true,
            field: 'Is_Active',
        },
    },
    {
        tableName: 'user_devices',
        timestamps: false,
        // Note: Cannot add unique index on TEXT field (FCM_Token)
        // Handle uniqueness in application logic instead
    }
);

module.exports = UserDevice;
