-- SQL script to create user_devices table for multi-device notification support
-- Run this script manually using MySQL Workbench, PHPMyAdmin, or MySQL CLI

-- Create the user_devices table
CREATE TABLE IF NOT EXISTS `user_devices` (
    `ID_Device` INT NOT NULL AUTO_INCREMENT,
    `ID_User` INT NOT NULL,
    `FCM_Token` TEXT NOT NULL,
    `Device_Name` VARCHAR(255) DEFAULT NULL,
    `Last_Active` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `Is_Active` TINYINT(1) DEFAULT 1,
    PRIMARY KEY (`ID_Device`),
    UNIQUE KEY `user_device_unique` (`ID_User`, `FCM_Token`(191)),
    KEY `idx_user_id` (`ID_User`),
    CONSTRAINT `fk_user_devices_user` FOREIGN KEY (`ID_User`) REFERENCES `users` (`ID_User`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- To run in MySQL CLI:
-- mysql -u root -p stora_db < create_user_devices_table.sql
