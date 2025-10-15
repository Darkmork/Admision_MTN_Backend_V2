#!/bin/bash

##############################################################################
# PostgreSQL SSL Configuration Script
# Sistema de Admisión MTN
#
# Este script automatiza la configuración de SSL/TLS para PostgreSQL
# en entorno de desarrollo y producción.
#
# Requisitos:
# - PostgreSQL 15 instalado (Homebrew en macOS)
# - Permisos de ejecución (chmod +x enable-postgresql-ssl.sh)
#
# Uso:
#   ./enable-postgresql-ssl.sh
#
# El script realiza:
# 1. Detección de la versión de PostgreSQL activa
# 2. Generación de certificados SSL autofirmados
# 3. Configuración de postgresql.conf para SSL
# 4. Reinicio del servicio PostgreSQL
# 5. Verificación del estado SSL
##############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ ${NC}$1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
}

# Detect PostgreSQL version and data directory
detect_postgres() {
    print_header "Detecting PostgreSQL Installation"

    # Check for PostgreSQL 15
    if [ -d "/opt/homebrew/var/postgresql@15" ]; then
        PG_VERSION="15"
        PG_DATA_DIR="/opt/homebrew/var/postgresql@15"
        PG_BIN_DIR="/opt/homebrew/opt/postgresql@15/bin"
        print_success "Found PostgreSQL $PG_VERSION"
    # Check for PostgreSQL 14
    elif [ -d "/opt/homebrew/var/postgresql@14" ]; then
        PG_VERSION="14"
        PG_DATA_DIR="/opt/homebrew/var/postgresql@14"
        PG_BIN_DIR="/opt/homebrew/opt/postgresql@14/bin"
        print_success "Found PostgreSQL $PG_VERSION"
    else
        print_error "PostgreSQL installation not found in expected locations"
        exit 1
    fi

    print_info "Data directory: $PG_DATA_DIR"
    print_info "Binary directory: $PG_BIN_DIR"
}

# Generate SSL certificates
generate_certificates() {
    print_header "Generating SSL Certificates"

    cd "$PG_DATA_DIR"

    if [ -f "server.crt" ] && [ -f "server.key" ]; then
        print_warning "SSL certificates already exist"
        read -p "Do you want to regenerate them? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "Using existing certificates"
            return
        fi
        print_info "Removing old certificates..."
        rm -f server.crt server.key
    fi

    print_info "Generating self-signed certificate (valid for 365 days)..."
    openssl req -new -x509 -days 365 -nodes -text \
        -out server.crt \
        -keyout server.key \
        -subj "/CN=localhost" \
        2>/dev/null

    print_info "Setting correct permissions..."
    chmod 600 server.key
    chmod 644 server.crt

    # Remove extended attributes if on macOS
    if command -v xattr &> /dev/null; then
        xattr -c server.crt 2>/dev/null || true
        xattr -c server.key 2>/dev/null || true
    fi

    print_success "SSL certificates generated successfully"
    print_info "Certificate: $PG_DATA_DIR/server.crt"
    print_info "Private key: $PG_DATA_DIR/server.key"
}

# Configure postgresql.conf for SSL
configure_postgresql() {
    print_header "Configuring PostgreSQL for SSL"

    CONF_FILE="$PG_DATA_DIR/postgresql.conf"
    BACKUP_FILE="$PG_DATA_DIR/postgresql.conf.backup.$(date +%Y%m%d_%H%M%S)"

    print_info "Creating backup: $BACKUP_FILE"
    cp "$CONF_FILE" "$BACKUP_FILE"

    print_info "Updating SSL configuration..."

    # Use sed to enable SSL settings
    sed -i.tmp 's/^#ssl = off/ssl = on/' "$CONF_FILE"
    sed -i.tmp 's/^#ssl_cert_file = .*/ssl_cert_file = '\''server.crt'\''/' "$CONF_FILE"
    sed -i.tmp 's/^#ssl_key_file = .*/ssl_key_file = '\''server.key'\''/' "$CONF_FILE"
    sed -i.tmp 's/^#ssl_ciphers = .*/ssl_ciphers = '\''HIGH:MEDIUM:+3DES:!aNULL'\''/' "$CONF_FILE"
    sed -i.tmp 's/^#ssl_prefer_server_ciphers = .*/ssl_prefer_server_ciphers = on/' "$CONF_FILE"
    sed -i.tmp 's/^#ssl_min_protocol_version = .*/ssl_min_protocol_version = '\''TLSv1.2'\''/' "$CONF_FILE"

    # Clean up temporary file
    rm -f "$CONF_FILE.tmp"

    print_success "PostgreSQL configuration updated"
    print_info "Backup saved: $BACKUP_FILE"
}

# Restart PostgreSQL service
restart_postgresql() {
    print_header "Restarting PostgreSQL Service"

    print_info "Stopping all PostgreSQL processes..."
    pkill -9 postgres || true
    sleep 2

    print_info "Starting PostgreSQL@$PG_VERSION..."
    brew services restart postgresql@$PG_VERSION
    sleep 3

    # Wait for PostgreSQL to be ready
    print_info "Waiting for PostgreSQL to accept connections..."
    for i in {1..10}; do
        if psql -U $(whoami) -d postgres -c "SELECT 1" &>/dev/null; then
            print_success "PostgreSQL is ready"
            return
        fi
        echo -n "."
        sleep 1
    done

    print_warning "PostgreSQL may not be fully ready yet, but continuing..."
}

# Verify SSL configuration
verify_ssl() {
    print_header "Verifying SSL Configuration"

    # Check if SSL is enabled
    SSL_STATUS=$(psql -U $(whoami) -d postgres -t -A -c "SHOW ssl;" 2>/dev/null || echo "error")

    if [ "$SSL_STATUS" = "on" ]; then
        print_success "SSL is ENABLED"
    elif [ "$SSL_STATUS" = "off" ]; then
        print_error "SSL is still DISABLED"
        print_warning "Please check the PostgreSQL logs for errors:"
        print_info "  tail -50 /opt/homebrew/var/log/postgresql@$PG_VERSION.log"
        return 1
    else
        print_error "Could not verify SSL status (connection error)"
        print_info "Make sure PostgreSQL is running and accessible"
        return 1
    fi

    # Display SSL configuration
    echo ""
    print_info "SSL Configuration:"
    psql -U $(whoami) -d postgres -c "
        SELECT
            name,
            setting
        FROM pg_settings
        WHERE name LIKE 'ssl%'
        ORDER BY name;
    " 2>/dev/null || true
}

# Update Node.js service connection strings
update_connection_strings() {
    print_header "Updating Service Connection Strings"

    print_info "To use SSL in your Node.js services, update the connection string:"
    echo ""
    echo -e "${YELLOW}const dbPool = new Pool({${NC}"
    echo -e "${YELLOW}  host: 'localhost',${NC}"
    echo -e "${YELLOW}  port: 5432,${NC}"
    echo -e "${YELLOW}  user: 'admin',${NC}"
    echo -e "${YELLOW}  password: 'admin123',${NC}"
    echo -e "${YELLOW}  database: 'Admisión_MTN_DB',${NC}"
    echo -e "${GREEN}  ssl: {${NC}"
    echo -e "${GREEN}    rejectUnauthorized: false  // Development only${NC}"
    echo -e "${GREEN}  },${NC}"
    echo -e "${YELLOW}  max: 20,${NC}"
    echo -e "${YELLOW}  idleTimeoutMillis: 30000,${NC}"
    echo -e "${YELLOW}  connectionTimeoutMillis: 2000${NC}"
    echo -e "${YELLOW}});${NC}"
    echo ""

    print_warning "Note: Set rejectUnauthorized: true in production with proper certificates"
}

# Generate SSL configuration guide
generate_documentation() {
    print_header "Generating Documentation"

    DOC_FILE="/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend/POSTGRESQL_SSL_GUIDE.md"

    cat > "$DOC_FILE" << 'EOF'
# PostgreSQL SSL/TLS Configuration Guide
## Sistema de Admisión MTN

### Overview
This guide documents the SSL/TLS configuration for PostgreSQL in the Sistema de Admisión MTN.

### SSL Status
✅ SSL/TLS is ENABLED

### Configuration Details

**PostgreSQL Version:** 15
**Data Directory:** `/opt/homebrew/var/postgresql@15`
**Certificates Location:** `/opt/homebrew/var/postgresql@15/`

**SSL Settings:**
- `ssl = on`
- `ssl_cert_file = 'server.crt'`
- `ssl_key_file = 'server.key'`
- `ssl_ciphers = 'HIGH:MEDIUM:+3DES:!aNULL'`
- `ssl_prefer_server_ciphers = on`
- `ssl_min_protocol_version = 'TLSv1.2'`

### Certificates

**Type:** Self-signed (Development)
**Validity:** 365 days
**Common Name (CN):** localhost

For production, replace with certificates from a trusted Certificate Authority (CA).

### Node.js Connection Configuration

#### Development (Self-signed certificates)
```javascript
const { Pool } = require('pg');

const dbPool = new Pool({
  host: 'localhost',
  port: 5432,
  user: 'admin',
  password: 'admin123',
  database: 'Admisión_MTN_DB',
  ssl: {
    rejectUnauthorized: false  // Accept self-signed certificates
  },
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
});
```

#### Production (CA-signed certificates)
```javascript
const { Pool } = require('pg');
const fs = require('fs');

const dbPool = new Pool({
  host: process.env.DB_HOST || 'production-db.example.com',
  port: process.env.DB_PORT || 5432,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: {
    rejectUnauthorized: true,  // Verify certificates
    ca: fs.readFileSync('/path/to/ca-certificate.crt').toString(),
    key: fs.readFileSync('/path/to/client-key.key').toString(),
    cert: fs.readFileSync('/path/to/client-cert.crt').toString()
  },
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
});
```

### Verification Commands

Check SSL status:
```bash
psql -U admin -d "Admisión_MTN_DB" -c "SHOW ssl;"
```

View SSL configuration:
```bash
psql -U admin -d "Admisión_MTN_DB" -c "SELECT name, setting FROM pg_settings WHERE name LIKE 'ssl%' ORDER BY name;"
```

Test SSL connection:
```bash
psql "postgresql://admin:admin123@localhost:5432/Admisión_MTN_DB?sslmode=require"
```

### Troubleshooting

#### SSL not enabled after restart
1. Check PostgreSQL logs:
   ```bash
   tail -50 /opt/homebrew/var/log/postgresql@15.log
   ```

2. Verify certificate permissions:
   ```bash
   ls -la /opt/homebrew/var/postgresql@15/server.*
   ```
   - `server.key` should be `-rw-------` (600)
   - `server.crt` should be `-rw-r--r--` (644)

3. Restart PostgreSQL:
   ```bash
   brew services restart postgresql@15
   ```

#### Connection refused errors
- Ensure PostgreSQL is running: `brew services list`
- Check port availability: `lsof -i :5432`
- Verify pg_hba.conf allows SSL connections

#### Certificate errors in production
- Ensure certificates are from a trusted CA
- Verify certificate chain is complete
- Check certificate expiration date
- Validate certificate matches hostname

### Security Best Practices

1. **Development:**
   - Self-signed certificates are acceptable
   - Use `sslmode=require` minimum
   - Keep `rejectUnauthorized: false` only in dev

2. **Production:**
   - Use CA-signed certificates
   - Set `sslmode=verify-full`
   - Enable `rejectUnauthorized: true`
   - Implement certificate rotation
   - Use environment variables for secrets
   - Enable client certificate authentication
   - Monitor certificate expiration

3. **Certificate Management:**
   - Rotate certificates before expiration
   - Store private keys securely
   - Use strong cipher suites
   - Disable SSLv3 and TLS 1.0/1.1
   - Enable TLS 1.2 or higher only

### Railway Production Deployment

For Railway deployment, PostgreSQL SSL is automatically configured:

```javascript
// Railway automatically provides DATABASE_URL with SSL
const dbPool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false  // Railway uses self-signed certs
  },
  max: 20
});
```

Railway environment variables:
- `DATABASE_URL` - Full connection string with SSL
- `DB_HOST` - Database hostname
- `DB_PORT` - Database port (default: 5432)
- `DB_USER` - Database user
- `DB_PASSWORD` - Database password
- `DB_NAME` - Database name

### Maintenance

#### Certificate Renewal
Certificates expire after 365 days. To renew:

```bash
cd /opt/homebrew/var/postgresql@15/
openssl req -new -x509 -days 365 -nodes -text \
    -out server.crt \
    -keyout server.key \
    -subj "/CN=localhost"
chmod 600 server.key
brew services restart postgresql@15
```

#### Backup Certificates
```bash
cp /opt/homebrew/var/postgresql@15/server.crt ~/backups/
cp /opt/homebrew/var/postgresql@15/server.key ~/backups/
```

### References

- [PostgreSQL SSL Documentation](https://www.postgresql.org/docs/current/ssl-tcp.html)
- [Node.js pg SSL](https://node-postgres.com/features/ssl)
- [OpenSSL Certificate Commands](https://www.openssl.org/docs/man1.1.1/man1/openssl-req.html)

### Support

For issues or questions:
- Check logs: `/opt/homebrew/var/log/postgresql@15.log`
- Run verification script: `./enable-postgresql-ssl.sh`
- Review CLAUDE.md for architecture details

---

**Last Updated:** $(date +"%Y-%m-%d %H:%M:%S")
**Generated by:** enable-postgresql-ssl.sh
EOF

    print_success "Documentation generated: $DOC_FILE"
}

# Main execution
main() {
    print_header "PostgreSQL SSL Configuration Script"
    print_info "Sistema de Admisión MTN"
    echo ""

    detect_postgres
    generate_certificates
    configure_postgresql
    restart_postgresql

    if verify_ssl; then
        update_connection_strings
        generate_documentation

        print_header "✅ SSL Configuration Complete!"
        print_success "PostgreSQL is now configured with SSL/TLS"
        print_info "Documentation: POSTGRESQL_SSL_GUIDE.md"
        echo ""
        print_warning "Next steps:"
        print_info "1. Update your service connection strings to use SSL"
        print_info "2. Restart your Node.js services"
        print_info "3. Verify connections with: psql 'postgresql://admin:admin123@localhost:5432/Admisión_MTN_DB?sslmode=require'"
        echo ""
    else
        print_error "SSL configuration failed"
        print_info "Check the logs and retry: tail -50 /opt/homebrew/var/log/postgresql@$PG_VERSION.log"
        exit 1
    fi
}

# Run main function
main
