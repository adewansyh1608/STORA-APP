# STORA Backend API

A robust Express.js backend API server with modern architecture and best practices.

## 🚀 Features

- **RESTful API** with Express.js
- **MongoDB** integration with Mongoose
- **JWT Authentication** with secure middleware
- **Input Validation** with express-validator
- **Error Handling** with custom error middleware
- **Logging** with Morgan
- **Security** with Helmet, CORS, and Rate Limiting
- **File Upload** support
- **Environment Configuration**
- **Modular Architecture** with separation of concerns

## 📁 Project Structure

```
STORA/
├── src/
│   ├── controllers/     # Request handlers
│   ├── routes/         # API routes
│   ├── middleware/     # Custom middleware
│   ├── models/         # Database models
│   ├── services/       # Business logic
│   └── utils/          # Utility functions
├── config/             # Configuration files
├── public/             # Static files
├── logs/              # Application logs
├── tests/             # Test files
├── app.js             # Express app setup
├── server.js          # Server entry point
└── package.json       # Dependencies
```

## 🛠️ Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Backend\ STORA
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Set up environment variables**
   ```bash
   cp .env.example .env
   ```
   Edit `.env` file with your configuration.

4. **Start MongoDB**
   Make sure MongoDB is running on your system.

5. **Run the application**
   ```bash
   # Development mode
   npm run dev

   # Production mode
   npm start
   ```

## 📦 Dependencies

### Production Dependencies
```bash
npm install express mongoose cors helmet compression express-rate-limit
npm install express-validator bcryptjs jsonwebtoken morgan dotenv
```

### Development Dependencies
```bash
npm install --save-dev nodemon jest supertest eslint prettier
```

## 🔧 Environment Variables

Create a `.env` file in the root directory:

```env
NODE_ENV=development
PORT=5000
MONGODB_URI=mongodb://localhost:27017/stora_db
JWT_SECRET=your-super-secret-jwt-key-here
JWT_EXPIRE=7d
ALLOWED_ORIGINS=http://localhost:3000
```

## 🚦 API Endpoints

### Health Check
- `GET /api/v1/health` - API health status

### Users
- `GET /api/v1/users` - Get all users
- `GET /api/v1/users/:id` - Get user by ID
- `POST /api/v1/users` - Create new user
- `PUT /api/v1/users/:id` - Update user
- `DELETE /api/v1/users/:id` - Delete user

## 🧪 Testing

```bash
# Run tests
npm test

# Run tests with coverage
npm run test:coverage
```

## 📝 Scripts

```bash
npm start          # Start production server
npm run dev        # Start development server with nodemon
npm test           # Run tests
npm run lint       # Run ESLint
npm run format     # Format code with Prettier
```

## 🔒 Security Features

- **Helmet.js** for security headers
- **CORS** configuration
- **Rate limiting** to prevent abuse
- **JWT** authentication
- **Password hashing** with bcrypt
- **Input validation** and sanitization

## 📊 Logging

- **Morgan** for HTTP request logging
- **Custom error logging**
- **Separate log files** for different environments

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

If you have any questions or need help, please open an issue in the repository.
