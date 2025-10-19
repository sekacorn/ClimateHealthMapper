# Climate Health Mapper - User Session Service

User authentication and session management microservice with SSO and MFA support.

## Features

- **User Authentication**: Traditional username/password authentication with BCrypt hashing
- **Multi-Factor Authentication (MFA)**: TOTP-based 2FA with backup codes
- **Single Sign-On (SSO)**: Integration with multiple identity providers:
  - Google OAuth2
  - Azure AD (OIDC)
  - Okta
  - SAML 2.0
- **JWT Token Management**: Stateless authentication with access and refresh tokens
- **Role-Based Access Control (RBAC)**: Support for USER, MODERATOR, ADMIN, and ENTERPRISE roles
- **Security Features**:
  - Account lockout after failed login attempts
  - Password complexity requirements
  - Audit logging for security events
  - PKCE for OAuth2 flows
- **Session Management**: Redis-backed session storage
- **Enterprise Support**: Organization-based user management

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Security**: Spring Security 6, OAuth2, SAML 2.0
- **Database**: PostgreSQL with JPA/Hibernate
- **Cache**: Redis
- **Authentication**: JWT (jjwt 0.12.3)
- **MFA**: TOTP with Google Authenticator compatibility
- **Password Hashing**: BCrypt
- **Windows Auth**: Waffle-JNA (optional)

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 13+
- Redis 6+

### Installation

1. Clone the repository
2. Copy `.env.example` to `.env` and configure:
   ```bash
   cp .env.example .env
   ```

3. Update database credentials in `.env`

4. Build the project:
   ```bash
   mvn clean install
   ```

5. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The service will start on port 8081.

### Docker Deployment

Build and run with Docker:

```bash
docker build -t climate-user-session .
docker run -p 8081:8081 \
  -e DB_PASSWORD=postgres \
  -e JWT_SECRET=your-secret-key \
  climate-user-session
```

## API Endpoints

### Authentication

#### Register User
```http
POST /api/session/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

#### Login
```http
POST /api/session/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "johndoe",
  "password": "SecurePass123!"
}
```

Response:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "requiresMfa": false,
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"]
  }
}
```

### SSO Authentication

#### Initiate Google SSO
```http
GET /api/session/auth/login/sso/google
```

Response:
```json
{
  "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?...",
  "state": "uuid-state-token"
}
```

#### SSO Callback (handled automatically)
```http
GET /api/session/auth/login/sso/{provider}/callback?code=...&state=...
```

Supported providers: `google`, `azure`, `okta`, `saml`

### MFA

#### Enable MFA
```http
POST /api/session/auth/mfa/enable
Authorization: Bearer {token}
Content-Type: application/json

{
  "password": "current-password"
}
```

Response:
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeUrl": "data:image/png;base64,...",
  "backupCodes": ["123456", "234567", ...],
  "message": "MFA enabled successfully"
}
```

#### Verify MFA
```http
POST /api/session/auth/mfa/verify
Content-Type: application/json

{
  "sessionToken": "mfa-session-token",
  "code": "123456"
}
```

### Session Management

#### Get Current User
```http
GET /api/session/me
Authorization: Bearer {token}
```

#### Validate Token
```http
POST /api/session/validate
Content-Type: application/json

{
  "token": "jwt-token"
}
```

#### Refresh Token
```http
POST /api/session/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh-token"
}
```

#### Logout
```http
POST /api/session/auth/logout
Authorization: Bearer {token}
```

## Configuration

### SSO Provider Setup

#### Google OAuth2
1. Create project in [Google Cloud Console](https://console.cloud.google.com)
2. Enable Google+ API
3. Create OAuth2 credentials
4. Set redirect URI: `http://localhost:8081/api/session/auth/login/sso/google/callback`
5. Add credentials to `.env`:
   ```
   GOOGLE_CLIENT_ID=your-client-id
   GOOGLE_CLIENT_SECRET=your-client-secret
   ```

#### Azure AD
1. Register app in [Azure Portal](https://portal.azure.com)
2. Add redirect URI
3. Create client secret
4. Configure in `.env`:
   ```
   AZURE_CLIENT_ID=your-client-id
   AZURE_CLIENT_SECRET=your-secret
   AZURE_TENANT_ID=your-tenant-id
   ```

#### Okta
1. Create app in [Okta Admin Console](https://admin.okta.com)
2. Set application type to Web
3. Configure redirect URIs
4. Add to `.env`:
   ```
   OKTA_CLIENT_ID=your-client-id
   OKTA_CLIENT_SECRET=your-secret
   OKTA_DOMAIN=your-domain.okta.com
   ```

### JWT Configuration

Generate a secure secret key:
```bash
openssl rand -base64 64
```

Update in `.env`:
```
JWT_SECRET=your-generated-secret
```

### Database Schema

The service automatically creates tables on startup. Manual schema creation:

```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255),
  full_name VARCHAR(255),
  -- ... (see schema.sql for complete definition)
);
```

## Security Considerations

1. **Password Policy**:
   - Minimum 8 characters
   - Must contain uppercase, lowercase, numbers, and special characters
   - BCrypt hashing with 12 rounds

2. **Account Lockout**:
   - Locked after 5 failed login attempts
   - 30-minute lockout duration

3. **Token Expiration**:
   - Access tokens: 1 hour
   - Refresh tokens: 24 hours

4. **MFA**:
   - TOTP with 30-second time window
   - 10 backup codes per user
   - Clock skew tolerance: ±30 seconds

## Testing

Run tests:
```bash
mvn test
```

Run specific test:
```bash
mvn test -Dtest=MfaServiceTest
```

## Monitoring

Health check endpoint:
```http
GET /api/session/health
```

Actuator endpoints:
- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Verify PostgreSQL is running
   - Check credentials in `.env`
   - Ensure database exists

2. **Redis Connection Failed**
   - Verify Redis is running
   - Check port configuration

3. **SSO Not Working**
   - Verify client IDs and secrets
   - Check redirect URIs match exactly
   - Ensure provider is properly configured

## License

Copyright 2024 Climate Health Mapper

## Support

For issues and questions, please open an issue on GitHub.
