# PostgreSQL SSL/TLS Configuration Guide
## Sistema de Admisión MTN

**Status**: ✅ **SSL/TLS ENABLED**
**Date**: October 12, 2025
**PostgreSQL Version**: 15

---

## Table of Contents
- [Overview](#overview)
- [Configuration Details](#configuration-details)
- [Certificates](#certificates)
- [Node.js Connection Configuration](#nodejs-connection-configuration)
- [Verification](#verification)
- [Troubleshooting](#troubleshooting)
- [Security Best Practices](#security-best-practices)
- [Railway Production Deployment](#railway-production-deployment)
- [Maintenance](#maintenance)

---

## Overview

This guide documents the SSL/TLS configuration for PostgreSQL in the Sistema de Admisión MTN. SSL/TLS encryption protects data in transit between the application services and the database, which is critical for production environments.

### Why SSL/TLS?

- **Data Protection**: Encrypts sensitive data (credentials, personal information, etc.)
- **Compliance**: Required for PCI DSS, GDPR, and other regulations
- **Man-in-the-Middle Protection**: Prevents eavesdropping and tampering
- **Production Requirement**: Essential for deployed applications

---

## Configuration Details

### PostgreSQL Version & Paths

- **Version**: PostgreSQL 15 (Homebrew)
- **Data Directory**: `/opt/homebrew/var/postgresql@15`
- **Config File**: `/opt/homebrew/var/postgresql@15/postgresql.conf`
- **Certificates**: `/opt/homebrew/var/postgresql@15/`

###  SSL Settings (postgresql.conf)

```ini
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'
ssl_ciphers = 'HIGH:MEDIUM:+3DES:!aNULL'
ssl_prefer_server_ciphers = on
ssl_min_protocol_version = 'TLSv1.2'
```

**Key Configuration Points:**
- ✅ SSL enabled
- ✅ TLS 1.2 minimum (disables older protocols)
- ✅ Strong cipher suites only
- ✅ Server-preferred cipher order

---

## Certificates

### Current Setup (Development)

**Type**: Self-signed
**Validity**: 365 days (generated October 12, 2025)
**Common Name (CN)**: localhost

**Location**:
- Certificate: `/opt/homebrew/var/postgresql@15/server.crt`
- Private Key: `/opt/homebrew/var/postgresql@15/server.key`

**Permissions**:
```bash
-rw-r--r--  server.crt  (644)
-rw-------  server.key  (600)  # Must be 600 or PostgreSQL won't start
```

### Certificate Generation (Reference)

```bash
cd /opt/homebrew/var/postgresql@15/

# Generate self-signed certificate
openssl req -new -x509 -days 365 -nodes -text \
  -out server.crt \
  -keyout server.key \
  -subj "/CN=localhost"

# Set permissions
chmod 600 server.key
chmod 644 server.crt

# Remove extended attributes (macOS)
xattr -c server.crt server.key

# Restart PostgreSQL
brew services restart postgresql@15
```

### Production Certificates

For production, replace with **CA-signed certificates** from a trusted Certificate Authority like:
- Let's Encrypt (free, automated)
- DigiCert
- GlobalSign
- Your organization's internal CA

---

## Node.js Connection Configuration

### Development (Self-Signed Certificates)

For local development with self-signed certificates, use `rejectUnauthorized: false`:

```javascript
const { Pool } = require('pg');

const dbPool = new Pool({
  host: 'localhost',
  port: 5432,
  user: 'admin',
  password: 'admin123',
  database: 'Admisión_MTN_DB',

  // SSL Configuration
  ssl: {
    rejectUnauthorized: false  // Accept self-signed certificates
  },

  // Connection Pooling
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
});

// Log connection status
dbPool.on('connect', (client) => {
  console.log('✅ Connected to PostgreSQL with SSL');
});

dbPool.on('error', (err, client) => {
  console.error('❌ PostgreSQL connection error:', err.message);
});
```

### Production (CA-Signed Certificates)

For production with proper CA-signed certificates, use `rejectUnauthorized: true`:

```javascript
const { Pool } = require('pg');
const fs = require('fs');

const dbPool = new Pool({
  host: process.env.DB_HOST || 'production-db.example.com',
  port: process.env.DB_PORT || 5432,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,

  // SSL Configuration
  ssl: {
    rejectUnauthorized: true,  // Verify certificates
    ca: fs.readFileSync('/path/to/ca-certificate.crt').toString(),
    key: fs.readFileSync('/path/to/client-key.key').toString(),
    cert: fs.readFileSync('/path/to/client-cert.crt').toString()
  },

  // Connection Pooling
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
});
```

### Environment-Based Configuration

Use environment variables to switch between development and production:

```javascript
const { Pool } = require('pg');
const fs = require('fs');

const sslConfig = process.env.NODE_ENV === 'production'
  ? {
      rejectUnauthorized: true,
      ca: fs.readFileSync(process.env.DB_SSL_CA).toString(),
      key: fs.readFileSync(process.env.DB_SSL_KEY).toString(),
      cert: fs.readFileSync(process.env.DB_SSL_CERT).toString()
    }
  : {
      rejectUnauthorized: false  // Development only
    };

const dbPool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  user: process.env.DB_USER || 'admin',
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME || 'Admisión_MTN_DB',
  ssl: sslConfig,
  max: 20
});
```

---

## Verification

### Check SSL Status

```bash
# Using psql
psql -U $(whoami) -d postgres -c "SHOW ssl;"
# Expected output: ssl | on

# View all SSL settings
psql -U $(whoami) -d postgres -c "
  SELECT name, setting
  FROM pg_settings
  WHERE name LIKE 'ssl%'
  ORDER BY name;"
```

### Test SSL Connection

```bash
# Connect with SSL required
psql "postgresql://admin:admin123@localhost:5432/Admisión_MTN_DB?sslmode=require"

# Connect with SSL verification
psql "postgresql://admin:admin123@localhost:5432/Admisión_MTN_DB?sslmode=verify-full"
```

### Check Certificate Details

```bash
# View certificate info
openssl x509 -in /opt/homebrew/var/postgresql@15/server.crt -text -noout

# Check expiration date
openssl x509 -in /opt/homebrew/var/postgresql@15/server.crt -noout -dates
```

### Node.js Connection Test

Create a test script `test-ssl-connection.js`:

```javascript
const { Pool } = require('pg');

const pool = new Pool({
  host: 'localhost',
  port: 5432,
  user: 'admin',
  password: 'admin123',
  database: 'Admisión_MTN_DB',
  ssl: { rejectUnauthorized: false }
});

async function testSSL() {
  try {
    const client = await pool.connect();

    // Check SSL status
    const sslResult = await client.query('SHOW ssl');
    console.log('✅ SSL Status:', sslResult.rows[0].ssl);

    // Get connection info
    const connResult = await client.query(`
      SELECT
        inet_server_addr() as server_ip,
        inet_server_port() as server_port,
        version() as pg_version
    `);
    console.log('✅ Connection Details:', connResult.rows[0]);

    client.release();
    await pool.end();

    console.log('✅ SSL connection test successful!');
  } catch (err) {
    console.error('❌ SSL connection test failed:', err.message);
    process.exit(1);
  }
}

testSSL();
```

Run the test:
```bash
node test-ssl-connection.js
```

---

## Troubleshooting

### SSL Not Enabled After Restart

**Symptoms**: `SHOW ssl;` returns `off`

**Solutions**:

1. **Check PostgreSQL logs**:
   ```bash
   tail -50 /opt/homebrew/var/log/postgresql@15.log
   ```

2. **Verify certificate permissions**:
   ```bash
   ls -la /opt/homebrew/var/postgresql@15/server.*
   ```
   - `server.key` MUST be `-rw-------` (600)
   - `server.crt` should be `-rw-r--r--` (644)

3. **Fix permissions**:
   ```bash
   chmod 600 /opt/homebrew/var/postgresql@15/server.key
   chmod 644 /opt/homebrew/var/postgresql@15/server.crt
   ```

4. **Restart PostgreSQL**:
   ```bash
   brew services restart postgresql@15
   ```

### Connection Refused Errors

**Symptoms**: `connection refused` or `could not connect to server`

**Solutions**:

1. **Check if PostgreSQL is running**:
   ```bash
   brew services list | grep postgresql
   ```

2. **Check port availability**:
   ```bash
   lsof -i :5432
   ```

3. **Verify pg_hba.conf allows SSL connections**:
   ```bash
   grep ssl /opt/homebrew/var/postgresql@15/pg_hba.conf
   ```

### Certificate Errors in Production

**Symptoms**: `SSL certificate verify failed` or `certificate signed by unknown authority`

**Solutions**:

1. **Verify certificate chain is complete**:
   ```bash
   openssl verify -CAfile ca-bundle.crt server.crt
   ```

2. **Check certificate expiration**:
   ```bash
   openssl x509 -in server.crt -noout -dates
   ```

3. **Validate certificate matches hostname**:
   ```bash
   openssl x509 -in server.crt -noout -subject
   ```

### Node.js Connection Errors

**Error**: `self signed certificate`

**Solution**: For development only, use `rejectUnauthorized: false`. For production, use proper CA-signed certificates.

**Error**: `no pg_hba.conf entry for host`

**Solution**: Update `/opt/homebrew/var/postgresql@15/pg_hba.conf`:
```
# TYPE  DATABASE        USER            ADDRESS                 METHOD
hostssl all             all             0.0.0.0/0               md5
```

---

## Security Best Practices

### Development Environment

✅ **Recommended**:
- Self-signed certificates are acceptable
- Use `sslmode=require` minimum
- Keep `rejectUnauthorized: false` only in dev

❌ **Not Recommended**:
- Never commit certificates to version control
- Don't share private keys
- Don't use weak cipher suites

### Production Environment

✅ **Required**:
- Use CA-signed certificates from trusted authority
- Set `sslmode=verify-full`
- Enable `rejectUnauthorized: true`
- Implement certificate rotation policy
- Use environment variables for secrets
- Enable client certificate authentication
- Monitor certificate expiration dates
- Use strong cipher suites (TLS 1.2+)

❌ **Forbidden**:
- Never use `rejectUnauthorized: false` in production
- Never use self-signed certificates in production
- Don't use SSLv3 or TLS 1.0/1.1
- Don't hardcode credentials

### Certificate Management

**Rotation Schedule**:
- Development: 365 days
- Production: 90 days (recommended)

**Storage**:
- Store private keys in secure vaults (AWS Secrets Manager, HashiCorp Vault, etc.)
- Never commit certificates to git
- Use `.gitignore` to exclude certificate files:
  ```
  *.crt
  *.key
  *.pem
  ```

**Cipher Suites**:
Use strong cipher suites only:
```ini
ssl_ciphers = 'ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!3DES'
```

---

## Railway Production Deployment

Railway automatically configures PostgreSQL with SSL. Update your services to use the provided `DATABASE_URL`:

### Railway Environment Variables

Railway provides these environment variables automatically:
- `DATABASE_URL` - Full connection string with SSL
- `DB_HOST` - Database hostname
- `DB_PORT` - Database port (usually 5432)
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password
- `DB_NAME` - Database name

### Railway Connection Configuration

```javascript
const { Pool } = require('pg');

// Railway provides DATABASE_URL with SSL configured
const dbPool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false  // Railway uses self-signed certificates
  },
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000
});
```

**Note**: Railway PostgreSQL uses self-signed certificates, so `rejectUnauthorized: false` is required.

### Alternative Configuration

If you prefer individual environment variables:

```javascript
const { Pool } = require('pg');

const dbPool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT || 5432,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: {
    rejectUnauthorized: false
  },
  max: 20
});
```

---

## Maintenance

### Certificate Renewal

Certificates expire after their validity period. To renew:

```bash
# Navigate to PostgreSQL data directory
cd /opt/homebrew/var/postgresql@15/

# Backup old certificates
mkdir -p ~/backups/postgres-ssl/
cp server.crt server.key ~/backups/postgres-ssl/

# Generate new certificate (valid for 365 days)
openssl req -new -x509 -days 365 -nodes -text \
  -out server.crt \
  -keyout server.key \
  -subj "/CN=localhost"

# Set permissions
chmod 600 server.key
chmod 644 server.crt

# Restart PostgreSQL
brew services restart postgresql@15

# Verify SSL is still enabled
psql -U $(whoami) -d postgres -c "SHOW ssl;"
```

### Automated Certificate Renewal Script

Create `/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend/renew-ssl-cert.sh`:

```bash
#!/bin/bash

PG_DATA_DIR="/opt/homebrew/var/postgresql@15"
BACKUP_DIR="$HOME/backups/postgres-ssl"

# Create backup
mkdir -p "$BACKUP_DIR"
cp "$PG_DATA_DIR/server.crt" "$BACKUP_DIR/server.crt.$(date +%Y%m%d)"
cp "$PG_DATA_DIR/server.key" "$BACKUP_DIR/server.key.$(date +%Y%m%d)"

# Generate new certificate
cd "$PG_DATA_DIR"
openssl req -new -x509 -days 365 -nodes -text \
  -out server.crt \
  -keyout server.key \
  -subj "/CN=localhost"

# Set permissions
chmod 600 server.key
chmod 644 server.crt

# Restart PostgreSQL
brew services restart postgresql@15

echo "✅ Certificate renewed successfully"
```

Make it executable:
```bash
chmod +x renew-ssl-cert.sh
```

### Backup Certificates

Regular backups are essential:

```bash
# Backup to local directory
cp /opt/homebrew/var/postgresql@15/server.crt ~/backups/
cp /opt/homebrew/var/postgresql@15/server.key ~/backups/

# Backup with timestamp
BACKUP_DIR="$HOME/backups/postgres-ssl/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"
cp /opt/homebrew/var/postgresql@15/server.* "$BACKUP_DIR/"
```

### Monitor Certificate Expiration

Create a monitoring script `check-cert-expiration.sh`:

```bash
#!/bin/bash

CERT_FILE="/opt/homebrew/var/postgresql@15/server.crt"
WARN_DAYS=30

# Get expiration date
EXPIRY=$(openssl x509 -in "$CERT_FILE" -noout -enddate | cut -d= -f2)
EXPIRY_EPOCH=$(date -j -f "%b %d %H:%M:%S %Y %Z" "$EXPIRY" "+%s")
NOW_EPOCH=$(date +%s)
DAYS_REMAINING=$(( ($EXPIRY_EPOCH - $NOW_EPOCH) / 86400 ))

if [ $DAYS_REMAINING -lt 0 ]; then
  echo "❌ Certificate EXPIRED $((0 - $DAYS_REMAINING)) days ago"
  exit 1
elif [ $DAYS_REMAINING -lt $WARN_DAYS ]; then
  echo "⚠️  Certificate expires in $DAYS_REMAINING days"
  exit 1
else
  echo "✅ Certificate valid for $DAYS_REMAINING days"
fi
```

Add to cron for daily checks:
```bash
# Run daily at 9am
0 9 * * * /path/to/check-cert-expiration.sh
```

---

## References

- [PostgreSQL SSL Documentation](https://www.postgresql.org/docs/current/ssl-tcp.html)
- [Node.js pg SSL Configuration](https://node-postgres.com/features/ssl)
- [OpenSSL Certificate Commands](https://www.openssl.org/docs/man1.1.1/man1/openssl-req.html)
- [Let's Encrypt (Free CA)](https://letsencrypt.org/)
- [Railway PostgreSQL SSL](https://docs.railway.app/databases/postgresql)

---

## Support & Troubleshooting

For issues or questions:

1. **Check PostgreSQL logs**:
   ```bash
   tail -50 /opt/homebrew/var/log/postgresql@15.log
   ```

2. **Run automated SSL configuration script**:
   ```bash
   ./enable-postgresql-ssl.sh
   ```

3. **Review CLAUDE.md** for architecture details

4. **Test connection**:
   ```bash
   node test-ssl-connection.js
   ```

---

**Last Updated**: October 12, 2025
**PostgreSQL Version**: 15
**SSL Status**: ✅ ENABLED
**Certificate Type**: Self-signed (Development)
**Certificate Validity**: 365 days
