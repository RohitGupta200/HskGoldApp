# JDBC URL Port Fix Examples

## The Problem
Your Render database URL was missing the port number:
```
postgresql://capgold_user:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@dpg-d2ljodndiees73c3q1bg-a/capgold
```

JDBC driver needs:
```
jdbc:postgresql://capgold_user:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@dpg-d2ljodndiees73c3q1bg-a:5432/capgold
```

## URL Transformation Examples

### Input → Output

**Render Database URL:**
```
Input:  postgresql://capgold_user:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@dpg-d2ljodndiees73c3q1bg-a/capgold
Output: jdbc:postgresql://capgold_user:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@dpg-d2ljodndiees73c3q1bg-a:5432/capgold
```

**Supabase Database URL:**
```
Input:  postgresql://postgres:password@db.abcdefghijklmnop.supabase.co/postgres
Output: jdbc:postgresql://postgres:password@db.abcdefghijklmnop.supabase.co:5432/postgres
```

**Already Correct URLs (unchanged):**
```
Input:  jdbc:postgresql://user:pass@host:5432/database
Output: jdbc:postgresql://user:pass@host:5432/database
```

## What the Fix Does

1. **Adds `jdbc:` prefix** if missing
2. **Adds port `:5432`** if missing
3. **Preserves existing ports** if already specified
4. **Handles all URL formats** (postgresql://, postgres://, jdbc:postgresql://)

## Migration Request Format

You can now use your original URLs without modification:

```bash
curl -X POST https://your-app.onrender.com/api/admin/migration/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co/postgres",
    "targetPassword": "YOUR_PASSWORD",
    "dryRun": false
  }'
```

The migration tool will automatically:
- Convert to: `jdbc:postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres`
- Handle your Render source database internally

## Error Handling

If URL parsing fails, the tool will:
- Print a warning message
- Use the original URL as fallback
- Continue with migration attempt