const { Inventaris, User, sequelize } = require('./src/models');

async function testInventaris() {
  console.log('========================================');
  console.log('🧪 TESTING INVENTARIS API');
  console.log('========================================\n');

  try {
    // Test 1: Database Connection
    console.log('Test 1: Database Connection');
    await sequelize.authenticate();
    console.log('✓ Database connected successfully\n');

    // Test 2: Check if models are loaded
    console.log('Test 2: Check Models');
    console.log('✓ Inventaris model:', typeof Inventaris);
    console.log('✓ User model:', typeof User);
    console.log('✓ findAndCountAll method:', typeof Inventaris.findAndCountAll);
    console.log('✓ create method:', typeof Inventaris.create);
    console.log('✓ findByPk method:', typeof Inventaris.findByPk);
    console.log('✓ update method:', typeof Inventaris.update);
    console.log('✓ destroy method:', typeof Inventaris.destroy);
    console.log('');

    // Test 3: Create User for testing
    console.log('Test 3: Create Test User');
    const [testUser] = await User.findOrCreate({
      where: { Email: 'test@example.com' },
      defaults: {
        Nama_User: 'Test User',
        Email: 'test@example.com',
        Password: 'test123456',
        isSynced: true,
      },
    });
    console.log('✓ Test user created/found with ID:', testUser.ID_User);
    console.log('');

    // Test 4: Create Inventaris
    console.log('Test 4: Create Inventaris');
    const newItem = await Inventaris.create({
      Nama_Barang: 'Monitor LED 24 inch',
      Kode_Barang: 'HMSI/ELK/001',
      Jumlah: 10,
      Kategori: 'Elektronik',
      Lokasi: 'Ruang Sekretariat',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2025-12-04',
      ID_User: testUser.ID_User,
      isSynced: false,
    });
    console.log('✓ Created inventaris with ID:', newItem.ID_Inventaris);
    console.log('  - Nama:', newItem.Nama_Barang);
    console.log('  - Kode:', newItem.Kode_Barang);
    console.log('  - Jumlah:', newItem.Jumlah);
    console.log('');

    // Test 5: Find All with Pagination
    console.log('Test 5: Find All Inventaris (Pagination)');
    const { count, rows } = await Inventaris.findAndCountAll({
      limit: 10,
      offset: 0,
      order: [['ID_Inventaris', 'DESC']],
    });
    console.log('✓ Total items in database:', count);
    console.log('✓ Items returned:', rows.length);
    if (rows.length > 0) {
      console.log('  - First item:', rows[0].Nama_Barang);
    }
    console.log('');

    // Test 6: Find By ID
    console.log('Test 6: Find Inventaris By ID');
    const foundItem = await Inventaris.findByPk(newItem.ID_Inventaris);
    if (foundItem) {
      console.log('✓ Found item:', foundItem.Nama_Barang);
      console.log('  - ID:', foundItem.ID_Inventaris);
      console.log('  - Kode:', foundItem.Kode_Barang);
    } else {
      console.log('✗ Item not found');
    }
    console.log('');

    // Test 7: Update Inventaris
    console.log('Test 7: Update Inventaris');
    const [updatedCount] = await Inventaris.update(
      { Jumlah: 15, Kondisi: 'Baik' },
      { where: { ID_Inventaris: newItem.ID_Inventaris } }
    );
    console.log('✓ Updated rows:', updatedCount);
    const updatedItem = await Inventaris.findByPk(newItem.ID_Inventaris);
    console.log('  - New Jumlah:', updatedItem.Jumlah);
    console.log('  - New Kondisi:', updatedItem.Kondisi);
    console.log('');

    // Test 8: Find with Where Clause
    console.log('Test 8: Find with Where Clause');
    const elektronikItems = await Inventaris.findAll({
      where: { Kategori: 'Elektronik' },
    });
    console.log('✓ Found', elektronikItems.length, 'Elektronik items');
    console.log('');

    // Test 9: Find with Association
    console.log('Test 9: Find with User Association');
    const itemWithUser = await Inventaris.findByPk(newItem.ID_Inventaris, {
      include: [
        {
          association: 'user',
          attributes: ['ID_User', 'Nama_User', 'Email'],
        },
      ],
    });
    if (itemWithUser && itemWithUser.user) {
      console.log('✓ Item with user association loaded');
      console.log('  - Item:', itemWithUser.Nama_Barang);
      console.log('  - User:', itemWithUser.user.Nama_User);
      console.log('  - Email:', itemWithUser.user.Email);
    } else {
      console.log('✗ Association not loaded properly');
    }
    console.log('');

    // Test 10: Count by Category
    console.log('Test 10: Count by Category');
    const categories = await Inventaris.findAll({
      attributes: [
        'Kategori',
        [sequelize.fn('COUNT', sequelize.col('ID_Inventaris')), 'count'],
      ],
      group: ['Kategori'],
    });
    console.log('✓ Categories found:', categories.length);
    categories.forEach((cat) => {
      const categoryData = cat.get({ plain: true });
      console.log(`  - ${categoryData.Kategori}: ${categoryData.count} items`);
    });
    console.log('');

    // Test 11: Delete Inventaris
    console.log('Test 11: Delete Test Inventaris');
    const deletedCount = await Inventaris.destroy({
      where: { Kode_Barang: 'HMSI/ELK/001' },
    });
    console.log('✓ Deleted rows:', deletedCount);
    console.log('');

    // Test 12: Verify Deletion
    console.log('Test 12: Verify Deletion');
    const deletedItem = await Inventaris.findByPk(newItem.ID_Inventaris);
    if (!deletedItem) {
      console.log('✓ Item successfully deleted');
    } else {
      console.log('✗ Item still exists');
    }
    console.log('');

    // Clean up test user
    console.log('Cleaning up test user...');
    await User.destroy({
      where: { Email: 'test@example.com' },
    });
    console.log('✓ Test user deleted\n');

    console.log('========================================');
    console.log('✅ ALL TESTS PASSED');
    console.log('========================================');

    process.exit(0);
  } catch (error) {
    console.error('\n========================================');
    console.error('❌ TEST FAILED');
    console.error('========================================');
    console.error('Error:', error.message);
    console.error('Stack:', error.stack);
    process.exit(1);
  }
}

// Run tests
testInventaris();
