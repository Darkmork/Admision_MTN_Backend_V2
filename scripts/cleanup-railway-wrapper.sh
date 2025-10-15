#!/bin/bash

# Railway Database Cleanup Wrapper Script
#
# This script provides instructions for cleaning the Railway production database.
#
# USAGE:
#   1. Get DATABASE_URL from Railway dashboard:
#      - Go to https://railway.app/project/Admision_MTN_Backend
#      - Go to Variables tab
#      - Copy DATABASE_URL value
#
#   2. Run the cleanup script:
#      export DATABASE_URL="postgresql://postgres:xxxxx@postgres.railway.internal:5432/railway"
#      node scripts/cleanup-railway-db.js --dry-run     # Test first
#      node scripts/cleanup-railway-db.js --execute     # Execute cleanup

echo "========================================="
echo "🗑️  RAILWAY DATABASE CLEANUP"
echo "========================================="
echo ""
echo "This script will clean the Railway production database,"
echo "removing all data EXCEPT the admin user (jorge.gangale@mtn.cl)."
echo ""
echo "⚠️  WARNING: This is DESTRUCTIVE and IRREVERSIBLE."
echo ""
echo "STEPS TO RUN:"
echo ""
echo "1. Get DATABASE_URL from Railway dashboard:"
echo "   - Open: https://railway.app/"
echo "   - Select project: Admision_MTN_Backend"
echo "   - Go to PostgreSQL service"
echo "   - Go to 'Connect' tab"
echo "   - Copy 'Postgres Connection URL'"
echo ""
echo "2. Set the environment variable:"
echo "   export DATABASE_URL=\"<paste_connection_url_here>\""
echo ""
echo "3. Test with dry-run (recommended):"
echo "   node scripts/cleanup-railway-db.js --dry-run"
echo ""
echo "4. Execute cleanup (if dry-run looks good):"
echo "   node scripts/cleanup-railway-db.js --execute"
echo ""
echo "========================================="
echo ""

# Check if DATABASE_URL is already set
if [ -z "$DATABASE_URL" ]; then
  echo "❌ DATABASE_URL not set"
  echo ""
  echo "Please set DATABASE_URL first:"
  echo "  export DATABASE_URL=\"postgresql://...\""
  echo ""
  exit 1
fi

echo "✅ DATABASE_URL is set"
echo ""
echo "Connection string: ${DATABASE_URL:0:50}... (truncated for security)"
echo ""

# Check command line argument
if [ $# -eq 0 ]; then
  echo "❌ No mode specified"
  echo ""
  echo "Usage:"
  echo "  $0 --dry-run     # Test mode (no changes)"
  echo "  $0 --execute     # Execute cleanup (DESTRUCTIVE)"
  echo ""
  exit 1
fi

MODE=$1

if [ "$MODE" != "--dry-run" ] && [ "$MODE" != "--execute" ]; then
  echo "❌ Invalid mode: $MODE"
  echo ""
  echo "Valid modes:"
  echo "  --dry-run     # Test mode (no changes)"
  echo "  --execute     # Execute cleanup (DESTRUCTIVE)"
  echo ""
  exit 1
fi

# Confirmation for execute mode
if [ "$MODE" = "--execute" ]; then
  echo "⚠️  ⚠️  ⚠️  WARNING ⚠️  ⚠️  ⚠️"
  echo ""
  echo "You are about to DELETE ALL DATA from Railway production database!"
  echo "Only the admin user (jorge.gangale@mtn.cl) will be preserved."
  echo ""
  echo "This action is IRREVERSIBLE."
  echo ""
  read -p "Type 'DELETE EVERYTHING' to confirm: " confirmation

  if [ "$confirmation" != "DELETE EVERYTHING" ]; then
    echo ""
    echo "❌ Cleanup cancelled (confirmation did not match)"
    exit 1
  fi

  echo ""
  echo "✅ Confirmation received. Proceeding with cleanup..."
  echo ""
fi

# Run the Node.js cleanup script
node scripts/cleanup-railway-db.js $MODE
