#!/usr/bin/env node
/**
 * BCrypt Performance Test
 *
 * Este script prueba el rendimiento de BCrypt con diferentes números de rounds
 * para identificar si BCrypt es el cuello de botella en Railway.
 *
 * Uso:
 *   node test-bcrypt-performance.js
 */

const bcrypt = require('bcryptjs');

const password = 'admin123';

async function testBCryptRounds(rounds) {
  console.log(`\n=== Testing ${rounds} rounds ===`);

  // Hash
  console.time(`  Hash (${rounds} rounds)`);
  const hash = await bcrypt.hash(password, rounds);
  console.timeEnd(`  Hash (${rounds} rounds)`);

  // Compare
  console.time(`  Compare (${rounds} rounds)`);
  const isValid = await bcrypt.compare(password, hash);
  console.timeEnd(`  Compare (${rounds} rounds)`);

  console.log(`  Result: ${isValid ? '✅ Valid' : '❌ Invalid'}`);
  console.log(`  Hash: ${hash.substring(0, 40)}...`);

  return { rounds, isValid };
}

async function main() {
  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║  BCrypt Performance Test                                   ║');
  console.log('╚════════════════════════════════════════════════════════════╝');
  console.log('');
  console.log(`Password: ${password}`);
  console.log(`Platform: ${process.platform}`);
  console.log(`Node version: ${process.version}`);
  console.log(`CPU architecture: ${process.arch}`);
  console.log('');

  // Test different rounds
  const roundsToTest = [6, 7, 8, 9, 10, 11, 12];

  console.log('Testing BCrypt with different rounds...');
  console.log('(Lower rounds = faster but less secure)');
  console.log('');

  const results = [];

  for (const rounds of roundsToTest) {
    const result = await testBCryptRounds(rounds);
    results.push(result);
  }

  console.log('');
  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║  Summary & Recommendations                                 ║');
  console.log('╚════════════════════════════════════════════════════════════╝');
  console.log('');
  console.log('BCrypt Rounds Guide:');
  console.log('  6 rounds:  Fast but NOT recommended (too weak)');
  console.log('  7 rounds:  Fast but risky (minimal security)');
  console.log('  8 rounds:  GOOD BALANCE (256 iterations, ~acceptable security)');
  console.log('  9 rounds:  Better security (512 iterations)');
  console.log(' 10 rounds:  CURRENT (1,024 iterations, recommended)');
  console.log(' 11 rounds:  High security (2,048 iterations, slow)');
  console.log(' 12 rounds:  Maximum security (4,096 iterations, very slow)');
  console.log('');
  console.log('Recommendation for Railway:');
  console.log('  - If 10 rounds takes >5s on Railway → Use 8 rounds');
  console.log('  - If 8 rounds takes <2s on Railway → Keep 8 rounds');
  console.log('  - Add rate limiting (3 attempts/minute) for extra security');
  console.log('');
  console.log('Note: Existing password hashes will still work after changing');
  console.log('      rounds because BCrypt auto-detects the number of rounds');
  console.log('      from the hash itself.');
  console.log('');
}

main().catch(console.error);
