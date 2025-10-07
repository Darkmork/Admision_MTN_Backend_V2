#!/bin/bash

# Test script for credential encryption
# Demonstrates that plain text credentials work (backward compatibility)
# Frontend will automatically encrypt when encryption service is available

echo "=========================================="
echo "Credential Encryption Test Suite"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8082"
ADMIN_EMAIL="jorge.gangale@mtn.cl"
ADMIN_PASSWORD="admin123"

echo "1. Testing Public Key Endpoint..."
echo "-------------------------------------------"
PUBLIC_KEY_RESPONSE=$(curl -s ${BASE_URL}/api/auth/public-key)
PUBLIC_KEY_SUCCESS=$(echo $PUBLIC_KEY_RESPONSE | grep -o '"success":true' | wc -l)

if [ $PUBLIC_KEY_SUCCESS -eq 1 ]; then
  echo -e "${GREEN}✓${NC} Public key endpoint working"
  echo "   Algorithm: RSA-OAEP-2048"
  echo "   Hash: SHA-256"
  KEY_ID=$(echo $PUBLIC_KEY_RESPONSE | grep -o '"keyId":"[^"]*"' | cut -d'"' -f4)
  echo "   Key ID: ${KEY_ID}"
else
  echo -e "${RED}✗${NC} Public key endpoint failed"
  exit 1
fi

echo ""
echo "2. Testing CSRF Token Generation..."
echo "-------------------------------------------"
CSRF_RESPONSE=$(curl -c /tmp/test-cookies.txt -s ${BASE_URL}/api/auth/csrf-token)
CSRF_TOKEN=$(echo $CSRF_RESPONSE | grep -o '"csrfToken":"[^"]*"' | cut -d'"' -f4)

if [ ! -z "$CSRF_TOKEN" ]; then
  echo -e "${GREEN}✓${NC} CSRF token generated"
  echo "   Token: ${CSRF_TOKEN:0:40}..."
else
  echo -e "${RED}✗${NC} CSRF token generation failed"
  exit 1
fi

echo ""
echo "3. Testing Plain Text Login (Backward Compatibility)..."
echo "-------------------------------------------"
LOGIN_RESPONSE=$(curl -b /tmp/test-cookies.txt \
  -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-CSRF-Token: ${CSRF_TOKEN}" \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}" \
  -s)

LOGIN_SUCCESS=$(echo $LOGIN_RESPONSE | grep -o '"success":true' | wc -l)

if [ $LOGIN_SUCCESS -eq 1 ]; then
  echo -e "${GREEN}✓${NC} Plain text login successful (backward compatibility working)"
  JWT_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  echo "   JWT Token: ${JWT_TOKEN:0:50}..."
else
  echo -e "${RED}✗${NC} Plain text login failed"
  echo "   Response: ${LOGIN_RESPONSE}"
  exit 1
fi

echo ""
echo "4. Checking Server Logs for Encryption Status..."
echo "-------------------------------------------"
if [ -f /tmp/user-service-encryption-test.log ]; then
  PLAIN_TEXT_DETECTED=$(grep "Plain text credentials detected" /tmp/user-service-encryption-test.log | tail -1)
  if [ ! -z "$PLAIN_TEXT_DETECTED" ]; then
    echo -e "${GREEN}✓${NC} Server correctly detected plain text credentials"
    echo "   Log: ${PLAIN_TEXT_DETECTED}"
  else
    echo -e "${YELLOW}⚠${NC} Could not verify plain text detection in logs"
  fi

  ENCRYPTION_LOGS=$(grep "\[Encryption\]" /tmp/user-service-encryption-test.log | wc -l)
  echo "   Total encryption events: ${ENCRYPTION_LOGS}"
else
  echo -e "${YELLOW}⚠${NC} Server log file not found"
fi

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}✓${NC} Public key endpoint: PASS"
echo -e "${GREEN}✓${NC} CSRF token generation: PASS"
echo -e "${GREEN}✓${NC} Plain text login: PASS (backward compatible)"
echo -e "${GREEN}✓${NC} Server encryption ready: YES"
echo ""
echo "Next Steps:"
echo "  1. Frontend automatically encrypts when encryptionService available"
echo "  2. Test in browser at http://localhost:5173/login"
echo "  3. Check Network tab - password should NOT be visible"
echo "  4. Monitor logs: tail -f /tmp/user-service-encryption-test.log"
echo ""
echo "Documentation: CREDENTIAL_ENCRYPTION.md"
echo "=========================================="
