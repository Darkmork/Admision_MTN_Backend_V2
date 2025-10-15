const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const dbPool = new Pool({
  host: 'localhost',
  port: 5432,
  database: 'Admisión_MTN_DB',
  user: 'admin',
  password: 'admin123',
  ssl: false,
  max: 20
});

async function testLogin() {
  const email = 'jorge.gangale@mtn.cl';
  const password = 'admin123';

  console.log('🔍 Testing login for:', email);

  const client = await dbPool.connect();
  try {
    // Step 1: Query user
    console.log('Step 1: Querying database...');
    const userQuery = await client.query(
      'SELECT id, first_name, last_name, email, role, subject, password, active, email_verified FROM users WHERE email = $1',
      [email.toLowerCase().trim()]
    );

    console.log('Query result:', userQuery.rows.length, 'users found');

    if (userQuery.rows.length === 0) {
      console.log('❌ User not found');
      return;
    }

    const user = userQuery.rows[0];
    console.log('✅ User found:', { id: user.id, email: user.email, role: user.role, active: user.active });

    // Step 2: Check active status
    if (!user.active) {
      console.log('❌ User is inactive');
      return;
    }

    // Step 3: Verify password
    console.log('Step 2: Verifying password...');
    console.log('Password to check:', password);
    console.log('Stored hash:', user.password.substring(0, 20) + '...');

    const isValid = await bcrypt.compare(password, user.password);
    console.log('Password valid:', isValid);

    if (!isValid) {
      console.log('❌ Invalid password');
      return;
    }

    console.log('✅ Login successful!');

    // Step 4: Update last_login
    console.log('Step 3: Updating last_login...');
    await client.query(
      'UPDATE users SET last_login = NOW() WHERE id = $1',
      [user.id]
    );

    console.log('✅ Last login updated');

    // Step 5: Generate JWT token
    const header = Buffer.from(JSON.stringify({alg: "HS256", typ: "JWT"})).toString('base64');
    const payload = Buffer.from(JSON.stringify({
      userId: user.id.toString(),
      email: user.email,
      role: user.role,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60)
    })).toString('base64');
    const signature = "mock-signature";
    const token = `${header}.${payload}.${signature}`;

    console.log('✅ Token generated:', token.substring(0, 50) + '...');

    const response = {
      success: true,
      message: 'Login exitoso',
      token: token,
      id: user.id.toString(),
      firstName: user.first_name,
      lastName: user.last_name,
      email: user.email,
      role: user.role,
      subject: user.subject
    };

    console.log('\n📋 Final response:');
    console.log(JSON.stringify(response, null, 2));

  } catch (error) {
    console.error('💥 Error during login:', error.message);
    console.error('Stack:', error.stack);
  } finally {
    client.release();
    process.exit(0);
  }
}

testLogin();
