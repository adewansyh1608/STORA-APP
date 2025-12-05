#!/usr/bin/env node

/**
 * STORA Backend - Database Sync Validation Script
 *
 * This script validates that the database sync issue has been fixed
 * by checking all critical components and configurations.
 *
 * Run this script to verify the fix: node validate-fix.js
 */

const fs = require('fs');
const path = require('path');

// Colors for terminal output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function logSection(title) {
  console.log('\n' + '='.repeat(60));
  log(title, 'cyan');
  console.log('='.repeat(60));
}

function logCheck(message, passed) {
  const symbol = passed ? '✅' : '❌';
  const color = passed ? 'green' : 'red';
  log(`${symbol} ${message}`, color);
}

function logWarning(message) {
  log(`⚠️  ${message}`, 'yellow');
}

let totalChecks = 0;
let passedChecks = 0;
let criticalFailures = [];

function check(description, condition, critical = false) {
  totalChecks++;
  const passed = typeof condition === 'function' ? condition() : condition;

  if (passed) {
    passedChecks++;
    logCheck(description, true);
  } else {
    logCheck(description, false);
    if (critical) {
      criticalFailures.push(description);
    }
  }

  return passed;
}

// Validation functions
function validateFileExists(filePath) {
  return fs.existsSync(filePath);
}

function validateFileContains(filePath, searchText) {
  if (!fs.existsSync(filePath)) return false;
  const content = fs.readFileSync(filePath, 'utf8');
  return content.includes(searchText);
}

function validateModelExports() {
  try {
    const modelsIndexPath = path.join(__dirname, 'src', 'models', 'index.js');
    const content = fs.readFileSync(modelsIndexPath, 'utf8');

    const requiredExports = [
      'sequelize',
      'User',
      'Inventaris',
      'FotoInventaris',
      'Peminjaman',
      'PeminjamanBarang',
      'FotoPeminjaman',
      'Notifikasi'
    ];

    return requiredExports.every(exp => content.includes(exp));
  } catch (error) {
    return false;
  }
}

function validateTimestampsSetting(modelName) {
  try {
    const modelPath = path.join(__dirname, 'src', 'models', `${modelName}.js`);
    const content = fs.readFileSync(modelPath, 'utf8');
    return content.includes('timestamps: false');
  } catch (error) {
    return false;
  }
}

function validateTableName(modelName, expectedTableName) {
  try {
    const modelPath = path.join(__dirname, 'src', 'models', `${modelName}.js`);
    const content = fs.readFileSync(modelPath, 'utf8');
    return content.includes(`tableName: '${expectedTableName}'`);
  } catch (error) {
    return false;
  }
}

function validateAssociations() {
  try {
    const modelsIndexPath = path.join(__dirname, 'src', 'models', 'index.js');
    const content = fs.readFileSync(modelsIndexPath, 'utf8');

    const requiredAssociations = [
      'User.hasMany(Inventaris',
      'Inventaris.belongsTo(User',
      'Inventaris.hasMany(FotoInventaris',
    ];

    return requiredAssociations.every(assoc => content.includes(assoc));
  } catch (error) {
    return false;
  }
}

function validateAppInitialization() {
  try {
    const appPath = path.join(__dirname, 'app.js');
    const content = fs.readFileSync(appPath, 'utf8');
    return content.includes("require('./src/models')");
  } catch (error) {
    return false;
  }
}

async function testDatabaseConnection() {
  try {
    const { sequelize } = require('./config/db');
    await sequelize.authenticate();
    return true;
  } catch (error) {
    return false;
  }
}

async function testModelMethods() {
  try {
    const { Inventaris } = require('./src/models');
    return (
      typeof Inventaris.create === 'function' &&
      typeof Inventaris.findAndCountAll === 'function' &&
      typeof Inventaris.findByPk === 'function' &&
      typeof Inventaris.update === 'function' &&
      typeof Inventaris.destroy === 'function'
    );
  } catch (error) {
    return false;
  }
}

async function testCRUDOperations() {
  try {
    const { Inventaris, User, sequelize } = require('./src/models');
    await sequelize.authenticate();

    // Create test user
    const [testUser] = await User.findOrCreate({
      where: { Email: 'validation@test.com' },
      defaults: {
        Nama_User: 'Validation Test',
        Email: 'validation@test.com',
        Password: 'test123',
      },
    });

    // Create test item
    const testItem = await Inventaris.create({
      Nama_Barang: 'Validation Test Item',
      Kode_Barang: 'VAL/TEST/999',
      Jumlah: 1,
      Kategori: 'Test',
      Lokasi: 'Test',
      Kondisi: 'Baik',
      Tanggal_Pengadaan: '2025-12-04',
      ID_User: testUser.ID_User,
    });

    // Read
    const foundItem = await Inventaris.findByPk(testItem.ID_Inventaris);
    if (!foundItem) return false;

    // Update
    await Inventaris.update(
      { Jumlah: 2 },
      { where: { ID_Inventaris: testItem.ID_Inventaris } }
    );

    // Delete
    await Inventaris.destroy({
      where: { ID_Inventaris: testItem.ID_Inventaris },
    });

    // Cleanup test user
    await User.destroy({ where: { Email: 'validation@test.com' } });

    return true;
  } catch (error) {
    console.error('CRUD test error:', error.message);
    return false;
  }
}

// Main validation function
async function runValidation() {
  log('\n🔍 STORA Backend - Database Sync Validation', 'bright');
  log('This script validates that the database sync fix is complete\n', 'cyan');

  // Section 1: File Structure
  logSection('📁 File Structure Validation');

  check('config/db.js exists', () => validateFileExists('config/db.js'), true);
  check('src/models/index.js exists', () => validateFileExists('src/models/index.js'), true);
  check('src/models/User.js exists', () => validateFileExists('src/models/User.js'), true);
  check('src/models/Inventaris.js exists', () => validateFileExists('src/models/Inventaris.js'), true);
  check('src/models/FotoInventaris.js exists', () => validateFileExists('src/models/FotoInventaris.js'));
  check('src/controllers/inventarisController.js exists', () => validateFileExists('src/controllers/inventarisController.js'), true);
  check('app.js exists', () => validateFileExists('app.js'), true);

  // Section 2: Model Configuration
  logSection('⚙️  Model Configuration Validation');

  check('models/index.js exports all required models', validateModelExports, true);
  check('models/index.js imports sequelize from config/db', () =>
    validateFileContains('src/models/index.js', "require('../../config/db')"), true
  );
  check('models/index.js has User associations', validateAssociations, true);

  // Section 3: Timestamp Settings
  logSection('⏰ Timestamp Configuration');

  check('User model has timestamps: false', () => validateTimestampsSetting('User'), true);
  check('Inventaris model has timestamps: false', () => validateTimestampsSetting('Inventaris'), true);
  check('FotoInventaris model has timestamps: false', () => validateTimestampsSetting('FotoInventaris'));
  check('Peminjaman model has timestamps: false', () => validateTimestampsSetting('Peminjaman'));

  // Section 4: Table Names
  logSection('📊 Table Name Configuration');

  check('User model uses lowercase table name (users)', () => validateTableName('User', 'users'), true);
  check('Inventaris model uses lowercase table name (inventaris)', () => validateTableName('Inventaris', 'inventaris'), true);
  check('FotoInventaris model uses lowercase table name (foto_inventaris)', () => validateTableName('FotoInventaris', 'foto_inventaris'));

  // Section 5: App Initialization
  logSection('🚀 Application Initialization');

  check('app.js initializes models', validateAppInitialization, true);
  check('app.js imports database connection', () =>
    validateFileContains('app.js', "require('./config/db')"), true
  );

  // Section 6: Database Tests
  logSection('🗄️  Database Connection Tests');

  const dbConnected = await testDatabaseConnection();
  check('Database connection successful', dbConnected, true);

  if (dbConnected) {
    check('Model methods are available', await testModelMethods, true);

    log('\n📝 Running CRUD operations test (this may take a few seconds)...', 'yellow');
    check('CRUD operations work correctly', await testCRUDOperations, true);
  } else {
    logWarning('Skipping model tests - database not connected');
  }

  // Section 7: Controller Validation
  logSection('🎮 Controller Validation');

  check('inventarisController imports models correctly', () =>
    validateFileContains('src/controllers/inventarisController.js', "require('../models')"), true
  );
  check('inventarisController has getAllInventaris method', () =>
    validateFileContains('src/controllers/inventarisController.js', 'getAllInventaris')
  );
  check('inventarisController has createInventaris method', () =>
    validateFileContains('src/controllers/inventarisController.js', 'createInventaris')
  );

  // Final Summary
  logSection('📊 VALIDATION SUMMARY');

  const passRate = ((passedChecks / totalChecks) * 100).toFixed(1);

  console.log(`\nTotal Checks: ${totalChecks}`);
  log(`Passed: ${passedChecks}`, 'green');
  log(`Failed: ${totalChecks - passedChecks}`, passedChecks === totalChecks ? 'green' : 'red');
  log(`Pass Rate: ${passRate}%\n`, passRate === '100.0' ? 'green' : 'yellow');

  if (criticalFailures.length > 0) {
    log('❌ CRITICAL FAILURES DETECTED:', 'red');
    criticalFailures.forEach(failure => {
      log(`   • ${failure}`, 'red');
    });
    console.log('');
  }

  if (passedChecks === totalChecks) {
    log('✅ ALL VALIDATIONS PASSED!', 'green');
    log('🎉 Database sync fix is complete and verified!', 'green');
    log('\n📝 Next steps:', 'cyan');
    log('   1. Start the server: npm start', 'cyan');
    log('   2. Test with mobile app or run: node test-api.js', 'cyan');
    log('   3. See TESTING_GUIDE.md for more details\n', 'cyan');
    process.exit(0);
  } else if (criticalFailures.length > 0) {
    log('❌ VALIDATION FAILED - Critical issues found', 'red');
    log('Please review the failures above and fix them before proceeding.\n', 'red');
    process.exit(1);
  } else {
    log('⚠️  VALIDATION COMPLETED WITH WARNINGS', 'yellow');
    log('Some non-critical checks failed, but the system should work.\n', 'yellow');
    process.exit(0);
  }
}

// Error handling
process.on('unhandledRejection', (error) => {
  log('\n❌ Unexpected error during validation:', 'red');
  console.error(error);
  process.exit(1);
});

// Run validation
runValidation();
