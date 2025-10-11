# Claude Code Specialized Agents - MTN Admission System

This document catalogs all specialized agents available for the MTN Admission System project. These agents are designed to handle specific domains and tasks with expert-level knowledge and systematic workflows.

---

## 📚 Table of Contents

1. [Infrastructure & DevOps](#infrastructure--devops)
2. [Backend Services](#backend-services)
3. [Frontend Development](#frontend-development)
4. [Quality Assurance](#quality-assurance)
5. [Security](#security)
6. [Architecture & Planning](#architecture--planning)
7. [How to Use Agents](#how-to-use-agents)

---

## Infrastructure & DevOps

### 🔧 cicd-architect
**File:** `.claude/agents/cicd-architect.md`
**Color:** Green
**Model:** Sonnet

**Purpose:** Design, implement, debug, and optimize CI/CD pipelines, infrastructure automation, containerization, and deployment workflows.

**Use Cases:**
- Creating GitHub Actions workflows with automated testing
- Debugging pipeline failures (timeouts, dependencies, etc.)
- Dockerizing microservices with multi-stage builds
- Setting up Railway/Vercel deployments
- Optimizing pipeline execution time and costs
- Implementing security scanning (Trivy, SAST, dependency checks)
- Blue-green deployment strategies

**Example:**
```
user: "I want to set up automated testing that runs on every pull request"
assistant: "I'll use the cicd-architect agent to create a comprehensive GitHub Actions workflow."
```

**Key Strengths:**
- 95%+ confidence in diagnosing CI/CD failures
- Follows industry best practices for Docker and K8s
- Cost-aware optimizations (reduces CI minutes)
- Includes health checks and rollback procedures

---

### 🔒 lockfile-ci-fixer
**File:** `.claude/agents/lockfile-ci-fixer.md`
**Color:** Yellow
**Model:** Sonnet

**Purpose:** Fix CI/CD pipeline failures caused by lockfile desynchronization or package manager conflicts (npm/pnpm).

**Use Cases:**
- Resolving "Missing from lock file" errors in CI
- Standardizing repository to single package manager
- Fixing Railway deployment failures due to lockfile issues
- Migrating from npm to pnpm (or vice versa) in monorepos
- Cleaning up confusing CI workflow conditionals
- Ensuring idempotent builds

**Example:**
```
user: "Railway deployment broke after merging. It says packages are missing from the lockfile."
assistant: "I'm using the lockfile-ci-fixer agent to diagnose and fix the lockfile synchronization."
```

**Key Features:**
- Intelligent package manager detection (auto, npm, or pnpm)
- Creates clean, idempotent GitHub Actions workflows
- Railway/Nixpacks configuration
- Generates docs/pm-decision.md explaining rationale
- Validates lockfile sync before creating PR

---

### 🌐 gateway-architect
**File:** `.claude/agents/gateway-architect.md`
**Color:** Blue
**Model:** Sonnet

**Purpose:** NGINX configuration, API routing, CORS setup, load balancing, and gateway health checks.

**Use Cases:**
- Configuring NGINX for microservices routing
- Adding new service routes to gateway
- Fixing CORS issues (missing headers, preflight failures)
- Setting up SSL/TLS termination
- Implementing rate limiting
- Health check endpoints configuration

**Example:**
```
user: "Frontend is getting CORS errors when calling /api/applications"
assistant: "I'll use the gateway-architect agent to diagnose and fix the CORS configuration."
```

**Key Strengths:**
- Understands NGINX, Apache, and Traefik configurations
- Converts macOS NGINX configs to Linux for CI
- Implements proper CORS for SPA + API architectures

---

## Backend Services

### 👤 user-auth-service-builder
**File:** `.claude/agents/user-auth-service-builder.md`
**Color:** Purple
**Model:** Sonnet

**Purpose:** JWT authentication, user CRUD operations, RBAC implementation, and auth middleware.

**Use Cases:**
- Implementing JWT-based authentication
- Adding role-based access control (RBAC)
- Creating login/register endpoints
- Protecting routes with auth middleware
- Password hashing with BCrypt
- Token refresh strategies

**Example:**
```
user: "Add role-based access control to protect admin endpoints"
assistant: "I'll use the user-auth-service-builder agent to implement RBAC middleware."
```

**Supports:**
- JWT (HS256, HS512, RS256)
- OAuth 2.0 / OpenID Connect integration
- Multi-factor authentication (MFA)
- Password reset flows

---

### 📝 application-documents-service
**File:** `.claude/agents/application-documents-service.md`
**Color:** Orange
**Model:** Sonnet

**Purpose:** Application CRUD, document upload/validation, application workflows, and status management.

**Use Cases:**
- Creating application submission endpoints
- Implementing document upload with PDF/image validation
- Building application workflow state machines
- Adding file size/type validation
- Generating application receipts (PDF)
- Implementing multi-step form persistence

**Example:**
```
user: "Create document validation with PDF and image support"
assistant: "I'll use the application-documents-service agent to implement file validation."
```

**Features:**
- Multer/Busboy file uploads
- AWS S3 / Google Cloud Storage integration
- PDF generation with PDFKit/Puppeteer
- Document virus scanning hooks

---

### 📅 evaluation-interview-service
**File:** `.claude/agents/evaluation-interview-service.md`
**Color:** Teal
**Model:** Sonnet

**Purpose:** Interview scheduling, conflict detection, evaluation assignment, and calendar management.

**Use Cases:**
- Implementing interview scheduling with time slots
- Detecting scheduling conflicts (double bookings)
- Assigning evaluators to applications
- Building availability calendar
- Creating interview reminder notifications
- Generating evaluation reports

**Example:**
```
user: "Implement interview scheduling with time conflict validation"
assistant: "I'll use the evaluation-interview-service agent to create conflict-free scheduling."
```

**Integrations:**
- Google Calendar API
- Microsoft Graph (Outlook Calendar)
- SendGrid/Mailgun for email reminders

---

### 📧 notification-service-builder
**File:** `.claude/agents/notification-service-builder.md`
**Color:** Pink
**Model:** Sonnet

**Purpose:** Event-driven notifications, email templates, SMS integration, and notification history.

**Use Cases:**
- Creating templated email notification system
- Implementing SMS via Twilio/SNS
- Building notification queue with retry logic
- Creating dynamic email templates with variables
- Adding push notifications
- Implementing notification preferences

**Example:**
```
user: "Create notification service that sends templated emails on application status change"
assistant: "I'll use the notification-service-builder agent to implement event-driven notifications."
```

**Supports:**
- SMTP (Gmail, SendGrid, Amazon SES)
- SMS (Twilio, AWS SNS, MessageBird)
- Push notifications (FCM, APNS)
- Handlebars/EJS template engines

---

## Frontend Development

### ⚛️ frontend-spa-specialist
**File:** `.claude/agents/frontend-spa-specialist.md`
**Color:** Cyan
**Model:** Sonnet

**Purpose:** React components, API integration, form validation, error handling, and state management.

**Use Cases:**
- Building React 19 components with TypeScript
- Implementing form validation with React Hook Form
- Integrating with backend APIs via Axios
- Adding error boundaries and fallback UI
- Implementing loading states and skeletons
- Creating responsive layouts with Tailwind CSS

**Example:**
```
user: "Implement user dashboard with proper error handling and loading states"
assistant: "I'll use the frontend-spa-specialist agent to create a production-ready dashboard."
```

**Expertise:**
- React 19 (hooks, context, suspense)
- TypeScript with strict mode
- Vite build optimization
- Accessibility (WCAG 2.1)
- Performance (React.memo, useMemo, useCallback)

---

### 🔄 fe-be-contract-auditor
**File:** `.claude/agents/fe-be-contract-auditor.md`
**Color:** Amber
**Model:** Sonnet

**Purpose:** Form/DTO alignment verification, API contract validation, and data loss detection.

**Use Cases:**
- Auditing registration form against backend DTO
- Detecting missing fields between frontend and backend
- Validating API response shapes
- Identifying data transformation bugs
- Creating TypeScript interfaces from backend schemas
- Preventing data loss in form submissions

**Example:**
```
user: "Verify if interview scheduling user story is complete on both frontend and backend"
assistant: "I'll use the fe-be-contract-auditor agent to check contract alignment."
```

**Checks:**
- Field name mismatches (camelCase vs snake_case)
- Missing required fields
- Type incompatibilities (string vs number)
- Validation rule differences
- Null handling discrepancies

---

## Quality Assurance

### 🧪 platform-qa-orchestrator
**File:** `.claude/agents/platform-qa-orchestrator.md`
**Color:** Indigo
**Model:** Sonnet

**Purpose:** Docker Compose environments, E2E tests, smoke tests, integration testing, and monitoring setup.

**Use Cases:**
- Setting up Docker Compose with all microservices
- Creating Playwright E2E test suites
- Implementing smoke tests for health checks
- Building integration test harnesses
- Adding monitoring (Prometheus, Grafana)
- Load testing with k6/Artillery

**Example:**
```
user: "Set up Docker environment with all 6 microservices for local testing"
assistant: "I'll use the platform-qa-orchestrator agent to create a complete Docker Compose setup."
```

**Deliverables:**
- docker-compose.yml with all services
- Health check scripts
- E2E test scenarios (Playwright/Cypress)
- Performance baseline reports

---

### 🐛 bug-tracer-fixer
**File:** `.claude/agents/bug-tracer-fixer.md`
**Color:** Red
**Model:** Sonnet

**Purpose:** Debugging API errors (404, 401, 500), CORS issues, empty responses, and systematic troubleshooting.

**Use Cases:**
- Diagnosing 404 errors on API endpoints
- Fixing 401 Unauthorized authentication issues
- Debugging 500 Internal Server Errors with stack traces
- Resolving CORS preflight failures
- Investigating empty API responses
- Tracing request flow through microservices

**Example:**
```
user: "Getting 404 error on /api/interviews endpoint"
assistant: "I'll use the bug-tracer-fixer agent to systematically diagnose the routing issue."
```

**Methodology:**
1. Reproduce the bug with minimal steps
2. Check logs for error stack traces
3. Verify routing configuration
4. Test with curl/Postman
5. Implement fix with validation
6. Add regression test

---

## Security

### 🔐 csrf-guardian
**File:** `.claude/agents/csrf-guardian.md`
**Color:** Crimson
**Model:** Sonnet

**Purpose:** CSRF protection implementation, secure cookie configuration, and XSS/injection prevention.

**Use Cases:**
- Implementing double-submit cookie pattern for CSRF
- Configuring secure, httpOnly, sameSite cookies
- Adding CSRF tokens to forms
- Validating CSRF tokens in backend
- Protecting state-changing endpoints
- Adding Content Security Policy (CSP) headers

**Example:**
```
user: "Add CSRF protection to all POST/PUT/DELETE endpoints"
assistant: "I'll use the csrf-guardian agent to implement CSRF tokens with secure cookies."
```

**Security Measures:**
- CSRF tokens with cryptographic strength
- SameSite cookies (Strict/Lax)
- Token rotation on sensitive operations
- Rate limiting on token generation

---

## Architecture & Planning

### 📐 backlog-story-architect
**File:** `.claude/agents/backlog-story-architect.md`
**Color:** Violet
**Model:** Sonnet

**Purpose:** User story creation, epic decomposition, acceptance criteria definition, and backlog prioritization.

**Use Cases:**
- Converting feature requests into user stories
- Breaking down large epics into manageable tasks
- Writing clear acceptance criteria
- Prioritizing backlog items (MoSCoW method)
- Creating technical design documents
- Estimating story points

**Example:**
```
user: "Create user stories for the payment integration feature"
assistant: "I'll use the backlog-story-architect agent to decompose the feature into stories."
```

**Deliverables:**
- User stories in Gherkin format (Given/When/Then)
- Acceptance criteria checklist
- Technical design doc
- Dependencies and risks identified
- T-shirt size estimates (XS/S/M/L/XL)

---

## How to Use Agents

### Basic Usage

```typescript
// In Claude Code CLI
user: "I need to fix CORS errors between frontend and backend"
assistant: "I'm going to use the Task tool to launch the gateway-architect agent..."
```

### Parallel Agent Execution

For complex features requiring multiple domains:

```typescript
user: "Implement complete payment feature"
assistant: "I'll launch multiple agents in parallel:
1. backlog-story-architect - Analyze payment user story gaps
2. fe-be-contract-auditor - Verify form fields match backend DTO
3. application-documents-service - Integrate payment with applications
4. notification-service-builder - Send payment confirmation emails
5. gateway-architect - Add payment routes to NGINX
6. cicd-architect - Add payment tests to CI pipeline"
```

### Proactive Agent Selection

Agents are automatically selected based on task context:
- **CORS issues** → gateway-architect
- **Auth problems** → user-auth-service-builder
- **CI/CD failures** → cicd-architect or lockfile-ci-fixer
- **Form validation** → frontend-spa-specialist
- **Contract mismatches** → fe-be-contract-auditor
- **Bugs** → bug-tracer-fixer

---

## Agent Characteristics

| Agent | Primary Focus | Typical Task Duration | Confidence Level |
|-------|---------------|----------------------|------------------|
| cicd-architect | CI/CD | 30-60 min | 95% |
| lockfile-ci-fixer | Package Management | 15-30 min | 98% |
| gateway-architect | NGINX/Routing | 15-30 min | 90% |
| user-auth-service-builder | Authentication | 45-90 min | 92% |
| application-documents-service | Application Logic | 60-120 min | 88% |
| evaluation-interview-service | Scheduling | 45-90 min | 85% |
| notification-service-builder | Notifications | 30-60 min | 90% |
| frontend-spa-specialist | React/UI | 60-120 min | 87% |
| fe-be-contract-auditor | API Contracts | 20-40 min | 93% |
| platform-qa-orchestrator | Testing/Docker | 90-180 min | 85% |
| bug-tracer-fixer | Debugging | Variable | 91% |
| csrf-guardian | Security | 30-45 min | 94% |
| backlog-story-architect | Planning | 45-90 min | 88% |

---

## Best Practices

### 1. **Single Responsibility**
Use the most specific agent for the task. Don't use cicd-architect for frontend tasks.

### 2. **Agent Chaining**
For complex features, use agents sequentially:
1. backlog-story-architect (plan)
2. fe-be-contract-auditor (verify contracts)
3. Specific implementation agents
4. bug-tracer-fixer (if issues arise)
5. cicd-architect (add tests to pipeline)

### 3. **Context Provision**
Always provide:
- Current branch name
- Relevant file paths
- Error messages or logs
- Desired outcome

### 4. **Validation**
After agent work:
- Review generated code
- Test locally
- Check documentation
- Verify confidence level

---

## Agent Updates

This document is current as of **October 11, 2025**. Agents are continuously improved based on:
- New project requirements
- Claude Code feature updates
- User feedback
- Best practice evolution

To see all available agents:
```bash
cd .claude/agents
ls -la *.md
```

---

## Contributing

When creating new agents:
1. Use existing agents as templates
2. Include clear use case examples
3. Specify model and color
4. Document confidence levels
5. Update this AGENTS.md file
6. Test with real scenarios

---

**Maintained by:** MTN Development Team
**Last Updated:** October 11, 2025
**Agent Count:** 13 specialized agents
