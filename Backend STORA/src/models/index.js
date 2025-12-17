const { sequelize } = require('../../config/db');

// Import all models
const User = require('./User');
const Inventaris = require('./Inventaris');
const FotoInventaris = require('./FotoInventaris');
const Peminjaman = require('./Peminjaman');
const PeminjamanBarang = require('./PeminjamanBarang');
const FotoPeminjaman = require('./FotoPeminjaman');
const Notifikasi = require('./Notifikasi');
const ReminderSetting = require('./ReminderSetting');
const UserDevice = require('./UserDevice');

// Define associations

// User <-> Inventaris (One-to-Many)
User.hasMany(Inventaris, {
  foreignKey: 'ID_User',
  as: 'inventaris',
});
Inventaris.belongsTo(User, {
  foreignKey: 'ID_User',
  as: 'user',
});

// Inventaris <-> FotoInventaris (One-to-Many)
Inventaris.hasMany(FotoInventaris, {
  foreignKey: 'ID_Inventaris',
  as: 'foto',
});
FotoInventaris.belongsTo(Inventaris, {
  foreignKey: 'ID_Inventaris',
  as: 'inventaris',
});

// User <-> Peminjaman (One-to-Many)
User.hasMany(Peminjaman, {
  foreignKey: 'ID_User',
  as: 'peminjaman',
});
Peminjaman.belongsTo(User, {
  foreignKey: 'ID_User',
  as: 'user',
});

// Peminjaman <-> PeminjamanBarang (One-to-Many)
Peminjaman.hasMany(PeminjamanBarang, {
  foreignKey: 'ID_Peminjaman',
  as: 'barang',
});
PeminjamanBarang.belongsTo(Peminjaman, {
  foreignKey: 'ID_Peminjaman',
  as: 'peminjaman',
});

// Inventaris <-> PeminjamanBarang (One-to-Many)
Inventaris.hasMany(PeminjamanBarang, {
  foreignKey: 'ID_Inventaris',
  as: 'peminjaman_barang',
});
PeminjamanBarang.belongsTo(Inventaris, {
  foreignKey: 'ID_Inventaris',
  as: 'inventaris',
});

// PeminjamanBarang <-> FotoPeminjaman (One-to-Many)
PeminjamanBarang.hasMany(FotoPeminjaman, {
  foreignKey: 'ID_Peminjaman_Barang',
  as: 'foto',
});
FotoPeminjaman.belongsTo(PeminjamanBarang, {
  foreignKey: 'ID_Peminjaman_Barang',
  as: 'peminjaman_barang',
});

// User <-> Notifikasi (One-to-Many)
User.hasMany(Notifikasi, {
  foreignKey: 'ID_User',
  as: 'notifikasi',
});
Notifikasi.belongsTo(User, {
  foreignKey: 'ID_User',
  as: 'user',
});

// User <-> ReminderSetting (One-to-Many)
User.hasMany(ReminderSetting, {
  foreignKey: 'ID_User',
  as: 'reminder_settings',
});
ReminderSetting.belongsTo(User, {
  foreignKey: 'ID_User',
  as: 'user',
});

// User <-> UserDevice (One-to-Many)
User.hasMany(UserDevice, {
  foreignKey: 'ID_User',
  as: 'devices',
});
UserDevice.belongsTo(User, {
  foreignKey: 'ID_User',
  as: 'user',
});

// Peminjaman <-> Notifikasi (One-to-Many)
Peminjaman.hasMany(Notifikasi, {
  foreignKey: 'ID_Peminjaman',
  as: 'notifikasi',
});
Notifikasi.belongsTo(Peminjaman, {
  foreignKey: 'ID_Peminjaman',
  as: 'peminjaman',
});

// Export all models and sequelize instance
module.exports = {
  sequelize,
  User,
  Inventaris,
  FotoInventaris,
  Peminjaman,
  PeminjamanBarang,
  FotoPeminjaman,
  Notifikasi,
  ReminderSetting,
  UserDevice,
};

