const multer = require('multer');
const path = require('path');
const fs = require('fs');

// Ensure uploads directories exist
const uploadsDir = path.join(__dirname, '../../public/uploads/inventaris');
const profileUploadsDir = path.join(__dirname, '../../public/uploads/profiles');
const peminjamanUploadsDir = path.join(__dirname, '../../public/uploads/peminjaman');

if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}
if (!fs.existsSync(profileUploadsDir)) {
  fs.mkdirSync(profileUploadsDir, { recursive: true });
}
if (!fs.existsSync(peminjamanUploadsDir)) {
  fs.mkdirSync(peminjamanUploadsDir, { recursive: true });
}

// Configure storage for inventory
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, uploadsDir);
  },
  filename: function (req, file, cb) {
    // Generate unique filename: timestamp-random-originalname
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname);
    const nameWithoutExt = path.basename(file.originalname, ext);
    cb(null, `${nameWithoutExt}-${uniqueSuffix}${ext}`);
  }
});

// Configure storage for profile photos
const profileStorage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, profileUploadsDir);
  },
  filename: function (req, file, cb) {
    // Generate unique filename with user ID if available
    const userId = req.user?.id || 'unknown';
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `profile-${userId}-${uniqueSuffix}${ext}`);
  }
});

// Configure storage for peminjaman photos
const peminjamanStorage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, peminjamanUploadsDir);
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `peminjaman-${uniqueSuffix}${ext}`);
  }
});

// File filter - only accept images
const fileFilter = (req, file, cb) => {
  console.log('=== FILE UPLOAD DEBUG ===');
  console.log('Original filename:', file.originalname);
  console.log('MIME type:', file.mimetype);
  console.log('Field name:', file.fieldname);

  const allowedExtensions = /\.(jpeg|jpg|png|gif|webp)$/i;
  const allowedMimeTypes = /^image\/(jpeg|jpg|png|gif|webp)$/i;

  // Check file extension
  const hasValidExtension = allowedExtensions.test(file.originalname);

  // Check MIME type - also allow 'image/*' and 'application/octet-stream' from Android
  const hasValidMimetype = allowedMimeTypes.test(file.mimetype) ||
    file.mimetype === 'image/*' ||
    file.mimetype.startsWith('image/') ||
    file.mimetype === 'application/octet-stream';

  console.log('Valid extension:', hasValidExtension);
  console.log('Valid MIME type:', hasValidMimetype);
  console.log('=========================');

  // Accept if either extension OR mimetype is valid (more flexible for Android)
  if (hasValidExtension || hasValidMimetype) {
    return cb(null, true);
  } else {
    cb(new Error(`Only image files are allowed. Received: ${file.originalname} (${file.mimetype})`));
  }
};


// Configure multer for inventory
const upload = multer({
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB max file size
  },
  fileFilter: fileFilter
});

// Configure multer for profile photos
const profileUpload = multer({
  storage: profileStorage,
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB max file size
  },
  fileFilter: fileFilter
});

// Configure multer for peminjaman photos
const peminjamanUpload = multer({
  storage: peminjamanStorage,
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB max file size
  },
  fileFilter: fileFilter
});

module.exports = { upload, profileUpload, peminjamanUpload };

