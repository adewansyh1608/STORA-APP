const mysql = require('mysql2/promise');

async function addDescriptionColumn() {
  const connection = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'stora_db'
  });

  try {
    console.log('Adding Deskripsi column to inventaris table...');
    
    // Check if column already exists
    const [columns] = await connection.query(
      "SHOW COLUMNS FROM inventaris LIKE 'Deskripsi'"
    );
    
    if (columns.length > 0) {
      console.log('✅ Deskripsi column already exists');
    } else {
      // Add Deskripsi column after Tanggal_Pengadaan
      await connection.query(`
        ALTER TABLE inventaris 
        ADD COLUMN Deskripsi TEXT NULL 
        AFTER Tanggal_Pengadaan
      `);
      console.log('✅ Deskripsi column added successfully');
    }

    await connection.end();
    console.log('✅ Database migration completed');
  } catch (error) {
    console.error('❌ Error adding column:', error);
    await connection.end();
    process.exit(1);
  }
}

addDescriptionColumn();
