# 🔄 Export/Import Migration Guide

## 🎯 **Solution: Bypass Network Restrictions**

Since Render's free tier blocks external database connections, we'll use export/import method instead.

## 📦 **New Export Endpoints Available**

After deployment, you'll have these endpoints:

### **1. Get Export Summary**
```bash
curl https://your-app.onrender.com/api/admin/export/summary
```

**Shows record counts for all tables**

### **2. Export as JSON**
```bash
curl https://your-app.onrender.com/api/admin/export/json > capgold_data.json
```

**Complete data export in JSON format**

### **3. Export as SQL**
```bash
curl https://your-app.onrender.com/api/admin/export/sql > capgold_data.sql
```

**Ready-to-import SQL dump for Supabase**

## 🚀 **Step-by-Step Migration**

### **Step 1: Deploy Export Functionality**

```bash
git add server/src/main/kotlin/org/cap/gold/migration/
git add alternative-migration-strategies.md export-import-migration-guide.md
git commit -m "Add export/import migration solution - bypass network restrictions

- Create comprehensive export endpoints for all tables
- Support both JSON and SQL export formats
- Enable migration without direct database connections
- Workaround for Render free tier network limitations

🚀 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>"
git push
```

### **Step 2: Export Your Data**

After deployment:

```bash
# Check what data exists
curl https://your-app.onrender.com/api/admin/export/summary

# Export as SQL for direct import
curl https://your-app.onrender.com/api/admin/export/sql > capgold_export.sql
```

### **Step 3: Import to Supabase**

1. **Go to Supabase Dashboard**
2. **Open SQL Editor**
3. **Paste the exported SQL**
4. **Run the import**

**Or use Supabase CLI:**
```bash
psql "postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres" < capgold_export.sql
```

### **Step 4: Update App Configuration**

Update your `render.yaml`:
```yaml
envVars:
  - key: DATABASE_URL
    value: postgresql://postgres:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres
  - key: DB_USER
    value: postgres
  - key: DB_PASSWORD
    sync: false  # Set in Render dashboard
```

### **Step 5: Verify Migration**

Test your app with Supabase to ensure everything works.

## 📊 **Export Data Structure**

### **Tables Exported:**
- ✅ **AdminUsers** - Admin user management
- ✅ **Categories** - Product categories
- ✅ **AboutUs** - About us content
- ✅ **ProductsApproved** - Approved products
- ✅ **ProductsUnapproved** - Unapproved products
- ✅ **ProductImages** - Product images (Base64 encoded)
- ✅ **Orders** - Customer orders

### **JSON Export Structure:**
```json
{
  "adminUsers": [...],
  "categories": [...],
  "aboutUs": [...],
  "productsApproved": [...],
  "productsUnapproved": [...],
  "productImages": [...],
  "orders": [...],
  "exportTimestamp": "2024-..."
}
```

### **SQL Export Features:**
- ✅ **Proper escaping** of special characters
- ✅ **UUID preservation** for foreign keys
- ✅ **Timestamp formatting** compatible with PostgreSQL
- ✅ **NULL handling** for optional fields
- ✅ **Ready-to-run** INSERT statements

## 🔍 **Verification Steps**

### **Before Migration:**
```bash
# Check current data counts
curl https://your-app.onrender.com/api/admin/export/summary
```

### **After Migration:**
```sql
-- Run in Supabase SQL Editor
SELECT
  'admin_users' as table_name, COUNT(*) as count FROM "Admin_users"
UNION ALL
SELECT 'categories', COUNT(*) FROM "categories"
UNION ALL
SELECT 'products_approved', COUNT(*) FROM "products_approved"
UNION ALL
SELECT 'orders', COUNT(*) FROM "orders";
```

## ✅ **Benefits of This Approach**

- ✅ **No network dependencies** - works with any hosting provider
- ✅ **Complete data integrity** - all relationships preserved
- ✅ **Verifiable** - you can inspect the data before import
- ✅ **Reliable** - not affected by network issues
- ✅ **Fast** - no real-time connection needed

## 🎯 **Ready to Execute**

1. **Deploy the export functionality**
2. **Export your data**
3. **Import to Supabase**
4. **Update configuration**
5. **Switch to Supabase**

This completely bypasses the network connectivity issues!