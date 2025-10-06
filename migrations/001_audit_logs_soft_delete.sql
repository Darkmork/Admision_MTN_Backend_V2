-- ============================================================================
-- Migration: Audit Logs & Soft Delete
-- Version: 001
-- Date: 2025-10-06
-- Description: Add audit_logs table and soft delete columns to critical tables
-- ============================================================================

-- ============================================================================
-- PART 1: AUDIT LOGS TABLE
-- ============================================================================

-- Create audit_logs table for compliance and traceability
CREATE TABLE IF NOT EXISTS audit_logs (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
  action VARCHAR(50) NOT NULL,  -- CREATE, UPDATE, DELETE, STATUS_CHANGE, etc.
  entity_type VARCHAR(50) NOT NULL,  -- application, user, evaluation, etc.
  entity_id INTEGER,
  old_values JSONB,  -- Previous state (for updates/deletes)
  new_values JSONB,  -- New state (for creates/updates)
  ip_address VARCHAR(45),  -- IPv4 or IPv6
  user_agent TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit_logs performance
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- Add comment to audit_logs table
COMMENT ON TABLE audit_logs IS 'Audit trail for all critical operations in the system';
COMMENT ON COLUMN audit_logs.action IS 'Action type: CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN, etc.';
COMMENT ON COLUMN audit_logs.entity_type IS 'Entity being modified: application, user, evaluation, etc.';
COMMENT ON COLUMN audit_logs.old_values IS 'Previous state before modification (JSON)';
COMMENT ON COLUMN audit_logs.new_values IS 'New state after modification (JSON)';

-- ============================================================================
-- PART 2: SOFT DELETE COLUMNS
-- ============================================================================

-- Add deleted_at column to applications table
ALTER TABLE applications
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

COMMENT ON COLUMN applications.deleted_at IS 'Soft delete timestamp. NULL means not deleted. Use for data recovery.';

-- Create index for soft delete queries (improves WHERE deleted_at IS NULL performance)
CREATE INDEX idx_applications_deleted_at ON applications(deleted_at)
WHERE deleted_at IS NULL;

-- Add deleted_at column to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp. NULL means not deleted.';

CREATE INDEX idx_users_deleted_at ON users(deleted_at)
WHERE deleted_at IS NULL;

-- Add deleted_at column to evaluations table
ALTER TABLE evaluations
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

COMMENT ON COLUMN evaluations.deleted_at IS 'Soft delete timestamp. NULL means not deleted.';

CREATE INDEX idx_evaluations_deleted_at ON evaluations(deleted_at)
WHERE deleted_at IS NULL;

-- Add deleted_at column to guardians table
ALTER TABLE guardians
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

COMMENT ON COLUMN guardians.deleted_at IS 'Soft delete timestamp. NULL means not deleted.';

CREATE INDEX idx_guardians_deleted_at ON guardians(deleted_at)
WHERE deleted_at IS NULL;

-- Add deleted_at column to students table
ALTER TABLE students
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

COMMENT ON COLUMN students.deleted_at IS 'Soft delete timestamp. NULL means not deleted.';

CREATE INDEX idx_students_deleted_at ON students(deleted_at)
WHERE deleted_at IS NULL;

-- Add deleted_at column to interviews table
ALTER TABLE interviews
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

COMMENT ON COLUMN interviews.deleted_at IS 'Soft delete timestamp. NULL means not deleted.';

CREATE INDEX idx_interviews_deleted_at ON interviews(deleted_at)
WHERE deleted_at IS NULL;

-- Add deleted_at column to documents table
ALTER TABLE documents
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

COMMENT ON COLUMN documents.deleted_at IS 'Soft delete timestamp. NULL means not deleted.';

CREATE INDEX idx_documents_deleted_at ON documents(deleted_at)
WHERE deleted_at IS NULL;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify audit_logs table exists
SELECT 'audit_logs table created' AS status
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'audit_logs';

-- Verify all deleted_at columns exist
SELECT
  table_name,
  column_name,
  data_type,
  is_nullable
FROM information_schema.columns
WHERE column_name = 'deleted_at'
  AND table_schema = 'public'
  AND table_name IN ('applications', 'users', 'evaluations', 'guardians', 'students', 'interviews', 'documents')
ORDER BY table_name;

-- Verify indexes were created
SELECT
  schemaname,
  tablename,
  indexname,
  indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND (indexname LIKE '%deleted_at%' OR indexname LIKE '%audit_logs%')
ORDER BY tablename, indexname;

-- ============================================================================
-- ROLLBACK SCRIPT (run manually if needed)
-- ============================================================================

/*
-- ROLLBACK AUDIT LOGS
DROP INDEX IF EXISTS idx_audit_logs_user_id;
DROP INDEX IF EXISTS idx_audit_logs_entity;
DROP INDEX IF EXISTS idx_audit_logs_created_at;
DROP INDEX IF EXISTS idx_audit_logs_action;
DROP TABLE IF EXISTS audit_logs CASCADE;

-- ROLLBACK SOFT DELETE COLUMNS
ALTER TABLE applications DROP COLUMN IF EXISTS deleted_at;
DROP INDEX IF EXISTS idx_applications_deleted_at;

ALTER TABLE users DROP COLUMN IF EXISTS deleted_at;
DROP INDEX IF EXISTS idx_users_deleted_at;

ALTER TABLE evaluations DROP COLUMN IF EXISTS deleted_at;
DROP INDEX IF EXISTS idx_evaluations_deleted_at;

ALTER TABLE guardians DROP COLUMN IF EXISTS deleted_at;
DROP INDEX IF EXISTS idx_guardians_deleted_at;

ALTER TABLE students DROP COLUMN IF EXISTS deleted_at;
DROP INDEX IF EXISTS idx_students_deleted_at;

ALTER TABLE interviews DROP COLUMN IF EXISTS deleted_at;
DROP INDEX IF EXISTS idx_interviews_deleted_at;

ALTER TABLE documents DROP COLUMN IF EXISTS deleted_at;
DROP INDEX IF EXISTS idx_documents_deleted_at;
*/
