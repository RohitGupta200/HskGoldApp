#!/bin/bash

# Build script for migration tool
echo "🔨 Building CapGold Migration Tool..."

# Navigate to server directory
cd server

# Clean and build the project
echo "📦 Building project..."
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "🚀 You can now run the migration using:"
    echo ""
    echo "Method 1 - Command line with parameters:"
    echo "java -cp build/libs/server-all.jar org.cap.gold.migration.MigrationRunner \\"
    echo "  \"SOURCE_URL\" \"SOURCE_USER\" \"SOURCE_PASS\" \\"
    echo "  \"TARGET_URL\" \"TARGET_USER\" \"TARGET_PASS\""
    echo ""
    echo "Method 2 - Environment variables:"
    echo "export SOURCE_DATABASE_URL=\"your_render_url\""
    echo "export SOURCE_DB_USER=\"your_render_user\""
    echo "export SOURCE_DB_PASSWORD=\"your_render_password\""
    echo "export TARGET_DATABASE_URL=\"your_supabase_url\""
    echo "export TARGET_DB_USER=\"postgres\""
    echo "export TARGET_DB_PASSWORD=\"your_supabase_password\""
    echo "java -cp build/libs/server-all.jar org.cap.gold.migration.MigrationRunner"
    echo ""
    echo "📖 See migration-guide.md for detailed instructions"
else
    echo "❌ Build failed!"
    exit 1
fi