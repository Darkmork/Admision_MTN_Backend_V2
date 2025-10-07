#!/bin/bash

# Railway Database Setup Script
# This script helps you set up PostgreSQL on Railway

set -e

echo "🚂 Railway Database Setup"
echo "========================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo -e "${RED}❌ Railway CLI is not installed${NC}"
    echo ""
    echo "Install it with:"
    echo "  npm install -g @railway/cli"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ Railway CLI is installed${NC}"
echo ""

# Check if logged in
echo "Checking Railway login status..."
if ! railway whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  Not logged in to Railway${NC}"
    echo ""
    echo "Logging in to Railway..."
    railway login
    echo ""
fi

echo -e "${GREEN}✅ Logged in to Railway${NC}"
echo ""

# Link to project
echo "Linking to Railway project..."
echo ""
echo -e "${YELLOW}Please select: mtn-admission-backend${NC}"
echo ""
railway link

echo ""
echo -e "${GREEN}✅ Project linked${NC}"
echo ""

# Menu
echo "What would you like to do?"
echo ""
echo "1) Setup PostgreSQL (create tables and admin user)"
echo "2) View PostgreSQL connection info"
echo "3) Connect to PostgreSQL shell (psql)"
echo "4) Check if tables exist"
echo "5) Reset database (DROP all tables and recreate)"
echo "6) Exit"
echo ""
read -p "Enter your choice (1-6): " choice

case $choice in
    1)
        echo ""
        echo "📊 Setting up PostgreSQL..."
        echo ""
        echo "Please select the Postgres service when prompted:"
        railway service
        echo ""
        echo "Executing SQL setup script..."
        railway run psql < railway-db-setup.sql
        echo ""
        echo -e "${GREEN}✅ Database setup complete!${NC}"
        echo ""
        echo "Admin user created:"
        echo "  Email: jorge.gangale@mtn.cl"
        echo "  Password: admin123"
        ;;
    2)
        echo ""
        echo "📋 PostgreSQL Connection Info:"
        echo ""
        railway variables | grep PG
        ;;
    3)
        echo ""
        echo "🔌 Connecting to PostgreSQL..."
        echo ""
        echo "Please select the Postgres service when prompted:"
        railway service
        echo ""
        railway run psql
        ;;
    4)
        echo ""
        echo "🔍 Checking tables..."
        echo ""
        echo "Please select the Postgres service when prompted:"
        railway service
        echo ""
        railway run psql -c "\dt"
        ;;
    5)
        echo ""
        echo -e "${RED}⚠️  WARNING: This will DELETE all data!${NC}"
        echo ""
        read -p "Are you sure? Type 'yes' to confirm: " confirm
        if [ "$confirm" = "yes" ]; then
            echo ""
            echo "🗑️  Dropping all tables..."
            echo ""
            echo "Please select the Postgres service when prompted:"
            railway service
            echo ""
            railway run psql -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
            echo ""
            echo "📊 Recreating tables..."
            railway run psql < railway-db-setup.sql
            echo ""
            echo -e "${GREEN}✅ Database reset complete!${NC}"
        else
            echo ""
            echo "❌ Cancelled"
        fi
        ;;
    6)
        echo ""
        echo "👋 Goodbye!"
        exit 0
        ;;
    *)
        echo ""
        echo -e "${RED}❌ Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}✅ Done!${NC}"
echo ""
echo "Next steps:"
echo "1. Verify the backend is running: https://admisionmtnbackendv2-production.up.railway.app/health"
echo "2. Test login endpoint"
echo "3. Access frontend: https://admision-mtn-frontend.vercel.app"
echo ""
