/**
 * Generate BCrypt hash for admin password
 * Run: node generate-admin-password.js
 */

const bcrypt = require('bcryptjs');

const password = 'admin123';

bcrypt.hash(password, 10, (err, hash) => {
  if (err) {
    console.error('Error generating hash:', err);
    return;
  }

  console.log('\n=== Admin Password Hash ===');
  console.log('Password:', password);
  console.log('Hash:', hash);
  console.log('\nSQL Insert Statement:');
  console.log(`
INSERT INTO users (email, password, role, first_name, last_name)
VALUES (
    'jorge.gangale@mtn.cl',
    '${hash}',
    'ADMIN',
    'Jorge',
    'Gangale'
) ON CONFLICT (email) DO NOTHING;
  `);
  console.log('\n=========================\n');
});
