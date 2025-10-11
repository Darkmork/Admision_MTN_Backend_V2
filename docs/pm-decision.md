# Package Manager Decision

**Chosen:** npm

**Date:** October 11, 2025
**Node Version:** 20.x LTS
**npm Version:** 10.x+

## Rationale

### Primary Factors

1. **Single Lockfile Present**: `package-lock.json` exists and is the only lockfile in the repository
   - No `pnpm-lock.yaml` or `yarn.lock` detected
   - Lockfile is synchronized with `package.json` (all dependencies resolved)

2. **CI/CD Configuration**: GitHub Actions workflow already configured for npm
   - Uses `npm ci` for deterministic installs (line 58, 327 in `.github/workflows/ci.yml`)
   - Node.js setup includes `cache: 'npm'` for performance
   - No pnpm or yarn references in CI configuration

3. **Railway Compatibility**: Nixpacks auto-detection works seamlessly with npm
   - Detects `package-lock.json` automatically
   - No custom buildpack configuration required
   - Standard build command: `npm ci && npm run build`

4. **Project Configuration**: No explicit package manager preference
   - No `packageManager` field in `package.json`
   - No `.npmrc` with pnpm-specific settings
   - No `pnpm-workspace.yaml` for workspace configuration

5. **Dependency Ecosystem**: All dependencies compatible with npm
   - Express 5.1.0, PostgreSQL pg driver, Opossum circuit breaker
   - No pnpm-specific features required
   - Standard npm semver ranges work correctly

### Alternative Considered

**pnpm**: Not chosen because:
- No pnpm lockfile present
- No workspace configuration detected
- CI already standardized on npm
- Railway deployment simpler with npm (no corepack setup needed)
- No performance issues with current npm setup (274 packages install in ~3s)

## Configuration

### Node.js Version

**Specified in:**
- `.nvmrc`: `v20`
- `package.json` engines: `"node": ">=20 <21"`

**Rationale for Node 20:**
- Current LTS version (Long Term Support until April 2026)
- Active maintenance and security updates
- Native ECMAScript modules support
- Performance improvements over Node 18
- Compatible with all project dependencies

### npm Version

**Specified in:**
- `package.json` engines: `"npm": ">=10"`

**Current version:** 11.4.1 (detected locally)

**Features utilized:**
- `npm ci` for frozen lockfile installs
- Improved dependency resolution algorithm
- Better audit security features
- Workspace support (if needed in future)

### Lockfile Strategy

**File:** `package-lock.json` (lockfileVersion 3)

**Approach:**
- **Frozen lockfile in CI**: `npm ci` ensures exact dependency versions
- **Exact versions in lockfile**: All 274 packages with resolved versions
- **Synchronized state**: `package.json` and `package-lock.json` match perfectly
- **No manual edits**: Lockfile generated/updated only via npm commands

**Verification:**
```bash
# Clean install test
rm -rf node_modules
npm ci  # ✅ Succeeds in <3 seconds

# Module resolution test
node -e "require('express'); require('pg'); require('opossum');"
# ✅ All critical dependencies load successfully
```

## CI/CD Integration

### GitHub Actions

**Workflow:** `.github/workflows/ci.yml`

**Backend job (lines 18-83):**
```yaml
- name: Setup Node.js
  uses: actions/setup-node@v4
  with:
    node-version: ${{ env.NODE_VERSION }}  # 20.x
    cache: 'npm'

- name: Install dependencies
  run: npm ci

- name: Run tests
  run: npm test || echo "No tests configured yet"
```

**Key aspects:**
- Clean, idempotent installs with `npm ci`
- Dependency caching for faster builds
- No confusing conditionals or package manager detection
- Smoke tests validate all services start correctly

**Smoke tests job (lines 294-395):**
- Validates mock services start successfully
- Uses `npm ci` for dependency installation (line 327)
- Tests full NGINX + Node.js microservices stack
- No package manager confusion

### Frontend Job (Separate Repository)

**Note:** Frontend job (lines 91-165) handles multiple package managers because it builds from a separate repository (`Darkmork/Admision_MTN_front`). This is appropriate and not a "confusing conditional" - it's package manager auto-detection for an external repo.

## Railway Deployment

### Configuration

**Auto-detection:** Nixpacks detects `package-lock.json` automatically

**Build command:**
```bash
npm ci && npm run build
```

**Start command:**
```bash
npm start
```

**Nixpacks plan:**
- Detects Node.js 20.x from `.nvmrc`
- Installs dependencies with `npm ci`
- Runs build script if present
- Executes `npm start` to launch services

### Environment Variables

Railway requires these environment variables:
```bash
# Database
DB_HOST=<railway-postgres-host>
DB_PORT=5432
DB_NAME=Admisión_MTN_DB
DB_USERNAME=<railway-postgres-user>
DB_PASSWORD=<railway-postgres-password>

# JWT
JWT_SECRET=<secure-random-secret>
JWT_EXPIRATION_TIME=86400000

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=<email-address>
SMTP_PASSWORD=<app-specific-password>
EMAIL_MOCK_MODE=false

# Services
PORT=8080
NODE_ENV=production
```

### Health Check

Railway health check endpoint:
```bash
GET /health
# Returns: {"status": "ok", "service": "user-service", "timestamp": "..."}
```

**Multiple services:** If deploying individual services, use respective health endpoints:
- User Service: `http://localhost:8082/health`
- Application Service: `http://localhost:8083/health`
- Evaluation Service: `http://localhost:8084/health`
- Notification Service: `http://localhost:8085/health`
- Dashboard Service: `http://localhost:8086/health`
- Guardian Service: `http://localhost:8087/health`

### Deployment Notes

1. **Start Script**: Railway executes `start` script from `package.json`
   - Current: `"start": "node start-railway.js"`
   - This starts all 6 mock services

2. **Build Phase**: No build script required for Node.js services
   - Services are plain JavaScript (no TypeScript compilation)
   - Dependencies installed via `npm ci`

3. **Port Configuration**: Railway assigns `PORT` environment variable
   - Services listen on ports 8082-8087
   - NGINX gateway (if used) listens on `PORT` (default 8080)

4. **Database Migration**: Run seed script manually before first deploy
   ```bash
   # In Railway shell or local connection
   chmod +x scripts/seed-test-data.sh
   PGPASSWORD=<password> ./scripts/seed-test-data.sh
   ```

## Monorepo Support

**Current status:** Not a monorepo

**If monorepo in future:**
- npm workspaces configuration would go in `package.json`
- Root lockfile would cover all workspace packages
- CI would use `npm ci --workspaces` for parallel installs

## Edge Cases Handled

1. **No pnpm lockfile**: Confirmed not present, no removal needed
2. **Private registries**: No `.npmrc` with custom registries detected
3. **Missing scripts**: CI uses `|| echo "No X configured"` fallback pattern
4. **Security vulnerabilities**: CI runs `npm audit` with moderate threshold
5. **Git conflicts**: Feature branch created from clean state

## Quality Assurance Checklist

- [x] Only one lockfile exists (`package-lock.json`)
- [x] Lockfile is committed to version control
- [x] `npm ci` succeeds in clean directory (verified)
- [x] All critical dependencies load successfully (express, pg, opossum)
- [x] Build script runs (no build script, services are plain JS)
- [x] Test script has fallback (uses `|| echo` pattern)
- [x] CI workflow syntax is valid (existing workflow unchanged)
- [x] `package.json` engines field set correctly
- [x] `.nvmrc` matches Node version (v20)
- [x] No secrets exposed in configuration files

## Success Criteria Met

1. **Single lockfile:** `package-lock.json` present and synchronized
2. **CI frozen lockfile:** Uses `npm ci` consistently
3. **Railway compatible:** Nixpacks auto-detection works
4. **Local validation:** Clean install and module resolution tested
5. **Documentation complete:** This decision document comprehensive

## Future Considerations

1. **Dependency updates:** Use `npm update` to update within semver ranges
2. **Security patches:** Run `npm audit fix` regularly
3. **Major version upgrades:** Test in feature branch before merging
4. **Lockfile conflicts:** Resolve by regenerating with `npm install`
5. **Performance monitoring:** Current install time ~3s is acceptable

---

**Decision Rationale Summary:**
npm chosen because it's the existing standard with `package-lock.json` present, CI already configured for npm, Railway compatibility is seamless, and no pnpm-specific features are required. The lockfile is synchronized and all validation tests pass.
