#!/bin/bash
# Quick script to check if users exist in Railway database

echo "Checking users in Railway database..."
echo ""

read -s -p "Paste DATABASE_URL: " DATABASE_URL
echo ""
echo ""

psql "$DATABASE_URL" -c "SELECT id, email, role, first_name, last_name FROM users WHERE role = 'ADMIN' LIMIT 5;"
