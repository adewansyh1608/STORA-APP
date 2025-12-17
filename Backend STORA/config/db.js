const { Sequelize } = require('sequelize');

const dialect = "mysql";
const username = "root";
const password = "";
const host = "localhost";
const dbname = "stora_db";

// Create Sequelize instance
const sequelize = new Sequelize(dbname, username, password, {
  host: host,
  dialect: dialect,
  logging: process.env.NODE_ENV === 'development' ? console.log : false,
  timezone: '+07:00', // Asia/Jakarta timezone
  dialectOptions: {
    dateStrings: true,
    typeCast: function (field, next) {
      // For DATETIME and DATE fields, return as-is (string) to avoid timezone conversion
      if (field.type === 'DATETIME' || field.type === 'DATE') {
        return field.string();
      }
      return next();
    }
  },
  pool: {
    max: 10,
    min: 0,
    acquire: 30000,
    idle: 10000
  },
  define: {
    timestamps: false,
    underscored: false,
    freezeTableName: true
  }
});

// Test database connection
const connectDB = async () => {
  try {
    await sequelize.authenticate();
    console.log('✅ Database connection established successfully.');

    // Sync models - use force: false to not alter existing tables
    await sequelize.sync({ force: false });
    console.log('📊 Database models synchronized.');

    // Create user_devices table if it doesn't exist (for multi-device notifications)
    try {
      await sequelize.query(`
        CREATE TABLE IF NOT EXISTS user_devices (
          ID_Device INT NOT NULL AUTO_INCREMENT,
          ID_User INT NOT NULL,
          FCM_Token TEXT NOT NULL,
          Device_Name VARCHAR(255) DEFAULT NULL,
          Last_Active DATETIME DEFAULT CURRENT_TIMESTAMP,
          Is_Active TINYINT(1) DEFAULT 1,
          PRIMARY KEY (ID_Device),
          KEY idx_user_id (ID_User),
          CONSTRAINT fk_user_devices_user FOREIGN KEY (ID_User) REFERENCES users (ID_User) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
      `);
      console.log('📱 user_devices table ready.');
    } catch (tableError) {
      // Table might already exist with constraints, ignore error
      console.log('📱 user_devices table check complete.');
    }
  } catch (error) {
    console.error('❌ Unable to connect to the database:', error);
    process.exit(1);
  }
};

module.exports = { sequelize, connectDB };