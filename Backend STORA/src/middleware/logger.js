const logger = (req, res, next) => {
  const timestamp = new Date().toISOString();
  const method = req.method;
  const url = req.url;
  const ip = req.ip || req.connection.remoteAddress;

  console.log(`[${timestamp}] ${method} ${url} - ${ip}`);

  // Log response
  const originalSend = res.send;
  res.send = function (data) {
    console.log(
      `[${timestamp}] RESPONSE ${method} ${url} - Status: ${res.statusCode}`
    );
    if (res.statusCode >= 400) {
      console.log(`[${timestamp}] ERROR RESPONSE:`, data);
    }
    originalSend.call(this, data);
  };

  next();
};

module.exports = logger;
