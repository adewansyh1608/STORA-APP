module.exports = {
  // Application settings
  app: {
    name: 'STORA API',
    version: '1.0.0',
    description: 'STORA Backend API Server',
    port: process.env.PORT || 3000,
    env: process.env.NODE_ENV || 'development'
  },

  // Security settings
  security: {
    jwt: {
      secret: process.env.JWT_SECRET || 'your-fallback-secret-key',
      expiresIn: process.env.JWT_EXPIRE || '7d',
      issuer: 'stora-api',
      audience: 'stora-client'
    },
    bcrypt: {
      saltRounds: 12
    },
    cors: {
      origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'],
      credentials: true,
      optionsSuccessStatus: 200
    },
    rateLimit: {
      windowMs: 15 * 60 * 1000,
      max: process.env.NODE_ENV === 'production' ? 100 : 1000,
      message: {
        success: false,
        message: 'Too many requests from this IP, please try again later.'
      }
    }
  },

  // File upload settings
  upload: {
    maxFileSize: parseInt(process.env.MAX_FILE_SIZE) || 10 * 1024 * 1024, // 10MB
    allowedTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
    uploadPath: process.env.UPLOAD_PATH || './public/uploads'
  },

  // Email settings
  email: {
    host: process.env.EMAIL_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.EMAIL_PORT) || 587,
    secure: false,
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASS
    }
  },

  // Pagination defaults
  pagination: {
    defaultPage: 1,
    defaultLimit: 10,
    maxLimit: 100
  },

  // Logging settings
  logging: {
    level: process.env.LOG_LEVEL || 'info',
    format: process.env.NODE_ENV === 'production' ? 'combined' : 'dev'
  }
};
