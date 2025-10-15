#!/usr/bin/env node

/**
 * Railway Database Cleanup Script
 *
 * This script removes all data from the Railway production database
 * EXCEPT the admin user (jorge.gangale@mtn.cl).
 *
 * WARNING: This is DESTRUCTIVE and IRREVERSIBLE. Use with caution.
 *
 * Usage:
 *   node scripts/cleanup-railway-db.js --dry-run     # Test mode (no changes)
 *   node scripts/cleanup-railway-db.js --execute     # Execute cleanup
 */

const { Pool } = require('pg');

// Database configuration - uses Railway DATABASE_URL
if (!process.env.DATABASE_URL) {
  console.error('❌ ERROR: DATABASE_URL environment variable not found');
  console.error('   Run this script with: railway run node scripts/cleanup-railway-db.js --dry-run');
  process.exit(1);
}

const dbPool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: false, // Railway internal network
  max: 1
});

// Parse command line arguments
const args = process.argv.slice(2);
const isDryRun = args.includes('--dry-run');
const isExecute = args.includes('--execute');

if (!isDryRun && !isExecute) {
  console.error('❌ ERROR: You must specify --dry-run or --execute');
  console.error('   --dry-run:  Test mode (no changes)');
  console.error('   --execute:  Execute cleanup (DESTRUCTIVE)');
  process.exit(1);
}

console.log('');
console.log('========================================');
console.log('🗑️  RAILWAY DATABASE CLEANUP');
console.log('========================================');
console.log(`Mode: ${isDryRun ? '🔍 DRY RUN (no changes)' : '⚠️  EXECUTE (DESTRUCTIVE)'}`);
console.log('');

async function findAdminUser() {
  console.log('🔍 Step 1: Finding admin user...');

  const result = await dbPool.query(`
    SELECT id, email, role, first_name, last_name
    FROM users
    WHERE role = 'ADMIN'
    ORDER BY id
  `);

  if (result.rows.length === 0) {
    throw new Error('No admin user found in database');
  }

  console.log(`   Found ${result.rows.length} admin user(s):`);
  result.rows.forEach(user => {
    console.log(`   - ID ${user.id}: ${user.email} (${user.first_name} ${user.last_name})`);
  });

  // Find jorge.gangale@mtn.cl specifically
  const jorge = result.rows.find(u => u.email === 'jorge.gangale@mtn.cl');

  if (!jorge) {
    throw new Error('Admin user jorge.gangale@mtn.cl not found');
  }

  console.log('');
  console.log(`✅ Will preserve admin user: ${jorge.email} (ID ${jorge.id})`);
  console.log('');

  return jorge.id;
}

async function showDataCounts() {
  console.log('📊 Step 2: Current data counts...');

  const tables = [
    'users',
    'guardians',
    'students',
    'applications',
    'evaluations',
    'interviews',
    'interviewer_schedules',
    'documents',
    'notifications'
  ];

  for (const table of tables) {
    try {
      const result = await dbPool.query(`SELECT COUNT(*) as count FROM ${table}`);
      console.log(`   ${table.padEnd(25)}: ${result.rows[0].count} rows`);
    } catch (error) {
      console.log(`   ${table.padEnd(25)}: ERROR - ${error.message}`);
    }
  }
  console.log('');
}

async function executeCleanup(adminUserId, isDryRun) {
  console.log('🗑️  Step 3: Executing cleanup...');
  console.log('');

  const client = await dbPool.connect();

  try {
    await client.query('BEGIN');

    // Order matters: delete from child tables first to avoid FK violations
    const deleteQueries = [
      // 1. Documents (FK to applications)
      {
        name: 'documents',
        query: 'DELETE FROM documents WHERE application_id IN (SELECT id FROM applications WHERE created_by != $1)',
        params: [adminUserId]
      },

      // 2. Evaluations (FK to applications, assigned_to users)
      {
        name: 'evaluations',
        query: 'DELETE FROM evaluations WHERE application_id IN (SELECT id FROM applications WHERE created_by != $1) OR assigned_to_user_id != $1',
        params: [adminUserId]
      },

      // 3. Interviews (FK to applications, interviewer)
      {
        name: 'interviews',
        query: 'DELETE FROM interviews WHERE application_id IN (SELECT id FROM applications WHERE created_by != $1) OR interviewer_id != $1',
        params: [adminUserId]
      },

      // 4. Interviewer Schedules (FK to users)
      {
        name: 'interviewer_schedules',
        query: 'DELETE FROM interviewer_schedules WHERE user_id != $1',
        params: [adminUserId]
      },

      // 5. Notifications (FK to users, applications)
      {
        name: 'notifications',
        query: 'DELETE FROM notifications WHERE user_id != $1',
        params: [adminUserId]
      },

      // 6. Applications (FK to students, guardians, users)
      {
        name: 'applications',
        query: 'DELETE FROM applications WHERE created_by != $1',
        params: [adminUserId]
      },

      // 7. Students (FK to guardians)
      {
        name: 'students',
        query: 'DELETE FROM students WHERE id NOT IN (SELECT student_id FROM applications WHERE created_by = $1)',
        params: [adminUserId]
      },

      // 8. Guardians (FK to users)
      {
        name: 'guardians',
        query: 'DELETE FROM guardians WHERE user_id != $1',
        params: [adminUserId]
      },

      // 9. Users (parent table)
      {
        name: 'users',
        query: 'DELETE FROM users WHERE id != $1',
        params: [adminUserId]
      }
    ];

    for (const { name, query, params } of deleteQueries) {
      const result = await client.query(query, params);
      console.log(`   ✅ ${name.padEnd(25)}: ${result.rowCount} rows deleted`);
    }

    if (isDryRun) {
      console.log('');
      console.log('🔍 DRY RUN: Rolling back transaction (no changes made)');
      await client.query('ROLLBACK');
    } else {
      console.log('');
      console.log('⚠️  EXECUTE: Committing transaction (changes permanent)');
      await client.query('COMMIT');
    }

  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

async function verifyCleanup(adminUserId) {
  console.log('');
  console.log('✅ Step 4: Verification...');
  console.log('');

  // Check remaining users
  const usersResult = await dbPool.query('SELECT COUNT(*) as count FROM users');
  console.log(`   Users remaining: ${usersResult.rows[0].count} (should be 1)`);

  // Check admin user still exists
  const adminResult = await dbPool.query('SELECT id, email FROM users WHERE id = $1', [adminUserId]);
  if (adminResult.rows.length === 1) {
    console.log(`   ✅ Admin user preserved: ${adminResult.rows[0].email}`);
  } else {
    console.log('   ❌ ERROR: Admin user not found!');
  }

  // Check all other tables are empty
  const tables = ['guardians', 'students', 'applications', 'evaluations', 'interviews', 'interviewer_schedules', 'documents', 'notifications'];

  for (const table of tables) {
    try {
      const result = await dbPool.query(`SELECT COUNT(*) as count FROM ${table}`);
      const count = parseInt(result.rows[0].count);
      if (count === 0) {
        console.log(`   ✅ ${table.padEnd(25)}: ${count} rows (clean)`);
      } else {
        console.log(`   ⚠️  ${table.padEnd(25)}: ${count} rows (not empty)`);
      }
    } catch (error) {
      console.log(`   ❌ ${table.padEnd(25)}: ERROR - ${error.message}`);
    }
  }
}

async function main() {
  try {
    const adminUserId = await findAdminUser();
    await showDataCounts();
    await executeCleanup(adminUserId, isDryRun);
    await verifyCleanup(adminUserId);

    console.log('');
    console.log('========================================');
    if (isDryRun) {
      console.log('✅ DRY RUN COMPLETE');
      console.log('========================================');
      console.log('');
      console.log('No changes were made to the database.');
      console.log('Review the output above to see what would be deleted.');
      console.log('');
      console.log('To execute the cleanup for real, run:');
      console.log('  railway run node scripts/cleanup-railway-db.js --execute');
    } else {
      console.log('✅ CLEANUP COMPLETE');
      console.log('========================================');
      console.log('');
      console.log('⚠️  All data has been deleted except the admin user.');
      console.log('   Database is now in a clean state.');
    }
    console.log('');

  } catch (error) {
    console.error('');
    console.error('========================================');
    console.error('❌ ERROR');
    console.error('========================================');
    console.error(error.message);
    console.error('');
    console.error('Stack trace:');
    console.error(error.stack);
    process.exit(1);
  } finally {
    await dbPool.end();
  }
}

main();
