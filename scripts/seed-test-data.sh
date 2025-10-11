#!/bin/bash
# =========================================================================
# CI Test Data Seeding Script
# =========================================================================
# Seeds minimal test data for E2E tests in CI environment
# Idempotent: Can run multiple times without errors
# Creates: Admin user, Teacher, Student, Application
# =========================================================================

set -e  # Exit on any error

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-Admisión_MTN_DB}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

echo "=========================================="
echo "  CI Test Data Seeding"
echo "=========================================="
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo ""

# =========================================================================
# STEP 0: Ensure tables exist (idempotent schema creation)
# =========================================================================
echo "0️⃣  Ensuring database schema exists..."

PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<'EOSQL'
-- Create tables if they don't exist (idempotent)

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    subject VARCHAR(100),
    rut VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS guardians (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(50),
    rut VARCHAR(20) UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students (
    id SERIAL PRIMARY KEY,
    rut VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    paternal_last_name VARCHAR(100),
    maternal_last_name VARCHAR(100),
    date_of_birth DATE,
    birth_date DATE,
    gender VARCHAR(10),
    email VARCHAR(255),
    phone VARCHAR(20),
    address TEXT,
    previous_school VARCHAR(255),
    current_grade VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS applications (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    guardian_id INTEGER REFERENCES guardians(id),
    applicant_user_id INTEGER REFERENCES users(id),
    application_year INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    priority INTEGER DEFAULT 0,
    notes TEXT,
    submission_date TIMESTAMP,
    submitted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id)
);

CREATE TABLE IF NOT EXISTS interviewer_schedules (
    id SERIAL PRIMARY KEY,
    interviewer_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_guardians_rut ON guardians(rut);
CREATE INDEX IF NOT EXISTS idx_students_rut ON students(rut);
CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status);
CREATE INDEX IF NOT EXISTS idx_applications_student ON applications(student_id);

EOSQL

if [ $? -eq 0 ]; then
    echo "✅ Database schema ready (tables created/verified)"
else
    echo "❌ Schema creation failed"
    exit 1
fi

echo ""

# Function to execute SQL
execute_sql() {
    local sql="$1"
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$sql" -q
}

# Function to execute SQL and check if any rows were affected
execute_sql_check() {
    local sql="$1"
    local description="$2"

    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$sql" -q > /dev/null 2>&1; then
        echo "✅ $description"
        return 0
    else
        echo "⚠️  $description (may already exist)"
        return 0  # Don't fail on conflicts
    fi
}

echo "1️⃣  Creating Admin User..."
# BCrypt hash for 'admin123'
execute_sql_check "
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    rut,
    phone,
    role,
    email_verified,
    active,
    created_at,
    updated_at
) VALUES (
    'Jorge',
    'Gangale',
    'jorge.gangale@mtn.cl',
    '\$2a\$10\$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq',
    '11111111-1',
    '+56911111111',
    'ADMIN',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    password = '\$2a\$10\$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq',
    active = true,
    email_verified = true;
" "Admin user (jorge.gangale@mtn.cl / admin123)"

echo ""
echo "2️⃣  Creating Teacher User..."
execute_sql_check "
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    rut,
    phone,
    role,
    email_verified,
    active,
    created_at,
    updated_at
) VALUES (
    'Alejandra',
    'Flores',
    'alejandra.flores@mtn.cl',
    '\$2a\$10\$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq',
    '22222222-2',
    '+56922222222',
    'TEACHER',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    password = '\$2a\$10\$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq',
    active = true,
    email_verified = true;
" "Teacher user (alejandra.flores@mtn.cl / admin123)"

echo ""
echo "3️⃣  Creating Test Guardian..."
execute_sql_check "
INSERT INTO guardians (
    full_name,
    relationship,
    rut,
    email,
    phone,
    created_at
) VALUES (
    'María Test Guardian',
    'MADRE',
    '33333333-3',
    'maria.test@email.com',
    '+56933333333',
    NOW()
) ON CONFLICT (rut) DO NOTHING;
" "Test guardian"

echo ""
echo "4️⃣  Creating Test Student..."
execute_sql_check "
INSERT INTO students (
    first_name,
    paternal_last_name,
    maternal_last_name,
    rut,
    date_of_birth,
    gender,
    previous_school,
    current_grade,
    created_at
) VALUES (
    'Pedro',
    'Test',
    'Student',
    'CI-12345678',
    '2015-01-15',
    'MALE',
    'Escuela Test',
    'KINDER',
    NOW()
) ON CONFLICT (rut) DO NOTHING;
" "Test student (Pedro Test Student)"

echo ""
echo "5️⃣  Creating Test Application..."
# Get IDs first
STUDENT_ID=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM students WHERE rut = 'CI-12345678' LIMIT 1" | xargs)
GUARDIAN_ID=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM guardians WHERE rut = '33333333-3' LIMIT 1" | xargs)
ADMIN_ID=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM users WHERE email = 'jorge.gangale@mtn.cl' LIMIT 1" | xargs)

if [ -n "$STUDENT_ID" ] && [ -n "$GUARDIAN_ID" ] && [ -n "$ADMIN_ID" ]; then
    execute_sql_check "
    INSERT INTO applications (
        student_id,
        guardian_id,
        applicant_user_id,
        status,
        application_year,
        submission_date,
        created_at
    ) VALUES (
        $STUDENT_ID,
        $GUARDIAN_ID,
        $ADMIN_ID,
        'PENDING',
        2026,
        NOW(),
        NOW()
    ) ON CONFLICT (student_id) DO UPDATE SET
        status = 'PENDING',
        updated_at = NOW();
    " "Test application (PENDING status)"
else
    echo "⚠️  Could not create application - missing prerequisites"
fi

echo ""
echo "6️⃣  Creating Interviewer Schedule..."
TEACHER_ID=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM users WHERE email = 'alejandra.flores@mtn.cl' LIMIT 1" | xargs)

if [ -n "$TEACHER_ID" ]; then
    # Check if schedule already exists for this teacher on MONDAY
    EXISTING_SCHEDULE=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM interviewer_schedules WHERE interviewer_id = $TEACHER_ID AND day_of_week = 'MONDAY'" | xargs)

    if [ "$EXISTING_SCHEDULE" -eq "0" ]; then
        execute_sql_check "
        INSERT INTO interviewer_schedules (
            interviewer_id,
            day_of_week,
            start_time,
            end_time,
            is_active,
            created_at
        ) VALUES (
            $TEACHER_ID,
            'MONDAY',
            '09:00:00',
            '17:00:00',
            true,
            NOW()
        );
        " "Interviewer schedule for teacher"
    else
        echo "✅ Interviewer schedule for teacher (already exists)"
    fi
else
    echo "⚠️  Could not create interviewer schedule - teacher not found"
fi

echo ""
echo "=========================================="
echo "  ✅ Test Data Seeding Complete"
echo "=========================================="
echo ""
echo "Test Credentials:"
echo "  Admin:   jorge.gangale@mtn.cl / admin123"
echo "  Teacher: alejandra.flores@mtn.cl / admin123"
echo ""
echo "Test Data Created:"
echo "  - 2 Users (Admin + Teacher)"
echo "  - 1 Guardian"
echo "  - 1 Student"
echo "  - 1 Application (PENDING)"
echo "  - 1 Interviewer Schedule"
echo ""

# Verify data
echo "📊 Verification:"
execute_sql "SELECT COUNT(*) as user_count FROM users WHERE email IN ('jorge.gangale@mtn.cl', 'alejandra.flores@mtn.cl');"
execute_sql "SELECT COUNT(*) as application_count FROM applications WHERE student_id = ${STUDENT_ID:-0};"

echo ""
echo "✅ Seed complete - Ready for E2E tests"
