#!/bin/bash

# Railway Support Ticket Submission Script
# Date: 2025-10-14
# Project: admisionmtnbackendv2-production

echo "==================================="
echo "Railway Support Ticket Submission"
echo "==================================="
echo ""

# Check if railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI not found. Installing..."
    npm install -g @railway/cli
    if [ $? -ne 0 ]; then
        echo "❌ Failed to install Railway CLI"
        echo "Please install manually: npm install -g @railway/cli"
        exit 1
    fi
fi

echo "✅ Railway CLI found"
echo ""

# Check authentication
echo "Checking Railway authentication..."
railway whoami &> /dev/null
if [ $? -ne 0 ]; then
    echo "❌ Not authenticated with Railway"
    echo "Please login first: railway login"
    exit 1
fi

echo "✅ Authenticated with Railway"
echo ""

# Display ticket summary
echo "==================================="
echo "Ticket Summary"
echo "==================================="
echo "Project: admisionmtnbackendv2-production"
echo "Severity: CRITICAL"
echo "Issue: POST requests with JSON payload timing out"
echo "Impact: 100% production system unusable"
echo ""

# Ask for confirmation
read -p "Submit this ticket to Railway Support? (y/n): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Ticket submission cancelled."
    exit 0
fi

echo ""
echo "==================================="
echo "Submitting Ticket"
echo "==================================="
echo ""

# Create a temporary file with the ticket content
TICKET_FILE="/tmp/railway-support-ticket-$(date +%s).txt"
cat > "$TICKET_FILE" << 'EOF'
Subject: CRITICAL - POST requests with JSON payload timing out (0% success rate)

All POST requests with Content-Type: application/json and a request body are timing out after 10-30 seconds with 0 bytes received. GET requests work perfectly.

IMPACT:
- Users cannot log in (POST /api/auth/login)
- Users cannot submit applications (POST /api/applications)
- Production system is 100% unusable for write operations
- Success rate: 0% for all POST requests with JSON

PATTERN IDENTIFIED:
✅ GET /health → 200 OK in 0.640s (WORKS)
✅ GET /api/auth/public-key → 200 OK in 0.623s (WORKS)
✅ POST /api/security/csrf-token (no body) → 404 in 0.598s (WORKS)
❌ POST /api/auth/login (with JSON) → TIMEOUT 30s, 0 bytes (FAILS)
❌ POST /api/applications/search (with JSON) → TIMEOUT 10s, 0 bytes (FAILS)

EVIDENCE:
- 0 bytes received = Response never arrives
- No backend logs = Requests never reach service
- Works locally in 77ms = Code is correct
- Railway blocking at infrastructure layer (WAF/Firewall/Proxy)

REPRODUCTION:
curl -X POST "https://admisionmtnbackendv2-production.up.railway.app/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}' \
  --max-time 30

Result: Timeout after 30s with 0 bytes received

QUESTIONS:
1. Are POST requests with JSON bodies being blocked by WAF/firewall?
2. Can you provide Railway-side logs showing what happens to these requests?
3. Is there rate limiting causing this?
4. Is this a known proxy layer issue?
5. Can you whitelist our project if this is a security rule?

URGENCY: CRITICAL - Production completely down

Full documentation: See RAILWAY_SUPPORT_TICKET.md in project

Contact: jorge.gangale@mtn.cl
Project: admisionmtnbackendv2-production
EOF

echo "📄 Ticket content prepared at: $TICKET_FILE"
echo ""

# Try to open Railway support page
echo "Opening Railway support page..."
echo "URL: https://railway.app/help"
echo ""

# Open browser (works on macOS, Linux, Windows)
if [[ "$OSTYPE" == "darwin"* ]]; then
    open "https://railway.app/help"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    xdg-open "https://railway.app/help"
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    start "https://railway.app/help"
fi

echo "==================================="
echo "Next Steps"
echo "==================================="
echo ""
echo "1. A browser window should open with Railway support page"
echo "2. Click on 'Submit a request' or 'Contact Support'"
echo "3. Copy the ticket content from: $TICKET_FILE"
echo "4. Paste it into the support form"
echo "5. Mark severity as: CRITICAL"
echo "6. Submit the ticket"
echo ""
echo "Ticket content can also be found in: RAILWAY_SUPPORT_TICKET.md"
echo ""
echo "==================================="
echo "Ticket Ready for Submission"
echo "==================================="
echo ""

# Also try Railway logs command
echo "Fetching Railway logs (if available)..."
railway logs --json > /tmp/railway-logs-$(date +%s).json 2>&1 || echo "⚠️  Could not fetch logs (may need project linking)"

echo ""
echo "✅ All preparation complete!"
echo "Please submit the ticket through the Railway support portal."
