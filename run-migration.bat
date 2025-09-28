@echo off
echo 🚀 Running CapGold Database Migration...

REM ===========================================
REM UPDATE THESE VALUES WITH YOUR CREDENTIALS
REM ===========================================

REM Render Database (Source) - Get from Render dashboard
set SOURCE_DATABASE_URL=postgresql://your_render_user:your_render_password@your_render_host:5432/capgold_db
set SOURCE_DB_USER=your_render_user
set SOURCE_DB_PASSWORD=your_render_password

REM Supabase Database (Target) - Get from Supabase Settings > Database
set TARGET_DATABASE_URL=postgresql://postgres:your_supabase_password@db.your_project_ref.supabase.co:5432/postgres
set TARGET_DB_USER=postgres
set TARGET_DB_PASSWORD=your_supabase_password

REM ===========================================
REM DO NOT MODIFY BELOW THIS LINE
REM ===========================================

echo 📋 Migration Configuration:
echo Source: %SOURCE_DATABASE_URL%
echo Target: postgresql://postgres:****@db.your_project_ref.supabase.co:5432/postgres
echo.

echo 🔨 Building project...
cd server
call gradlew.bat build

if %errorlevel% equ 0 (
    echo ✅ Build successful!
    echo 📦 Starting migration...
    java -cp build/libs/server-all.jar org.cap.gold.migration.MigrationRunner
) else (
    echo ❌ Build failed!
    pause
    exit /b 1
)

pause