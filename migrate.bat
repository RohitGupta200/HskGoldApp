@echo off
echo 🔨 Building CapGold Migration Tool...

REM Navigate to server directory
cd server

REM Clean and build the project
echo 📦 Building project...
call gradlew.bat clean build

if %errorlevel% equ 0 (
    echo ✅ Build successful!
    echo.
    echo 🚀 You can now run the migration using:
    echo.
    echo Method 1 - Command line with parameters:
    echo java -cp build/libs/server-all.jar org.cap.gold.migration.MigrationRunner ^
    echo   "SOURCE_URL" "SOURCE_USER" "SOURCE_PASS" ^
    echo   "TARGET_URL" "TARGET_USER" "TARGET_PASS"
    echo.
    echo Method 2 - Environment variables:
    echo set SOURCE_DATABASE_URL=your_render_url
    echo set SOURCE_DB_USER=your_render_user
    echo set SOURCE_DB_PASSWORD=your_render_password
    echo set TARGET_DATABASE_URL=your_supabase_url
    echo set TARGET_DB_USER=postgres
    echo set TARGET_DB_PASSWORD=your_supabase_password
    echo java -cp build/libs/server-all.jar org.cap.gold.migration.MigrationRunner
    echo.
    echo 📖 See migration-guide.md for detailed instructions
) else (
    echo ❌ Build failed!
    exit /b 1
)