# 🌐 Network Connectivity Troubleshooting

## 🎯 **Good Progress!**
Error changed from `UnknownHostException` → `Network is unreachable`

This means:
- ✅ **URL parsing fixed** (hostname extracted correctly)
- ✅ **JDBC URL format correct**
- ❌ **Network connectivity issue** from Render to Supabase

## 🔧 **Solution 1: Try Supabase Session Pooler**

### **Current (Direct Connection - Port 5432):**
```
postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres
```

### **Try Session Pooler (Port 6543):**
```
postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:6543/postgres
```

**How to get Session Pooler URL:**
1. Go to **Supabase Dashboard → Settings → Database**
2. Find **"Session pooler"** section
3. Copy the connection string with **port 6543**

## 🧪 **Solution 2: Test Network Connectivity**

After deploying the network test endpoint:

```bash
curl -X POST https://your-app.onrender.com/api/admin/migration/test-network \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:5432/postgres",
    "targetPassword": "gRXyAeppbxwqfnt6E8mNql83xGIuqwKS"
  }'
```

This will test:
- Host resolution: `db.abqtdcwigcmjirqaivhl.supabase.co`
- Port connectivity: `5432` (or `6543` for session pooler)
- Network path from Render to Supabase

## 🔄 **Solution 3: Alternative Connection Methods**

### **A. Transaction Pooler (Port 6544):**
```
postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:6544/postgres
```

### **B. IPv4 Direct (if hostname fails):**
```bash
# Test if it's a DNS issue
nslookup db.abqtdcwigcmjirqaivhl.supabase.co
```

## 🎯 **Most Likely Solutions**

### **1. Use Session Pooler (Port 6543) - RECOMMENDED**
This often works better from external services like Render.

### **2. Check Supabase Project Settings**
- Ensure project is not paused
- Check if there are IP restrictions
- Verify database is accessible externally

### **3. Render Network Limitations**
- Render free tier might have some network restrictions
- Try during different times (network congestion)

## 📋 **Step-by-Step Troubleshooting**

1. **Deploy network test endpoint**
2. **Test current URL** (port 5432)
3. **If fails, try Session Pooler** (port 6543)
4. **If still fails, try Transaction Pooler** (port 6544)
5. **Check Supabase project status**

## 🚨 **If All Ports Fail**

This could indicate:
- **Render → Supabase network path blocked**
- **Supabase project configuration issue**
- **Temporary network outage**

## 🎯 **Quick Test**

**Try Session Pooler URL first:**
```bash
curl -X POST https://your-app.onrender.com/api/admin/migration/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "postgresql://postgres:gRXyAeppbxwqfnt6E8mNql83xGIuqwKS@db.abqtdcwigcmjirqaivhl.supabase.co:6543/postgres",
    "targetPassword": "gRXyAeppbxwqfnt6E8mNql83xGIuqwKS",
    "dryRun": false
  }'
```

**Port 6543 (Session Pooler) often works when port 5432 (Direct) doesn't!**