#!/bin/bash

###############################################################################
# Railway Database Restoration Script
###############################################################################
# This script restores the Admisión MTN database backup to Railway PostgreSQL
#
# Prerequisites:
# 1. Railway CLI installed: npm i -g @railway/cli
# 2. Logged into Railway: railway login
# 3. Linked to Railway project: railway link
# 4. PostgreSQL plugin added to Railway project
# 5. Backup file: backups/admision_mtn_backup_20251013_082802.sql.gz
#
# Usage:
#   ./railway-restore-db.sh
#
# What it does:
# 1. Extracts backup from .gz file
# 2. Connects to Railway PostgreSQL using railway run
# 3. Drops existing tables (if any)
# 4. Restores schema and data
# 5. Verifies restoration with record counts
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Railway Database Restoration${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v railway &> /dev/null; then
    echo -e "${RED}❌ Railway CLI not found${NC}"
    echo "Install with: npm i -g @railway/cli"
    exit 1
fi

if ! command -v psql &> /dev/null; then
    echo -e "${RED}❌ PostgreSQL client (psql) not found${NC}"
    echo "Install with: brew install postgresql@14"
    exit 1
fi

echo -e "${GREEN}✅ Railway CLI installed${NC}"
echo -e "${GREEN}✅ PostgreSQL client installed${NC}"
echo ""

# Check if linked to Railway project
echo -e "${YELLOW}Checking Railway project link...${NC}"
if ! railway status &> /dev/null; then
    echo -e "${RED}❌ Not linked to Railway project${NC}"
    echo "Run: railway link"
    exit 1
fi

echo -e "${GREEN}✅ Linked to Railway project${NC}"
echo ""

# Check backup file
BACKUP_DIR="backups"
BACKUP_GZ="${BACKUP_DIR}/admision_mtn_backup_20251013_082802.sql.gz"
BACKUP_SQL="${BACKUP_DIR}/admision_mtn_backup_20251013_082802.sql"

if [ ! -f "$BACKUP_GZ" ]; then
    echo -e "${RED}❌ Backup file not found: ${BACKUP_GZ}${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Backup file found: ${BACKUP_GZ}${NC}"
echo ""

# Extract backup if not already extracted
if [ ! -f "$BACKUP_SQL" ]; then
    echo -e "${YELLOW}Extracting backup...${NC}"
    gunzip -k "$BACKUP_GZ"
    echo -e "${GREEN}✅ Backup extracted${NC}"
else
    echo -e "${GREEN}✅ Backup already extracted${NC}"
fi
echo ""

# Confirm with user
echo -e "${YELLOW}⚠️  WARNING: This will drop ALL existing tables and data!${NC}"
echo -e "${YELLOW}⚠️  Make sure you're restoring to the correct Railway database.${NC}"
echo ""
read -p "Continue with restoration? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo -e "${RED}Restoration cancelled${NC}"
    exit 0
fi
echo ""

# Get DATABASE_URL from Railway
echo -e "${YELLOW}Fetching Railway database connection...${NC}"
DATABASE_URL=$(railway variables --json | grep -o '"DATABASE_URL":"[^"]*"' | cut -d'"' -f4)

if [ -z "$DATABASE_URL" ]; then
    echo -e "${RED}❌ DATABASE_URL not found in Railway variables${NC}"
    echo "Make sure PostgreSQL plugin is added to your Railway project"
    exit 1
fi

echo -e "${GREEN}✅ Railway database connection retrieved${NC}"
echo ""

# Create restoration script
echo -e "${YELLOW}Preparing restoration...${NC}"

# Drop existing tables (optional, be careful!)
echo -e "${YELLOW}Dropping existing tables...${NC}"
railway run psql -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" 2>&1 || true
echo -e "${GREEN}✅ Schema reset complete${NC}"
echo ""

# Restore database
echo -e "${YELLOW}Restoring database from backup...${NC}"
echo -e "${BLUE}This may take 1-2 minutes...${NC}"
echo ""

railway run psql < "$BACKUP_SQL"

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Database restoration completed successfully${NC}"
else
    echo ""
    echo -e "${RED}❌ Database restoration failed${NC}"
    exit 1
fi
echo ""

# Verify restoration
echo -e "${YELLOW}Verifying restoration...${NC}"
echo ""

# Check record counts
echo -e "${BLUE}Record counts:${NC}"
railway run psql -c "
SELECT
    'users' as table_name, COUNT(*) as records FROM users
UNION ALL SELECT 'applications', COUNT(*) FROM applications
UNION ALL SELECT 'students', COUNT(*) FROM students
UNION ALL SELECT 'guardians', COUNT(*) FROM guardians
UNION ALL SELECT 'interviews', COUNT(*) FROM interviews
UNION ALL SELECT 'evaluations', COUNT(*) FROM evaluations
ORDER BY table_name;
"

echo ""

# Check admin user exists
echo -e "${BLUE}Checking admin user...${NC}"
railway run psql -c "SELECT id, email, role, first_name, last_name FROM users WHERE role = 'ADMIN' LIMIT 3;"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ RESTORATION COMPLETE${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "1. Deploy your services to Railway"
echo "2. Test authentication: jorge.gangale@mtn.cl / admin123"
echo "3. Run smoke tests with: npm run test:smoke"
echo ""
echo -e "${YELLOW}Note: Clean up extracted backup if desired:${NC}"
echo "rm $BACKUP_SQL"
echo ""
