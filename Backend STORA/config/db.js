const { Sequelize } = require('sequelize');

const dialect = "mysql";
const username = "root";
const password = "";
const host = "localhost";
const dbname = "stora_db";

const sequelize = new Sequelize(dbname, username, password, {
  host: host,
  dialect: dialect,
  logging: process.env.NODE_ENV === 'development' ? console.log : false,
  timezone: '+07:00',
  dialectOptions: {
    dateStrings: true,
    typeCast: function (field, next) {
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

const connectDB = async () => {
  try {
    await sequelize.authenticate();
    console.log('✅ Database connection established successfully.');

    await sequelize.sync({ force: false });
    console.log('📊 Database models synchronized.');

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
    } catch (tableError) {
    }

    try {
      const [columns] = await sequelize.query(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = '${dbname}' 
        AND TABLE_NAME = 'peminjaman' 
        AND COLUMN_NAME = 'Tanggal_Dikembalikan'
      `);

      if (columns.length === 0) {
        await sequelize.query(`
          ALTER TABLE peminjaman 
          ADD COLUMN Tanggal_Dikembalikan DATETIME NULL 
          AFTER Tanggal_Kembali
        `);
      }
    } catch (columnError) {
    }
  } catch (error) {
    console.error('❌ Unable to connect to the database:', error);
    process.exit(1);
  }
};

module.exports = { sequelize, connectDB };