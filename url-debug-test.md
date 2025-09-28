# 🔍 URL Parsing Debug Test

## Your Supabase URL:
```
postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres
```

## Expected Transformation:
With the fixed URL parser, your URL should become:
```
jdbc:postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres?sslmode=require
```

## Test the URL Parsing (After Deployment):

```bash
curl -X POST https://your-app.onrender.com/api/admin/migration/test-url \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres",
    "targetPassword": "gRXyAeppbxwqfnt6E8mNql83xGIuqwKS"
  }'
```

**Expected Response:**
```json
{
  "original_url": "postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres",
  "normalized_url": "jdbc:postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres?sslmode=require",
  "message": "URL parsing test completed"
}
```

## Issues Fixed:

### 1. **Java URI Parser Problem**
- `java.net.URI` fails to parse PostgreSQL URLs with credentials properly
- Implemented manual parsing for URLs with username:password@host format

### 2. **Credential Handling**
- Fixed parsing of `postgres:password@host` format
- Maintains credentials in the JDBC URL

### 3. **SSL Requirement**
- Automatically adds `sslmode=require` for Supabase connections
- Essential for external database connections

## Manual URL Validation:

Let's break down your URL manually:

**Original:** `postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres`

**Parsed Components:**
- Protocol: `postgresql://`
- Username: `postgres`
- Password: `gRXyAeppbxwqfnt6E8mNql83xGIuqwKS`
- Host: `db.abqtdcwigcmjirqaivhl.supabase.co`
- Port: `5432`
- Database: `postgres`

**JDBC Result:** `jdbc:postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres?sslmode=require`

## Deploy and Test:

1. **Deploy the fix:**
   ```bash
   git add .
   git commit -m "Fix PostgreSQL URL parsing with credentials and add debug endpoint"
   git push
   ```

2. **Test URL parsing first:**
   Use the `/test-url` endpoint to verify the URL transforms correctly

3. **Run actual migration:**
   If URL parsing is correct, run the full migration

This should resolve the "Failed to initialize pool" error!