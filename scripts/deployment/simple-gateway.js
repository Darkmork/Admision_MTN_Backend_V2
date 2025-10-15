const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');

const app = express();
const PORT = 8080;

// Enable CORS for all routes
app.use(cors({
    origin: ['http://localhost:5173', 'http://localhost:5174', 'http://localhost:5175', 'http://localhost:3000'],
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'x-correlation-id', 'x-request-time', 'x-timezone', 'x-client-type', 'x-client-version', 'x-csrf-token'],
    optionsSuccessStatus: 204
}));

// Handle preflight requests - this is handled by the cors middleware above

// Gateway health check
app.get('/health', (req, res) => {
    res.json({
        status: 'UP',
        service: 'simple-gateway',
        port: PORT,
        timestamp: new Date().toISOString()
    });
});

app.get('/gateway/status', (req, res) => {
    res.json({
        status: 'UP',
        message: 'Simple Node.js Gateway is running',
        services: {
            'user-service': 'http://localhost:8082',
            'application-service': 'http://localhost:8083',
            'evaluation-service': 'http://localhost:8084',
            'notification-service': 'http://localhost:8085'
        },
        timestamp: new Date().toISOString()
    });
});

// Proxy routes
const proxyOptions = {
    changeOrigin: true,
    ws: true,
    timeout: 30000,
    onError: (err, req, res) => {
        console.error('Proxy error:', err.message);
        res.status(500).json({ error: 'Gateway error', message: err.message });
    },
    onProxyReq: (proxyReq, req, res) => {
        console.log(`🌐 Gateway: ${req.method} ${req.url} -> ${proxyReq.path}`);
    }
};

// User service routes (auth, users) - preserve full path by rewriting from stripped to full
app.use('/api/auth', createProxyMiddleware({
    target: 'http://localhost:8082',
    pathRewrite: {
        '^/': '/api/auth/'  // When Express strips /api/auth, add it back
    },
    changeOrigin: true,
    ws: true,
    timeout: 30000,
    onError: (err, req, res) => {
        console.error('Proxy error:', err.message);
        res.status(500).json({ error: 'Gateway error', message: err.message });
    },
    onProxyReq: (proxyReq, req, res) => {
        console.log(`🌐 Gateway: ${req.method} ${req.url} -> Target: ${proxyReq.path}`);
    }
}));

app.use('/api/users', createProxyMiddleware({
    target: 'http://localhost:8082',
    ...proxyOptions
}));

// Application service routes
app.use('/api/applications', createProxyMiddleware({
    target: 'http://localhost:8083',
    ...proxyOptions
}));

app.use('/api/documents', createProxyMiddleware({
    target: 'http://localhost:8083',
    ...proxyOptions
}));

// Evaluation service routes
app.use('/api/evaluations', createProxyMiddleware({
    target: 'http://localhost:8084',
    ...proxyOptions
}));

app.use('/api/interviews', createProxyMiddleware({
    target: 'http://localhost:8084',
    ...proxyOptions
}));

app.use('/api/schedules', createProxyMiddleware({
    target: 'http://localhost:8084',
    ...proxyOptions
}));

// Notification service routes
app.use('/api/notifications', createProxyMiddleware({
    target: 'http://localhost:8085',
    ...proxyOptions
}));

app.use('/api/email', createProxyMiddleware({
    target: 'http://localhost:8085',
    ...proxyOptions
}));

// Dashboard and other routes default to user service
app.use('/api/dashboard', createProxyMiddleware({
    target: 'http://localhost:8082',
    ...proxyOptions
}));

// Guardian/Apoderado routes
app.use('/api/guardians', createProxyMiddleware({
    target: 'http://localhost:8087', // Guardian service
    ...proxyOptions
}));

// RUT validation
app.use('/api/rut', createProxyMiddleware({
    target: 'http://localhost:8082',
    ...proxyOptions
}));

// Catch-all for any other API routes - send to user service
app.use('/api', createProxyMiddleware({
    target: 'http://localhost:8082',
    ...proxyOptions
}));

// Error handling
app.use((err, req, res, next) => {
    console.error('Gateway error:', err);
    res.status(500).json({ error: 'Gateway error', message: err.message });
});

app.listen(PORT, () => {
    console.log(`🌐 Simple Gateway running on port ${PORT}`);
    console.log(`🔗 Frontend should connect to: http://localhost:${PORT}`);
    console.log(`📊 Health check: http://localhost:${PORT}/health`);
    console.log(`📊 Gateway status: http://localhost:${PORT}/gateway/status`);
    console.log('\n🎯 Service routing:');
    console.log('  • /api/auth/* -> User Service (8082)');
    console.log('  • /api/users/* -> User Service (8082)');
    console.log('  • /api/applications/* -> Application Service (8083)');
    console.log('  • /api/evaluations/* -> Evaluation Service (8084)');
    console.log('  • /api/notifications/* -> Notification Service (8085)');
});