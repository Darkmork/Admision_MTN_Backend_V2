# Location Fields Testing Guide
**Quick Reference for Manual Testing**

## Test Scenarios

### Scenario 1: Chilean Student (Region + Comuna Required)
**Expected Behavior:** Form should require region and comuna

**Steps:**
1. Navigate to Application Form: `http://localhost:5173/application`
2. Fill in student basic information
3. In "Ubicación Geográfica" section:
   - País: Select "Chile" (default)
   - Región: Select "Región Metropolitana de Santiago"
   - Comuna: Enter "Providencia"
4. Attempt to proceed to next step
5. **Expected:** Form validates successfully and proceeds

**Test Validation Errors:**
- Leave Región empty → Should see "Región" in missing fields
- Leave Comuna empty → Should see "Comuna" in missing fields

---

### Scenario 2: International Student (Region + Comuna Optional)
**Expected Behavior:** Form should NOT require region and comuna

**Steps:**
1. Navigate to Application Form
2. Fill in student basic information
3. In "Ubicación Geográfica" section:
   - País: Select "Argentina"
   - **Notice:** Region and Comuna fields disappear
   - **See:** Blue info box: "Para estudiantes residentes fuera de Chile..."
4. Attempt to proceed to next step
5. **Expected:** Form validates successfully WITHOUT region/comuna

---

### Scenario 3: Country Switch (Auto-Clear Behavior)
**Expected Behavior:** Region and comuna should clear when switching from Chile

**Steps:**
1. Select País: "Chile"
2. Select Región: "Región Metropolitana de Santiago"
3. Enter Comuna: "Providencia"
4. **Change País to "Peru"**
5. **Notice:** Region and Comuna fields disappear
6. **Change back to "Chile"**
7. **Expected:** Region and Comuna are now empty (cleared)

---

### Scenario 4: Admission Preference Radio Buttons
**Expected Behavior:** Radio buttons should work intuitively

**Steps:**
1. Scroll to "Tipo de Relación Familiar" section
2. **See:** Three radio button options with descriptions
3. **Default:** "Ninguna" should be selected
4. Click "Hijo de Funcionario"
5. **See:** Green info box appears with documentation requirements
6. Click "Hijo de Ex-Alumno"
7. **See:** Blue info box appears with documentation requirements
8. Click "Ninguna" again
9. **See:** Info boxes disappear

---

## Database Verification

### Verify Schema
```sql
-- Check columns exist
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'students' 
  AND column_name IN ('pais', 'region', 'comuna');
```

**Expected Result:**
```
 column_name |     data_type     | is_nullable | column_default
-------------+-------------------+-------------+-------------------
 pais        | character varying | NO          | 'Chile'
 region      | character varying | YES         | 
 comuna      | character varying | YES         | 
```

### Verify Data Persistence
```sql
-- After submitting a test application
SELECT 
  id, 
  first_name, 
  paternal_last_name,
  pais, 
  region, 
  comuna, 
  admission_preference
FROM students 
ORDER BY id DESC 
LIMIT 5;
```

**Expected Result:** New records should have pais, region, comuna populated

---

## API Testing (Manual)

### Test POST Endpoint with Chilean Data
```bash
# Note: Requires CSRF token and auth token
# Use browser DevTools Network tab to see actual payload sent
```

**Expected Request Payload:**
```json
{
  "firstName": "JUAN",
  "paternalLastName": "PEREZ",
  "rut": "12345678-9",
  "pais": "Chile",
  "region": "Región Metropolitana de Santiago",
  "comuna": "Providencia",
  "admissionPreference": "NINGUNA"
}
```

**Expected Response (201 Created):**
```json
{
  "success": true,
  "message": "Postulación creada exitosamente",
  "student": {
    "pais": "Chile",
    "region": "Región Metropolitana de Santiago",
    "comuna": "Providencia",
    "admissionPreference": "NINGUNA"
  }
}
```

### Test Validation Error (Missing Region for Chile)
**Expected Request:**
```json
{
  "pais": "Chile",
  "region": "",
  "comuna": "Providencia"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Region y comuna son obligatorios para Chile",
  "errorCode": "APP_003"
}
```

---

## Browser DevTools Checklist

### Console Logs to Check
- ✅ No JavaScript errors
- ✅ Form validation logs
- ✅ API request/response logs

### Network Tab to Check
- ✅ POST /api/applications includes pais, region, comuna
- ✅ GET /api/applications/:id returns pais, region, comuna
- ✅ 201 Created response on successful submission
- ✅ 400 Bad Request on validation failure

### Elements Tab to Check
- ✅ Radio buttons render correctly
- ✅ Conditional fields show/hide properly
- ✅ Required fields have asterisk (*)
- ✅ Help text displays correctly

---

## Edge Cases to Test

### 1. Empty Country Selection
- **Action:** Clear país dropdown (if possible)
- **Expected:** Defaults to "Chile", shows region/comuna

### 2. Special Characters in Comuna
- **Action:** Enter "Ñuñoa" or "Maipú"
- **Expected:** Accepts special characters correctly

### 3. Long Region Names
- **Action:** Select "Región de Aysén del General Carlos Ibáñez del Campo"
- **Expected:** Displays fully without truncation

### 4. Form Refresh/Reload
- **Action:** Fill form, reload page
- **Expected:** Data clears (expected behavior for new applications)

---

## Known Limitations

1. **Comuna Field:** Currently a text input (not a dropdown)
   - **Reason:** Over 300 comunas in Chile
   - **Future:** Could implement region-specific dropdowns

2. **Country List:** Limited to 6 options
   - **Reason:** Most common for Chilean school
   - **Future:** Could expand based on needs

3. **Validation Language:** Spanish only
   - **Reason:** Target audience is Spanish-speaking
   - **Future:** Could add i18n support

---

## Troubleshooting

### Frontend Issues

**Problem:** Location fields not showing
- **Check:** Browser console for errors
- **Check:** Data state in React DevTools
- **Solution:** Clear browser cache, restart dev server

**Problem:** Validation not working
- **Check:** validation logic in ApplicationForm.tsx lines 618-624
- **Solution:** Verify `data.pais` value is exactly "Chile"

### Backend Issues

**Problem:** 400 validation error when it shouldn't fail
- **Check:** Request payload has correct field names
- **Check:** Backend logs for validation details
- **Solution:** Verify field names match exactly (pais, region, comuna)

**Problem:** Data not persisting
- **Check:** Database connection
- **Check:** SQL query in mock-application-service.js line 1998
- **Solution:** Restart backend service, check database permissions

---

## Success Indicators

✅ **Form UX:**
- Location fields display correctly
- Conditional visibility works smoothly
- Radio buttons are intuitive
- Validation messages are clear

✅ **Data Flow:**
- Frontend sends correct payload
- Backend validates appropriately
- Database stores data correctly
- GET endpoint returns complete data

✅ **User Experience:**
- No JavaScript errors
- Fast response times
- Clear feedback on errors
- Smooth form flow

---

## Quick Commands

```bash
# Check database schema
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "\d students"

# View recent applications with location data
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT id, first_name, paternal_last_name, pais, region, comuna FROM students ORDER BY id DESC LIMIT 5;"

# Check backend service status
lsof -ti:8083

# Restart backend service (if needed)
pkill -f "mock-application-service.js" && node mock-application-service.js &

# Check frontend dev server
lsof -ti:5173

# Restart frontend (if needed)
cd ../Admision_MTN_front && npm run dev
```

---

**Testing Date:** October 11, 2025
**Tester:** [Your Name]
**All Tests Passed:** ✅ / ❌
**Notes:** _______________
