# Database Migration Guide: Render to Supabase

This guide will help you migrate your CapGold database from Render to Supabase while keeping your backend server on Render.

## Prerequisites

1. **Supabase Account**: Create a project at [supabase.com](https://supabase.com)
2. **Database Credentials**: Gather connection details for both databases
3. **Migration Time**: Schedule during low-traffic period

## Step 1: Gather Database Connection Details

### Render Database (Source)
From your Render dashboard, get:
```
Database URL: postgresql://user:password@host:port/database
User: your_render_user
Password: your_render_password
```

### Supabase Database (Target)
From your Supabase project settings > Database:
```
Host: db.your_project_ref.supabase.co
Database: postgres
Port: 5432
User: postgres
Password: your_supabase_password
```

## Step 2: Run the Migration

### Option A: Command Line Migration

```bash
# Navigate to your project
cd server

# Compile the project
./gradlew build

# Run migration with parameters
java -cp build/libs/server-all.jar org.cap.gold.migration.MigrationRunner \
  "postgresql://render_user:render_pass@render_host:5432/capgold_db" \
  "render_user" \
  "render_password" \
  "postgresql://postgres:supabase_pass@db.project_ref.supabase.co:5432/postgres" \
  "postgres" \
  "supabase_password"
```

### Option B: Environment Variables

```bash
# Set environment variables
export SOURCE_DATABASE_URL="postgresql://render_user:render_pass@render_host:5432/capgold_db"
export SOURCE_DB_USER="render_user"
export SOURCE_DB_PASSWORD="render_password"
export TARGET_DATABASE_URL="postgresql://postgres:supabase_pass@db.project_ref.supabase.co:5432/postgres"
export TARGET_DB_USER="postgres"
export TARGET_DB_PASSWORD="supabase_password"

# Run migration
java -cp build/libs/server-all.jar org.cap.gold.migration.MigrationRunner
```

### Option C: Interactive Migration

For a guided migration process:

```kotlin
// Create a simple Kotlin script
import org.cap.gold.migration.InteractiveMigrationRunner

fun main() {
    val runner = InteractiveMigrationRunner()
    runner.runInteractiveMigration()
}
```

## Step 3: Update Application Configuration

After successful migration, update your application to use Supabase:

### Update render.yaml

```yaml
services:
  - type: web
    name: capgold-server
    runtime: docker
    plan: free
    autoDeploy: true
    healthCheckPath: /health
    envVars:
      - key: DATABASE_URL
        value: postgresql://postgres:your_supabase_password@db.your_project_ref.supabase.co:5432/postgres
      - key: DB_USER
        value: postgres
      - key: DB_PASSWORD
        sync: false  # Set this in Render dashboard
      # ... other existing environment variables
```

### Update Environment Variables in Render Dashboard

1. Go to Render Dashboard > Your Service > Environment
2. Update these variables:
   - `DATABASE_URL`: Your Supabase connection string
   - `DB_USER`: postgres
   - `DB_PASSWORD`: Your Supabase password
   - Remove any Render database references

## Step 4: Deploy and Test

1. **Deploy**: Push changes to trigger Render deployment
2. **Test**: Verify all functionality works with Supabase
3. **Monitor**: Check logs for any connection issues

## Migration Script Features

The migration script includes:

- ✅ **Schema Creation**: Automatically creates all tables in target database
- ✅ **Data Transfer**: Migrates all data with proper relationships
- ✅ **Verification**: Compares record counts to ensure completeness
- ✅ **Error Handling**: Comprehensive error reporting
- ✅ **Progress Tracking**: Real-time migration progress
- ✅ **Rollback Safety**: Uses `insertIgnore` to prevent duplicates

## Tables Migrated

1. **admin_users** - Admin user management
2. **categories** - Product categories
3. **products_approved** - Approved products
4. **products_unapproved** - Unapproved products
5. **product_images** - Product images (BLOB data)
6. **orders** - Customer orders
7. **about_us** - About us content

## Troubleshooting

### Connection Issues
```bash
# Test Supabase connection
psql "postgresql://postgres:password@db.project_ref.supabase.co:5432/postgres"
```

### Migration Fails
- Check database credentials
- Ensure Supabase allows external connections
- Verify table schemas match

### Performance Issues
- Run migration during low traffic
- Consider migrating large tables separately
- Monitor connection pool limits

## Post-Migration Checklist

- [ ] All data migrated successfully
- [ ] Application connects to Supabase
- [ ] All API endpoints working
- [ ] Database queries performing well
- [ ] Backup old Render database (optional)
- [ ] Update documentation with new connection details

## Support

If you encounter issues:

1. Check migration logs for specific errors
2. Verify connection strings format
3. Ensure Supabase project is properly configured
4. Contact support if authentication issues persist

## Security Notes

- Never commit database passwords to version control
- Use environment variables for all credentials
- Enable SSL/TLS for database connections
- Regularly rotate database passwords