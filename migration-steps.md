# 🚀 CapGold Database Migration: Render to Supabase

## Free Tier Solution: API-Based Migration

Since Render free tier doesn't allow external database connections, we've created an API endpoint that runs the migration **from within your Render server**.

## Step 1: Deploy Migration Tool

1. **Add migration files to git:**
   ```bash
   git add server/src/main/kotlin/org/cap/gold/migration/
   git add migration-steps.md
   git commit -m "Add Supabase migration API endpoints"
   git push
   ```

2. **Deploy to Render** (your app will redeploy automatically)

## Step 2: Get Supabase Connection Details

1. Go to your **Supabase Dashboard**
2. Select your project → **Settings** → **Database**
3. Copy these details:
   ```
   Host: db.your_project_ref.supabase.co
   Database: postgres
   Port: 5432
   User: postgres
   Password: [your_password]
   ```

4. **Construct your connection URL:**
   ```
   postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres
   ```

## Step 3: Run Migration via API

### Option A: Using curl (Command Line)

```bash
# Test the migration endpoint first
curl -X GET https://your-app.onrender.com/api/admin/migration/status

# Run a dry-run to test connectivity
curl -X POST https://your-app.onrender.com/api/admin/migration/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres",
    "targetPassword": "YOUR_PASSWORD",
    "dryRun": true
  }'

# Run the actual migration
curl -X POST https://your-app.onrender.com/api/admin/migration/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres",
    "targetPassword": "YOUR_PASSWORD",
    "dryRun": false
  }'
```

### Option B: Using Browser/Postman

1. **GET** `https://your-app.onrender.com/api/admin/migration/status`
   - Check if migration tool is ready

2. **POST** `https://your-app.onrender.com/api/admin/migration/start`
   - **Body (JSON):**
     ```json
     {
       "targetUrl": "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres",
       "targetPassword": "YOUR_PASSWORD",
       "dryRun": false
     }
     ```

### Option C: Create Simple Web Interface

```html
<!DOCTYPE html>
<html>
<head>
    <title>CapGold Migration Tool</title>
</head>
<body>
    <h2>Database Migration: Render → Supabase</h2>

    <form id="migrationForm">
        <label>Supabase Project Reference:</label><br>
        <input type="text" id="projectRef" placeholder="abcdefghijklmnop" required><br><br>

        <label>Supabase Password:</label><br>
        <input type="password" id="password" required><br><br>

        <input type="checkbox" id="dryRun"> Dry Run (test only)<br><br>

        <button type="submit">Start Migration</button>
    </form>

    <div id="result"></div>

    <script>
        document.getElementById('migrationForm').addEventListener('submit', async (e) => {
            e.preventDefault();

            const projectRef = document.getElementById('projectRef').value;
            const password = document.getElementById('password').value;
            const dryRun = document.getElementById('dryRun').checked;

            const targetUrl = `postgresql://postgres:${password}@db.${projectRef}.supabase.co:5432/postgres`;

            try {
                const response = await fetch('/api/admin/migration/start', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        targetUrl: targetUrl,
                        targetPassword: password,
                        dryRun: dryRun
                    })
                });

                const result = await response.json();
                document.getElementById('result').innerHTML =
                    `<pre>${JSON.stringify(result, null, 2)}</pre>`;

            } catch (error) {
                document.getElementById('result').innerHTML =
                    `<p style="color:red">Error: ${error.message}</p>`;
            }
        });
    </script>
</body>
</html>
```

## Step 4: Update Environment Variables

After successful migration, update your Render environment variables:

1. Go to **Render Dashboard** → Your Service → **Environment**
2. Update these variables:
   ```
   DATABASE_URL = postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres
   DB_USER = postgres
   DB_PASSWORD = YOUR_SUPABASE_PASSWORD
   ```
3. **Deploy** (your app will restart with Supabase)

## Step 5: Verify Migration

```bash
# Verify the migration was successful
curl -X POST https://your-app.onrender.com/api/admin/migration/verify \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres",
    "targetPassword": "YOUR_PASSWORD"
  }'
```

## Expected Migration Output

```json
{
  "success": true,
  "message": "Migration completed successfully! Migrated 422 records.",
  "result": {
    "success": true,
    "adminUsers": 5,
    "categories": 12,
    "aboutUs": 1,
    "productsApproved": 150,
    "productsUnapproved": 25,
    "productImages": 140,
    "orders": 89
  },
  "verification": {
    "success": true,
    "sourceCounts": { ... },
    "targetCounts": { ... }
  }
}
```

## Troubleshooting

### Common Issues:

1. **"Connection refused"** → Check Supabase connection string
2. **"Authentication failed"** → Verify Supabase password
3. **"Table already exists"** → Normal, migration handles this
4. **"Timeout"** → Large datasets may take time, be patient

### Check Migration Status:
```bash
# Check if your app deployed successfully
curl https://your-app.onrender.com/health

# Check migration endpoint
curl https://your-app.onrender.com/api/admin/migration/status
```

## Security Notes

- Migration endpoints are under `/admin/` path
- Consider adding authentication if needed
- Don't commit passwords to git
- Use environment variables for sensitive data

## Cleanup

After successful migration:
1. Test your app thoroughly with Supabase
2. Keep Render database for a few days as backup
3. Remove migration endpoints if no longer needed
4. Update your documentation with new database details

---

This approach works around Render's free tier limitations by running the migration from inside your deployed application! 🎉