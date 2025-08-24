# CapGold Server

This is the backend server for the CapGold application, built with Ktor and Firebase Authentication.

## Prerequisites

- JDK 17 or higher
- Kotlin 1.8.0 or higher
- PostgreSQL 13 or higher
- Firebase project with Authentication enabled
- Service account key for Firebase Admin SDK

## Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd CapGold/server
   ```

2. **Set up environment variables**
   - Copy `.env.example` to `.env`
   - Update the values in `.env` with your configuration

3. **Set up Firebase**
   - Go to the [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or select an existing one
   - Go to Project Settings > Service Accounts
   - Click "Generate New Private Key" and save it as `service-account.json` in the project root

4. **Set up the database**
   ```sql
   CREATE DATABASE capgold;
   CREATE USER postgres WITH PASSWORD 'postgres';
   GRANT ALL PRIVILEGES ON DATABASE capgold TO postgres;
   ```

5. **Run the application**
   ```bash
   ./gradlew run
   ```

   The server will start on `http://localhost:8080`

## API Endpoints

### Authentication

- `POST /auth/signin/phone` - Sign in with phone number and password
- `POST /auth/signup/phone` - Register a new user with phone number
- `POST /auth/refresh` - Refresh access token
- `POST /auth/password/reset/sms` - Request password reset SMS

### User

- `GET /auth/me` - Get current user profile (requires authentication)
- `PUT /auth/me` - Update user profile (requires authentication)
- `POST /auth/password/change` - Change password (requires authentication)
- `DELETE /auth/me` - Delete account (requires authentication)

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `ENVIRONMENT` | Application environment | `development` |
| `JWT_SECRET` | Secret key for JWT signing | - |
| `JWT_ISSUER` | JWT issuer | `capgold-server` |
| `JWT_AUDIENCE` | JWT audience | `capgold-client` |
| `JWT_REALM` | JWT realm | `capgold` |
| `DATABASE_URL` | Database connection URL | - |
| `DB_USER` | Database user | - |
| `DB_PASSWORD` | Database password | - |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to Firebase service account key | `service-account.json` |
| `FIREBASE_PROJECT_ID` | Firebase project ID | - |
| `FIREBASE_STORAGE_BUCKET` | Firebase storage bucket | - |
| `APP_BASE_URL` | Base URL for the frontend | `http://localhost:3000` |
| `SHOW_DETAILED_ERRORS` | Show detailed error messages | `true` in development |

## Development

### Running Tests

```bash
./gradlew test
```

### Building for Production

```bash
./gradlew build
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
