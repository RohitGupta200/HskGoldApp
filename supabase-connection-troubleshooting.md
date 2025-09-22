# 🔧 Supabase Connection Troubleshooting

## Error: "Failed to initialize pool: The connection attempt failed"

### 🎯 **Quick Fixes (Try These First)**

## 1. ✅ **Verify Your Connection Details**

**Get the correct URL from Supabase:**
1. Go to **Supabase Dashboard → Settings → Database**
2. Find **"Direct Connection"** section
3. Copy the **exact** connection string

**URL Format must be exactly:**
```
postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres
```

## 2. 🔐 **Check Database Password**

**Common Issues:**
- Using account password instead of database password
- Copy/paste added extra characters
- Password contains special characters that need escaping

**Fix:**
1. Go to **Supabase → Settings → Database**
2. Click **"Reset database password"**
3. Copy the new password carefully
4. Use the new password in your migration

## 3. 🏗️ **Verify Project Status**

**Check if your Supabase project is active:**
1. Go to **Supabase Dashboard**
2. Make sure project shows **"Active"** status
3. If paused, click **"Restore"** or **"Unpause"**

## 4. 🌐 **Test Connection from Render**

**Add a test endpoint to verify connectivity:**

Add this to your migration controller:

```kotlin
get("/test-supabase") {
    val testUrl = "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres"
    try {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres?sslmode=require"
            username = "postgres"
            password = "YOUR_PASSWORD"
            maximumPoolSize = 1
            connectionTimeout = 30000
        }

        val dataSource = HikariDataSource(config)
        dataSource.connection.use {
            call.respond("✅ Supabase connection successful!")
        }
    } catch (e: Exception) {
        call.respond("❌ Connection failed: ${e.message}")
    }
}
```

## 5. 🔒 **SSL Requirements**

Supabase requires SSL. The updated migration now automatically adds `sslmode=require`.

**Manual URL format:**
```
postgresql://postgres:PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres?sslmode=require
```

## 6. 📍 **Common URL Mistakes**

### ❌ **Wrong Formats:**
```bash
# Missing postgres username
postgresql://PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres

# Wrong database name
postgresql://postgres:PASSWORD@db.PROJECT_REF.supabase.co:5432/DATABASE_NAME

# Using transaction pooler URL instead of direct
postgresql://postgres:PASSWORD@db.PROJECT_REF.supabase.co:6543/postgres
```

### ✅ **Correct Format:**
```bash
postgresql://postgres:PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres
```

## 7. 🔍 **Debug with Enhanced Logging**

The updated migration now includes better logging. After deploying, check the logs:

**Expected Output:**
```
Connecting to target database: postgresql://postgres:****@db.abcd1234.supabase.co:5432/postgres
✅ Successfully connected to target database
```

**If Failed:**
```
❌ Failed to connect to target database: [specific error message]
```

## 8. 🌍 **Network Issues**

**If Render can't reach Supabase:**
- This is rare but possible
- Try different Supabase regions
- Contact Render support about external connections

## 9. 📋 **Step-by-Step Verification**

1. **Deploy the updated migration** (with better logging)
2. **Reset your Supabase password**
3. **Use the exact Direct Connection URL**
4. **Try a dry run first:**
   ```bash
   curl -X POST https://your-app.onrender.com/api/admin/migration/start \
     -H "Content-Type: application/json" \
     -d '{
       "targetUrl": "postgresql://postgres:NEW_PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres",
       "targetPassword": "NEW_PASSWORD",
       "dryRun": true
     }'
   ```

## 🚨 **If Still Failing**

**Check these in order:**

1. **Password Issues** - Most common cause
2. **URL Format** - Missing parts or wrong format
3. **Project Status** - Paused or inactive project
4. **SSL Requirements** - Should be handled automatically now
5. **Network Issues** - Rare but possible

**Deploy the updated migration with better logging to get more specific error details!**