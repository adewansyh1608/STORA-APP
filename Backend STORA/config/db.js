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
    // New columns like Foto_Profile need to be added manually via SQL
    await sequelize.sync({ force: false });
    console.log('📊 Database models synchronized.');
  } catch (error) {
    console.error('❌ Unable to connect to the database:', error);
    process.exit(1);
  }
};

module.exports = { sequelize, connectDB };