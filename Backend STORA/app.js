const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
const path = require('path');

const logger = require('./src/middleware/logger');
const errorHandler = require('./src/middleware/errorHandler');

const apiRoutes = require('./src/routes');

const { connectDB } = require('./config/db');
require('./src/models');

const { initializeFirebase } = require('./src/services/firebaseAdmin');
const reminderScheduler = require('./src/services/reminderScheduler');

const app = express();

connectDB();

initializeFirebase();

reminderScheduler.start();

app.use(helmet());

const corsOptions = {
  origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'],
  credentials: true,
  optionsSuccessStatus: 200,
};
app.use(cors(corsOptions));

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: process.env.NODE_ENV === 'production' ? 100 : 1000,
  message: {
    success: false,
    message: 'Too many requests from this IP, please try again later.',
  },
});
app.use('/api', limiter);

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

app.use(compression());

app.use(logger);

app.use(express.static(path.join(__dirname, 'public')));

app.use('/api/v1', apiRoutes);

app.get('/', (req, res) => {
  res.json({
    success: true,
    message: 'Welcome to STORA API',
    version: '1.0.0',
    documentation: '/api/v1/health',
  });
});

app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found',
  });
});

app.use(errorHandler);

module.exports = app;
