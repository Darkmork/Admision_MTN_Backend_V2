# Redis Migration Plan - Distributed Cache Implementation

## Executive Summary

This document outlines the migration plan to transition from in-memory cache to Redis distributed cache for the Sistema de Admisión MTN. The migration will improve scalability, enable cache sharing across service instances, and provide persistence options.

**Current State:** In-memory cache in 3 services (User, Evaluation, Dashboard)
**Target State:** Redis distributed cache shared across all services
**Estimated Timeline:** 2-3 weeks
**Estimated Effort:** 40-60 hours

---

## 1. Current Architecture Analysis

### Existing In-Memory Cache Implementation

**Services with caching:**
- **User Service** (mock-user-service.js)
  - 2 cached endpoints
  - TTL: 10-30 minutes
  - Cache hit rate: ~50%

- **Evaluation Service** (mock-evaluation-service.js)
  - 3 cached endpoints
  - TTL: 5-60 minutes
  - Cache hit rate: ~33%

- **Dashboard Service** (mock-dashboard-service.js)
  - 5 cached endpoints
  - TTL: 3-15 minutes
  - Cache hit rate: ~80%

**Current Implementation (SimpleCache class):**
```javascript
class SimpleCache {
  constructor() {
    this.cache = new Map();
  }

  set(key, value, ttlMs) {
    const expiry = Date.now() + ttlMs;
    this.cache.set(key, { value, expiry });
  }

  get(key) {
    const entry = this.cache.get(key);
    if (!entry) return null;
    if (Date.now() > entry.expiry) {
      this.cache.delete(key);
      return null;
    }
    return entry.value;
  }
}
```

**Limitations of current approach:**
- Cache not shared between service instances (horizontal scaling impossible)
- Cache lost on service restart
- Memory usage grows with data volume
- No advanced features (patterns, pub/sub, atomic operations)

---

## 2. Redis Architecture Design

### 2.1 Deployment Architecture

```
┌─────────────────────────────────────────────┐
│         NGINX Gateway (8080)                │
└─────────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
    ┌───▼───┐   ┌──▼──┐   ┌───▼───┐
    │User   │   │Eval │   │Dash   │
    │Service│   │Svc  │   │Service│
    │:8082  │   │:8084│   │:8086  │
    └───┬───┘   └──┬──┘   └───┬───┘
        │          │          │
        └──────────┼──────────┘
                   │
        ┌──────────▼──────────┐
        │   Redis Server      │
        │   localhost:6379    │
        │   (Single Instance) │
        └─────────────────────┘
```

### 2.2 Redis Configuration

**Development/Staging Environment:**
```redis
# redis.conf
bind 127.0.0.1
port 6379
maxmemory 256mb
maxmemory-policy allkeys-lru  # Least Recently Used eviction
appendonly yes                 # Persistence enabled
appendfsync everysec          # Write to disk every second
```

**Production Environment:**
```redis
# redis.conf
bind 0.0.0.0
port 6379
maxmemory 1gb
maxmemory-policy allkeys-lru
appendonly yes
appendfsync everysec
requirepass <REDIS_PASSWORD>  # Authentication required
```

### 2.3 Redis Key Naming Convention

**Pattern:** `{service}:{entity}:{identifier}:{version}`

**Examples:**
```
user:roles:all:v1
user:school-staff:public:v1
eval:interviewers:public:v1
eval:evaluators:TEACHER:v1
dash:stats:general:v1
dash:analytics:metrics:v1
```

**Benefits:**
- Easy to identify cache owner
- Supports pattern-based deletion
- Version support for cache invalidation
- Organized key namespace

---

## 3. Implementation Plan

### Phase 1: Infrastructure Setup (Week 1)

#### 1.1 Install Redis

**macOS (Development):**
```bash
# Install Redis via Homebrew
brew install redis

# Start Redis server
brew services start redis

# Verify installation
redis-cli ping  # Should return PONG
```

**Production (Docker):**
```bash
# docker-compose.yml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}

volumes:
  redis-data:
```

#### 1.2 Install Node.js Redis Client

```bash
cd Admision_MTN_backend
npm install redis@4.6.7 --save
```

**Why redis@4.6.7:**
- Latest stable version
- Promise-based API (async/await support)
- Connection pooling built-in
- TypeScript support

#### 1.3 Create Redis Utility

**File:** `utils/redisCache.js`

```javascript
const redis = require('redis');

// Singleton Redis client
let redisClient = null;

// Configuration
const REDIS_CONFIG = {
  host: process.env.REDIS_HOST || 'localhost',
  port: process.env.REDIS_PORT || 6379,
  password: process.env.REDIS_PASSWORD || undefined,
  db: process.env.REDIS_DB || 0,
  retryStrategy: (times) => {
    const delay = Math.min(times * 50, 2000);
    return delay;
  }
};

/**
 * Initialize Redis client (call once on service startup)
 */
async function initRedis() {
  if (redisClient) return redisClient;

  redisClient = redis.createClient(REDIS_CONFIG);

  redisClient.on('error', (err) => {
    console.error('❌ Redis Client Error:', err);
  });

  redisClient.on('connect', () => {
    console.log('✅ Redis connected');
  });

  await redisClient.connect();
  return redisClient;
}

/**
 * Get value from Redis cache
 */
async function get(key) {
  if (!redisClient) await initRedis();

  try {
    const value = await redisClient.get(key);
    if (!value) return null;

    return JSON.parse(value);
  } catch (error) {
    console.error(`❌ Redis GET error for key ${key}:`, error);
    return null;  // Graceful degradation
  }
}

/**
 * Set value in Redis cache with TTL
 */
async function set(key, value, ttlSeconds) {
  if (!redisClient) await initRedis();

  try {
    const serialized = JSON.stringify(value);
    await redisClient.setEx(key, ttlSeconds, serialized);
    return true;
  } catch (error) {
    console.error(`❌ Redis SET error for key ${key}:`, error);
    return false;  // Graceful degradation
  }
}

/**
 * Delete key(s) from Redis cache
 */
async function del(pattern) {
  if (!redisClient) await initRedis();

  try {
    // If pattern contains wildcards, use SCAN + DEL
    if (pattern.includes('*')) {
      let cursor = 0;
      let deletedCount = 0;

      do {
        const result = await redisClient.scan(cursor, {
          MATCH: pattern,
          COUNT: 100
        });

        cursor = result.cursor;
        const keys = result.keys;

        if (keys.length > 0) {
          await redisClient.del(keys);
          deletedCount += keys.length;
        }
      } while (cursor !== 0);

      return deletedCount;
    } else {
      // Single key deletion
      return await redisClient.del(pattern);
    }
  } catch (error) {
    console.error(`❌ Redis DEL error for pattern ${pattern}:`, error);
    return 0;
  }
}

/**
 * Get cache statistics
 */
async function getStats() {
  if (!redisClient) await initRedis();

  try {
    const info = await redisClient.info('stats');
    const keyspace = await redisClient.info('keyspace');

    return {
      totalKeys: await redisClient.dbSize(),
      info: info,
      keyspace: keyspace
    };
  } catch (error) {
    console.error('❌ Redis STATS error:', error);
    return { error: error.message };
  }
}

/**
 * Close Redis connection (call on service shutdown)
 */
async function closeRedis() {
  if (redisClient) {
    await redisClient.quit();
    redisClient = null;
  }
}

module.exports = {
  initRedis,
  get,
  set,
  del,
  getStats,
  closeRedis
};
```

### Phase 2: Service Migration (Week 2)

#### 2.1 User Service Migration

**File:** `mock-user-service.js`

**Changes:**
```javascript
// Line 1: Add Redis import
const redisCache = require('./utils/redisCache');

// Line 20: Initialize Redis on startup
app.listen(8082, async () => {
  await redisCache.initRedis();
  console.log('User Service running on port 8082');
});

// Line 1075: Replace SimpleCache with Redis
// BEFORE (in-memory):
const cachedRoles = cache.get('users:roles');
if (cachedRoles) {
  return res.json(cachedRoles);
}

// AFTER (Redis):
const cachedRoles = await redisCache.get('user:roles:all:v1');
if (cachedRoles) {
  return res.json(cachedRoles);
}

// After database query:
await redisCache.set('user:roles:all:v1', response, 1800); // 30 min TTL
```

**Affected endpoints:**
1. `GET /api/users/roles` - 30 min TTL
2. `GET /api/users/public/school-staff` - 10 min TTL

#### 2.2 Evaluation Service Migration

**File:** `mock-evaluation-service.js`

**Affected endpoints:**
1. `GET /api/interviews/public/interviewers` - 5 min TTL
2. `GET /api/evaluations/evaluators/:role` - 10 min TTL
3. `GET /api/interviews/metadata/enums` - 60 min TTL

**Cache keys:**
```
eval:interviewers:public:v1
eval:evaluators:{role}:v1
eval:metadata:enums:v1
```

#### 2.3 Dashboard Service Migration

**File:** `mock-dashboard-service.js`

**Affected endpoints:**
1. `GET /api/dashboard/stats` - 5 min TTL
2. `GET /api/dashboard/admin/stats` - 3 min TTL
3. `GET /api/analytics/dashboard-metrics` - 5 min TTL
4. `GET /api/analytics/status-distribution` - 10 min TTL
5. `GET /api/analytics/temporal-trends` - 15 min TTL

**Cache keys:**
```
dash:stats:general:v1
dash:stats:admin:v1
dash:analytics:metrics:v1
dash:analytics:status:v1
dash:analytics:temporal:v1
```

### Phase 3: Testing & Validation (Week 3)

#### 3.1 Unit Tests

**File:** `tests/redisCache.test.js`

```javascript
const redisCache = require('../utils/redisCache');

describe('Redis Cache', () => {
  beforeAll(async () => {
    await redisCache.initRedis();
  });

  afterAll(async () => {
    await redisCache.closeRedis();
  });

  test('should set and get value', async () => {
    await redisCache.set('test:key', { foo: 'bar' }, 60);
    const value = await redisCache.get('test:key');
    expect(value).toEqual({ foo: 'bar' });
  });

  test('should return null for expired key', async () => {
    await redisCache.set('test:expire', { foo: 'bar' }, 1);
    await new Promise(resolve => setTimeout(resolve, 1100));
    const value = await redisCache.get('test:expire');
    expect(value).toBeNull();
  });

  test('should delete keys by pattern', async () => {
    await redisCache.set('test:delete:1', { a: 1 }, 60);
    await redisCache.set('test:delete:2', { b: 2 }, 60);
    const deleted = await redisCache.del('test:delete:*');
    expect(deleted).toBe(2);
  });
});
```

#### 3.2 Integration Tests

**Validate:**
- Cache hit/miss behavior
- TTL expiration
- Pattern-based deletion
- Service restarts (cache persistence)
- Multiple concurrent requests (race conditions)

#### 3.3 Performance Benchmarks

**Metrics to measure:**
- Latency: In-memory vs Redis (expect +1-2ms for Redis)
- Throughput: Requests/second with Redis cache
- Memory usage: Redis memory vs in-memory cache

**Expected improvements:**
- ✅ Cache shared across instances (horizontal scaling enabled)
- ✅ Cache persists across restarts
- ⚠️ Slight latency increase (1-2ms acceptable)

---

## 4. Rollback Strategy

### 4.1 Feature Flag

**Add to each service:**
```javascript
const USE_REDIS = process.env.USE_REDIS === 'true';

async function getCachedValue(key) {
  if (USE_REDIS) {
    return await redisCache.get(key);
  } else {
    return simpleCache.get(key);
  }
}
```

### 4.2 Rollback Steps

If Redis migration causes issues:

1. **Stop Redis-enabled services:**
   ```bash
   pkill -f mock-user-service
   pkill -f mock-evaluation-service
   pkill -f mock-dashboard-service
   ```

2. **Set environment variable:**
   ```bash
   export USE_REDIS=false
   ```

3. **Restart services:**
   ```bash
   node mock-user-service.js &
   node mock-evaluation-service.js &
   node mock-dashboard-service.js &
   ```

4. **Monitor logs for in-memory cache usage**

---

## 5. Deployment Checklist

### 5.1 Development Environment

- [ ] Install Redis via Homebrew
- [ ] Start Redis server (`brew services start redis`)
- [ ] Install `redis` npm package
- [ ] Create `utils/redisCache.js`
- [ ] Migrate User Service
- [ ] Migrate Evaluation Service
- [ ] Migrate Dashboard Service
- [ ] Run unit tests
- [ ] Run integration tests
- [ ] Verify cache hit rates match in-memory

### 5.2 Production Environment

- [ ] Deploy Redis via Docker Compose
- [ ] Set `REDIS_PASSWORD` environment variable
- [ ] Update service environment variables:
  - `REDIS_HOST=redis` (Docker service name)
  - `REDIS_PORT=6379`
  - `REDIS_PASSWORD=<secret>`
  - `USE_REDIS=true`
- [ ] Deploy updated services
- [ ] Monitor Redis metrics (keys, memory, CPU)
- [ ] Set up Redis monitoring alerts

---

## 6. Monitoring & Maintenance

### 6.1 Redis Monitoring

**Key metrics to track:**
```bash
# Total keys
redis-cli DBSIZE

# Memory usage
redis-cli INFO memory

# Cache hit rate
redis-cli INFO stats | grep keyspace_hits
redis-cli INFO stats | grep keyspace_misses

# Evicted keys (if maxmemory reached)
redis-cli INFO stats | grep evicted_keys
```

### 6.2 Maintenance Tasks

**Daily:**
- Monitor cache hit rates
- Check Redis memory usage

**Weekly:**
- Review slow queries (`SLOWLOG GET 10`)
- Analyze key patterns (`redis-cli --scan --pattern 'dash:*'`)

**Monthly:**
- Backup Redis data (`BGSAVE`)
- Optimize maxmemory policy if needed

### 6.3 Alerting

**Setup alerts for:**
- Redis memory usage > 80%
- Cache hit rate < 50%
- Redis connection errors
- Evicted keys > 1000/hour

---

## 7. Cost-Benefit Analysis

### Benefits

✅ **Horizontal Scalability**
- Can run multiple instances of each service
- Cache shared across all instances

✅ **Persistence**
- Cache survives service restarts
- No cold start penalty

✅ **Advanced Features**
- Pub/Sub for cache invalidation
- Atomic operations (INCR, DECR)
- Sorted sets for leaderboards
- TTL per key

✅ **Centralized Management**
- Single Redis instance to monitor
- Easy to clear all caches
- Pattern-based invalidation

### Costs

⚠️ **Infrastructure**
- Additional Redis server (256MB-1GB RAM)
- Network latency (1-2ms per request)

⚠️ **Complexity**
- Additional service to maintain
- Redis connection handling
- Graceful degradation logic

⚠️ **Development Effort**
- 40-60 hours implementation
- Testing and validation
- Documentation updates

### ROI Calculation

**Current state:**
- 3 services with in-memory cache
- Cannot scale horizontally
- Cache lost on restart

**With Redis:**
- Horizontal scaling enabled → 3x capacity (3 instances per service)
- Persistence → 95% cache hit rate maintained
- Total cost: 1 week development + $5/month Redis hosting

**Break-even:** After first horizontal scaling event (< 6 months)

---

## 8. Future Enhancements

### 8.1 Redis Cluster (High Availability)

When traffic grows, migrate to Redis Cluster:
```
┌─────────┐   ┌─────────┐   ┌─────────┐
│Redis    │   │Redis    │   │Redis    │
│Master 1 │───│Master 2 │───│Master 3 │
└────┬────┘   └────┬────┘   └────┬────┘
     │             │             │
┌────▼────┐   ┌───▼─────┐   ┌───▼─────┐
│Redis    │   │Redis    │   │Redis    │
│Slave 1  │   │Slave 2  │   │Slave 3  │
└─────────┘   └─────────┘   └─────────┘
```

**Benefits:**
- High availability (automatic failover)
- Data sharding (10x scalability)
- Read replicas (lower latency)

### 8.2 Cache Invalidation Events

Implement pub/sub for cache invalidation:
```javascript
// When application status changes:
await redisCache.publish('invalidate', 'dash:stats:*');

// Subscribers automatically clear their cache
```

### 8.3 Multi-level Caching

Combine in-memory + Redis:
```javascript
// L1: In-memory (fastest, 50ms TTL)
const localCache = memoryCache.get(key);
if (localCache) return localCache;

// L2: Redis (shared, 5min TTL)
const redisValue = await redisCache.get(key);
if (redisValue) {
  memoryCache.set(key, redisValue, 50);  // Cache locally
  return redisValue;
}

// L3: Database (slowest)
const dbValue = await db.query(...);
await redisCache.set(key, dbValue, 300);
memoryCache.set(key, dbValue, 50);
return dbValue;
```

---

## 9. Conclusion

Redis migration is recommended for production deployment. The benefits of horizontal scalability and cache persistence outweigh the costs of additional infrastructure and development effort.

**Next Steps:**
1. Approve migration plan
2. Allocate 1 week development time
3. Deploy Redis in staging environment
4. Migrate services one by one
5. Validate performance and stability
6. Deploy to production

**Timeline:**
- Week 1: Infrastructure setup + User Service migration
- Week 2: Evaluation + Dashboard Service migration
- Week 3: Testing, validation, production deployment
