# Archived NGINX Configurations

This directory contains historical NGINX configuration files that are **NO LONGER ACTIVE**.

## Active Configuration

**Current:** `local-gateway.conf` (in project root)  
**Last Updated:** 2025-10-01  
**Purpose:** API Gateway for Node.js mock services

## Archived Configurations

| File | Date | Purpose | Why Archived |
|------|------|---------|--------------|
| `nginx-gateway.conf` | Sep 4 | Early gateway config | Replaced by local-gateway.conf |
| `cors-clean-gateway.conf` | Sep 22 | CORS fixes attempt | Merged into local-gateway.conf |
| `local-gateway-fixed.conf` | Sep 22 | Bug fix version | Superseded by local-gateway.conf |
| `gateway-microservices.conf` | Sep 2 | Spring Boot routing | Not in use (Node.js active) |
| `gateway-hybrid.conf` | Sep 2 | Hybrid routing | Superseded |
| `gateway-simple.conf` | Sep 2 | Minimal config | Too simple for production |
| `gateway-nginx.conf` | Sep 2 | Initial template | Prototype only |

## History

**2024-09-02:** Initial NGINX configs created  
**2024-09-22:** CORS fixes implemented  
**2025-10-01:** Consolidated to single active config, archived others

## Usage

These files are kept for:
- Historical reference
- Rollback scenarios
- Learning from past iterations

**DO NOT USE** these configurations directly. Always use `local-gateway.conf` in project root.

---

**Note:** If you need to restore any config, copy from this directory to root and test thoroughly.
