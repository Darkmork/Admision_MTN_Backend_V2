# Package Manager Decision

**Chosen:** npm

**Date:** October 12, 2025

**Rationale:**

This repository uses npm as the exclusive package manager for the following reasons:

1. **Explicit Configuration**: The `package.json` contains an explicit `engines` field specifying npm:
   ```json
   "engines": {
     "node": ">=20 <21",
     "npm": ">=10"
   }
   ```

2. **Single Lockfile**: Only `package-lock.json` exists in the repository (no `pnpm-lock.yaml` or `yarn.lock`)

3. **CI/CD Consistency**: GitHub Actions workflow (`.github/workflows/ci.yml`) uses `npm ci` for deterministic installs:
   - Line 46: `cache: 'npm'`
   - Line 58: `run: npm ci`
   - Line 327: `run: npm ci`

4. **Node.js Version**: Node.js 20.x LTS is the standard version
   - Verified in `.nvmrc`: `v20`
   - CI environment variable: `NODE_VERSION: '20.x'`
   - Current local version: `v20.19.2`

5. **npm Version**: npm 11.4.1 (bundled with Node.js 20)

## Configuration

### Lockfile
- **File**: `package-lock.json` (lockfileVersion 3)
- **Status**: Synchronized with package.json (as of commit 5672a8c)
- **Total packages**: 293 packages + 1 devDependency

### Dependencies (17 total)
| Package | Version | Purpose |
|---------|---------|---------|
| express | 5.1.0 | Web framework (upgraded from 4.x) |
| pg | 8.16.3 | PostgreSQL client with connection pooling |
| bcryptjs | 3.0.2 | Password hashing |
| axios | 1.12.2 | HTTP client |
| nodemailer | 7.0.9 | Email sending |
| opossum | 9.0.0 | Circuit breaker pattern |
| cors | 2.8.5 | CORS middleware |
| jsonwebtoken | 9.0.2 | JWT authentication |
| winston | 3.18.3 | Logging framework |
| compression | 1.8.1 | HTTP compression |
| cookie-parser | 1.4.7 | Cookie parsing |
| csrf-csrf | 4.0.3 | CSRF protection |
| dotenv | 17.2.3 | Environment variables |
| http-proxy-middleware | 3.0.5 | HTTP proxy |
| multer | 2.0.2 | File upload handling |
| pdfkit | 0.17.2 | PDF generation |
| eslint | 9.37.0 | Linting (devDependency) |

### CI/CD Pipeline
- **Platform**: GitHub Actions
- **Workflow**: `.github/workflows/ci.yml`
- **Install command**: `npm ci` (frozen lockfile install)
- **Build command**: None (Node.js services run directly)
- **Test command**: `npm test` (runs `integration-tests.sh`)

### Validation Steps
The lockfile synchronization was validated through:

1. Clean install test: `rm -rf node_modules && npm ci`
   - Result: 293 packages installed in 757ms, 0 vulnerabilities

2. Module resolution test: `node -e "require('express')"`
   - Result: Express 5.1.0 loaded successfully

3. Dependency tree verification: `npm list --depth=0`
   - Result: All 17 dependencies correctly resolved

## Recent Fix: Lockfile Desynchronization (Oct 12, 2025)

### Problem
The CI pipeline was failing with error:
```
npm error `npm ci` can only install packages when your package.json and
package-lock.json or npm-shrinkwrap.json are in sync. Please update your
lock file with `npm install` before continuing.
```

**Root Cause**:
- `package.json` was updated with `express@^5.1.0` and `dotenv@^17.2.3`
- `package-lock.json` still referenced `express@4.x` dependencies
- Missing entries for 60+ packages in the lockfile

### Solution
1. Removed existing `node_modules` and `package-lock.json`
2. Regenerated lockfile with `npm install`
3. Validated synchronization with `npm ci` in clean environment
4. Committed synchronized lockfile (commit 5672a8c)

### Verification
```bash
# Test npm ci works
cd Admision_MTN_backend
rm -rf node_modules
npm ci

# Expected output:
# added 293 packages, and audited 294 packages in 757ms
# 0 vulnerabilities
```

## Railway Deployment

**Railway/Nixpacks Configuration:**
- Auto-detects `package-lock.json` and uses npm
- Build command: `npm ci && npm run build` (if build script exists)
- Start command: `npm start` (runs `start-railway.js`)

**Environment Variables Required:**
- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- JWT_SECRET, JWT_EXPIRATION_TIME
- SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD
- EMAIL_MOCK_MODE (true/false)

## Maintenance

### Updating Dependencies
```bash
# Update a specific package
npm update <package-name>

# Update all packages (respecting semver)
npm update

# Check for outdated packages
npm outdated

# After updates, always validate
rm -rf node_modules
npm ci
```

### Security Audits
```bash
# Check for vulnerabilities
npm audit

# Fix automatically (semver-compatible updates)
npm audit fix

# Force major version updates (use with caution)
npm audit fix --force
```

### Adding New Dependencies
```bash
# Add production dependency
npm install <package-name>

# Add development dependency
npm install --save-dev <package-name>

# Always commit both files
git add package.json package-lock.json
git commit -m "chore(deps): add <package-name>"
```

## Troubleshooting

### Lockfile Out of Sync
If you see "npm ci can only install packages when your package.json and package-lock.json are in sync":

```bash
# Regenerate lockfile
rm -rf node_modules package-lock.json
npm install

# Validate it works
rm -rf node_modules
npm ci

# Commit changes
git add package.json package-lock.json
git commit -m "chore(deps): synchronize lockfile"
```

### CI Failures
If CI fails with npm ci errors:

1. Check that both `package.json` and `package-lock.json` are committed
2. Verify lockfile is not corrupted: `npm ci` should work locally
3. Ensure Node.js version matches CI environment (20.x)
4. Check GitHub Actions cache: May need to clear cache and re-run

### Module Not Found
If services fail to start with "Cannot find module 'X'":

```bash
# Verify module is in package.json
cat package.json | grep <module-name>

# Reinstall dependencies
rm -rf node_modules
npm ci

# Test module loading
node -e "require('<module-name>'); console.log('OK')"
```

## References

- [npm ci documentation](https://docs.npmjs.com/cli/v10/commands/npm-ci)
- [package-lock.json format](https://docs.npmjs.com/cli/v10/configuring-npm/package-lock-json)
- [Node.js 20.x LTS](https://nodejs.org/en/blog/release/v20.0.0)
- Project CI workflow: `.github/workflows/ci.yml`
