const jwt = require('jsonwebtoken');

const authMiddleware = (req, res, next) => {
  try {
    console.log('===== AUTH MIDDLEWARE =====');
    console.log('Request URL:', req.originalUrl);
    console.log('Request Method:', req.method);
    console.log('Authorization Header:', req.header('Authorization'));

    const token = req.header('Authorization')?.replace('Bearer ', '');

    if (!token) {
      console.log('✗ No token provided');
      console.log('============================');
      return res.status(401).json({
        success: false,
        message: 'Access denied. No token provided.',
      });
    }

    console.log('Token:', token.substring(0, 20) + '...');

    const decoded = jwt.verify(
      token,
      process.env.JWT_SECRET || 'your-secret-key'
    );
    req.user = decoded;

    console.log('✓ Token decoded successfully');
    console.log('User ID:', decoded.id);
    console.log('User Email:', decoded.email);
    console.log('============================');

    next();
  } catch (error) {
    console.log('✗ Token verification failed:', error.message);
    console.log('============================');
    res.status(401).json({
      success: false,
      message: 'Invalid token.',
    });
  }
};

module.exports = authMiddleware;
