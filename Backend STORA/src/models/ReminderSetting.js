const { DataTypes } = require('sequelize');
const { sequelize } = require('../../config/db');

const ReminderSetting = sequelize.define(
    'ReminderSetting',
    {
        ID_Reminder: {
            type: DataTypes.INTEGER,
            primaryKey: true,
            autoIncrement: true,
            field: 'ID_Reminder',
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
        reminder_type: {
            type: DataTypes.ENUM('periodic', 'custom'),
            allowNull: false,
            field: 'reminder_type',
        },
        title: {
            type: DataTypes.STRING(100),
            allowNull: true,
            field: 'title',
            defaultValue: 'Pengingat Pengecekan Inventory',
        },
        periodic_months: {
            type: DataTypes.INTEGER,
            allowNull: true,
            field: 'periodic_months',
            defaultValue: 3,
            validate: {
                min: 1,
                max: 12,
            },
        },
        scheduled_datetime: {
            type: DataTypes.DATE,
            allowNull: true,
            field: 'scheduled_datetime',
        },
        fcm_token: {
            type: DataTypes.STRING(500),
            allowNull: true,
            field: 'fcm_token',
        },
        is_active: {
            type: DataTypes.BOOLEAN,
            defaultValue: true,
            field: 'is_active',
        },
        last_notified: {
            type: DataTypes.DATE,
            allowNull: true,
            field: 'last_notified',
        },
    },
    {
        tableName: 'reminder_settings',
        timestamps: true,
        createdAt: 'created_at',
        updatedAt: 'updated_at',
    }
);

module.exports = ReminderSetting;
