# JDBC URL Format Fix

## Problem
The migration was failing with:
```
Driver org.postgresql.Driver claims to not accept jdbcUrl, postgresql://capgold_user:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@dpg-d2ljodndiees73c3q1bg-a/capgold
```

## Root Cause
- JDBC driver expects: `jdbc:postgresql://...`
- But received: `postgresql://...`

## Solution Applied
Added `normalizeJdbcUrl()` function that converts:
- `postgresql://` → `jdbc:postgresql://`
- `postgres://` → `jdbc:postgresql://`
- `jdbc:postgresql://` → unchanged

## URL Format Examples

### ✅ Correct Formats for Migration API:

**Render Database (Source):**
```json
{
  "sourceUrl": "postgresql://capgold_user:password@dpg-xxx-a.oregon-postgres.render.com:5432/capgold"
}
```

**Supabase Database (Target):**
```json
{
  "targetUrl": "postgresql://postgres:password@db.abcdefghijklmnop.supabase.co:5432/postgres"
}
```

### 🔧 What the Migration Tool Does:
- Automatically converts to: `jdbc:postgresql://...`
- Handles both `postgresql://` and `postgres://` formats
- Works with all PostgreSQL connection strings

## Updated Migration Request

Now you can use either format in your API calls:

```bash
curl -X POST https://your-app.onrender.com/api/admin/migration/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres",
    "targetPassword": "YOUR_PASSWORD",
    "dryRun": false
  }'
```

The migration tool will automatically fix the URL format internally!