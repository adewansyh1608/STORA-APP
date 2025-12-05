const mysql = require('mysql2/promise');

async function resetDatabase() {
  const connection = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: ''
  });

  try {
    console.log('Dropping database stora_db...');
    await connection.query('DROP DATABASE IF EXISTS stora_db');
    console.log('✅ Database dropped successfully');

    console.log('Creating database stora_db...');
    await connection.query('CREATE DATABASE stora_db');
    console.log('✅ Database created successfully');

    await connection.end();
    console.log('✅ Database reset completed');
  } catch (error) {
    console.error('❌ Error resetting database:', error);
    await connection.end();
    process.exit(1);
  }
}

resetDatabase();
