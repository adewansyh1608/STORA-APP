const { Sequelize } = require('sequelize');

const connectDB = async () => {
  try {
    const sequelize = new Sequelize(
      process.env.DB_NAME || 'stora_db',
      process.env.DB_USER || 'root',
      process.env.DB_PASSWORD || '',
      {
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 3306,
        dialect: process.env.DB_DIALECT || 'mysql',
        logging: process.env.NODE_ENV === 'development' ? console.log : false,
        pool: {
          max: 5,
          min: 0,
          acquire: 30000,
          idle: 10000
        }
      }
    );

    await sequelize.authenticate();
    console.log('✅ Database connection established successfully.');

    if (process.env.NODE_ENV === 'development') {
      await sequelize.sync({ alter: true });
      console.log('📊 Database models synchronized.');
    }

    return sequelize;
  } catch (error) {
    console.error('❌ Unable to connect to the database:', error);
    process.exit(1);
  }
};

module.exports = connectDB;
