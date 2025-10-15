#!/usr/bin/env node

/**
 * Railway Database Cleanup Script - SIMPLIFIED VERSION
 *
 * This script removes ALL DATA from Railway except admin user (ID 25)
 * Simplified to work regardless of table schema differences
 */

const { Pool } = require('pg');

if (!process.env.DATABASE_URL) {
  console.error('❌ ERROR: DATABASE_URL environment variable not found');
  process.exit(1);
}

const dbPool = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: 1
});

const args = process.argv.slice(2);
const isDryRun = args.includes('--dry-run');
const isExecute = args.includes('--execute');

if (!isDryRun && !isExecute) {
  console.error('❌ ERROR: You must specify --dry-run or --execute');
  process.exit(1);
}

const ADMIN_USER_ID = 25; // jorge.gangale@mtn.cl

console.log('');
console.log('========================================');
console.log('🗑️  RAILWAY DATABASE CLEANUP - SIMPLE');
console.log('========================================');
console.log(`Mode: ${isDryRun ? '🔍 DRY RUN (no changes)' : '⚠️  EXECUTE (DESTRUCTIVE)'}`);
console.log('');

async function main() {
  const client = await dbPool.connect();

  try {
    console.log('🔍 Step 1: Verifying admin user...');
    const adminCheck = await client.query('SELECT id, email, role FROM users WHERE id = $1', [ADMIN_USER_ID]);

    if (adminCheck.rows.length === 0) {
      throw new Error(`Admin user with ID ${ADMIN_USER_ID} not found`);
    }

    console.log(`   ✅ Admin user: ${adminCheck.rows[0].email} (ID ${ADMIN_USER_ID})`);
    console.log('');

    console.log('📊 Step 2: Current data counts...');

    const tables = [
      'users',
      'guardians',
      'students',
      'applications',
      'evaluations',
      'interviews',
      'interviewer_schedules',
      'documents'
    ];

    for (const table of tables) {
      try {
        const result = await client.query(`SELECT COUNT(*) as count FROM ${table}`);
        console.log(`   ${table.padEnd(25)}: ${result.rows[0].count} rows`);
      } catch (error) {
        console.log(`   ${table.padEnd(25)}: ERROR - ${error.message}`);
      }
    }

    console.log('');
    console.log('🗑️  Step 3: Executing cleanup...');
    console.log('');

    await client.query('BEGIN');

    try {
      // Delete in correct order (children first, parents last)

      // 1. Documents (if exists)
      try {
        const result = await client.query('DELETE FROM documents WHERE 1=1 RETURNING id');
        console.log(`   ✅ documents                : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  documents                : SKIP - ${error.message}`);
      }

      // 2. Evaluations - DELETE ALL
      try {
        const result = await client.query('DELETE FROM evaluations WHERE 1=1');
        console.log(`   ✅ evaluations              : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  evaluations              : SKIP - ${error.message}`);
      }

      // 3. Interviews - DELETE ALL
      try {
        const result = await client.query('DELETE FROM interviews WHERE 1=1');
        console.log(`   ✅ interviews               : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  interviews               : SKIP - ${error.message}`);
      }

      // 4. Interviewer Schedules - DELETE ALL
      try {
        const result = await client.query('DELETE FROM interviewer_schedules WHERE 1=1');
        console.log(`   ✅ interviewer_schedules    : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  interviewer_schedules    : SKIP - ${error.message}`);
      }

      // 5. Applications - DELETE ALL
      try {
        const result = await client.query('DELETE FROM applications WHERE 1=1');
        console.log(`   ✅ applications             : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  applications             : SKIP - ${error.message}`);
      }

      // 6. Students - DELETE ALL
      try {
        const result = await client.query('DELETE FROM students WHERE 1=1');
        console.log(`   ✅ students                 : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  students                 : SKIP - ${error.message}`);
      }

      // 7. Guardians - DELETE ALL
      try {
        const result = await client.query('DELETE FROM guardians WHERE 1=1');
        console.log(`   ✅ guardians                : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  guardians                : SKIP - ${error.message}`);
      }

      // 8. Users (keep only admin)
      try {
        const result = await client.query('DELETE FROM users WHERE id != $1', [ADMIN_USER_ID]);
        console.log(`   ✅ users                    : ${result.rowCount} rows deleted`);
      } catch (error) {
        console.log(`   ⚠️  users                    : SKIP - ${error.message}`);
      }

      console.log('');

      if (isDryRun) {
        console.log('🔍 DRY RUN: Rolling back transaction (no changes made)');
        await client.query('ROLLBACK');
      } else {
        console.log('⚠️  EXECUTE: Committing transaction (changes permanent)');
        await client.query('COMMIT');
      }

    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    }

    console.log('');
    console.log('✅ Step 4: Verification...');
    console.log('');

    // Check remaining data
    for (const table of tables) {
      try {
        const result = await client.query(`SELECT COUNT(*) as count FROM ${table}`);
        const count = parseInt(result.rows[0].count);

        if (table === 'users') {
          if (count === 1) {
            console.log(`   ✅ ${table.padEnd(25)}: ${count} row (admin only)`);
          } else {
            console.log(`   ⚠️  ${table.padEnd(25)}: ${count} rows (expected 1)`);
          }
        } else {
          if (count === 0) {
            console.log(`   ✅ ${table.padEnd(25)}: ${count} rows (clean)`);
          } else {
            console.log(`   ⚠️  ${table.padEnd(25)}: ${count} rows (not empty)`);
          }
        }
      } catch (error) {
        console.log(`   ⚠️  ${table.padEnd(25)}: ERROR - ${error.message}`);
      }
    }

    // Verify admin user still exists
    const finalCheck = await client.query('SELECT id, email FROM users WHERE id = $1', [ADMIN_USER_ID]);
    if (finalCheck.rows.length === 1) {
      console.log('');
      console.log(`   ✅ Admin user preserved: ${finalCheck.rows[0].email}`);
    } else {
      console.log('');
      console.log('   ❌ ERROR: Admin user not found after cleanup!');
    }

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
      console.log('  node scripts/cleanup-railway-simple.js --execute');
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
    if (error.stack) {
      console.error('Stack trace:');
      console.error(error.stack);
    }
    process.exit(1);
  } finally {
    client.release();
    await dbPool.end();
  }
}

main();
