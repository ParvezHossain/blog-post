# 📝 Blog Post API

A production-grade **Java Spring Boot** REST API for a multi-user blog platform, featuring JWT authentication, Google & GitHub OAuth2 login, role-based access control, Redis caching, RabbitMQ messaging, and category-based post subscriptions.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security 7 · JWT (jjwt 0.11.5) · OAuth2 (Google, GitHub) |
| Database | PostgreSQL (JPA / Hibernate 7) |
| Cache | Redis (Spring Cache) |
| Messaging | RabbitMQ (Spring AMQP) |
| Email | Spring Mail |
| Build | Maven |
| Utilities | Lombok · dotenv-java |

---

## ✨ Features

### Authentication & Authorization
- **Local registration** — strict username rules (8 chars, upper + lower + special) and BCrypt-hashed passwords
- **Google OAuth2 login** — auto-registers users via Google profile; issues JWT pair on success
- **GitHub OAuth2 login** — auto-registers users via GitHub profile; handles private-email edge case gracefully
- **JWT Access + Refresh Token** pair on every login
- **Refresh token rotation** — old token is invalidated on every refresh
- **Logout with token denylist** — revoked tokens stored in Redis with automatic TTL expiry
- **Forgot / Reset password** flow via email

### Role-Based Access Control (RBAC)
- `ADMIN` — full CRUD on blog posts
- `USER` — read-only access to public posts
- *(Planned)* `PREMIUM` — access to premium posts

### Blog Posts
- Paginated public post listing
- Single post retrieval by ID
- Admin-only create, update, and delete
- Each post belongs to a **category**
- *(Planned)* Premium posts visible only to subscribed users

### Security Hardening
- Stateless JWT — CSRF disabled
- `X-Frame-Options: DENY` (clickjacking protection)
- HSTS with 1-year max-age and subdomains
- Content Security Policy (CSP)
- Permissions Policy (camera, microphone, geolocation blocked)
- CORS configured per environment
- Optimistic locking (`@Version`) on all user records
- Rate limiting filter
- Request counter filter

### Infrastructure
- Redis for token denylist and response caching
- RabbitMQ for async email / notification events
- Validation groups (`LocalUserValidation`) to separate strict local-user rules from OAuth2 user rules

---

## 🗂️ Project Structure

```
src/main/java/com/parvez/blogs/
├── config/
│   ├── SecurityConfig.java          # Filter chain, CORS, OAuth2, headers
│   ├── JwtAuthFilter.java           # JWT extraction and SecurityContext population
│   ├── OAuth2SuccessHandler.java    # Post-OAuth2 user creation + JWT issuance
│   ├── OAuth2FailureHandler.java    # OAuth2 error responses
│   ├── AuthProvider.java            # Enum: LOCAL | GOOGLE | GITHUB
│   ├── ApiPaths.java                # Centralized URL constants
│   └── RateLimitingFilter.java      # Per-IP rate limiting
├── controller/
│   ├── AuthController.java          # /register /login /logout /refresh-token
│   └── PostController.java          # CRUD endpoints for blog posts
├── entity/
│   ├── User.java                    # Users table with validation groups
│   ├── RefreshToken.java            # Refresh token store
│   └── Role.java                    # Enum: ADMIN | USER
├── service/
│   ├── AuthService.java             # Registration, login, logout, refresh rotation
│   ├── PostService.java             # Post business logic
│   └── RefreshTokenService.java     # Refresh token lifecycle
├── repository/
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   └── TokenDenylistRepository.java # Redis-backed denylist
├── security/
│   └── JwtUtil.java                 # Token generation and parsing
├── validation/
│   └── LocalUserValidation.java     # Validation group marker interface
└── exception/                       # Custom exception types
```

---

## 🔐 Authentication Flows

### Local Registration & Login

```
POST /api/v1/auth/register
{
  "username": "Pa@rvez1",   // exactly 8 chars, upper + lower + special
  "email": "user@email.com",
  "password": "Secret@99",
  "firstName": "Parvez",
  "lastName": "Hossain"
}

POST /api/v1/auth/login
→ { "accessToken": "...", "refreshToken": "..." }
```

### OAuth2 Login (Browser)

| Provider | Start URL |
|----------|-----------|
| Google   | `GET /oauth2/authorize/google` |
| GitHub   | `GET /oauth2/authorize/github` |

Both providers redirect back with:
```json
{
  "accessToken":  "<JWT>",
  "refreshToken": "<opaque>"
}
```

OAuth2 users are **auto-registered** on first login using their provider profile. A user registered via Google cannot be hijacked by GitHub login on the same email, and vice versa — provider isolation is enforced at both the application and database level.

### Token Refresh & Logout

```
POST /api/v1/auth/refresh-token
{ "refreshToken": "..." }
→ New access + refresh token pair (old refresh token is revoked)

POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
→ 200 OK (token added to Redis denylist with remaining TTL)
```

---

## 📋 API Reference

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` | Public | Register a new local user |
| `POST` | `/api/v1/auth/login` | Public | Login with username + password |
| `POST` | `/api/v1/auth/logout` | Bearer | Logout and revoke token |
| `POST` | `/api/v1/auth/refresh-token` | Public | Rotate refresh token |
| `POST` | `/api/v1/auth/forgot-password` | Public | Request password reset email |
| `POST` | `/api/v1/auth/reset-password` | Public | Reset password with token |
| `GET`  | `/oauth2/authorize/google` | Public | Start Google OAuth2 flow |
| `GET`  | `/oauth2/authorize/github` | Public | Start GitHub OAuth2 flow |

### Posts

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/post?page=0&size=10` | Public | Paginated list of posts |
| `GET` | `/api/v1/post/{id}` | Public | Get a single post |
| `POST` | `/api/v1/post` | ADMIN | Create a new post |
| `PUT` | `/api/v1/post/{id}` | ADMIN | Update a post |
| `DELETE` | `/api/v1/post/{id}` | ADMIN | Delete a post |

---

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/blogpost
DB_USERNAME=postgres
DB_PASSWORD=yourpassword

# JWT
JWT_SECRET=your-256-bit-secret
JWT_EXPIRATION_MS=900000

# OAuth2 — Google
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# OAuth2 — GitHub
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password
```

### `application.yml` — OAuth2 Registration

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: "${GOOGLE_CLIENT_ID}"
            client-secret: "${GOOGLE_CLIENT_SECRET}"
            scope: [email, profile]
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          github:
            client-id: "${GITHUB_CLIENT_ID}"
            client-secret: "${GITHUB_CLIENT_SECRET}"
            scope: [read:user, user:email]
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
```

### OAuth2 Redirect URIs to Register

**Google Cloud Console → Authorized redirect URIs:**
```
http://localhost:8081/login/oauth2/code/google
```

**GitHub → OAuth App → Authorization callback URL:**
```
http://localhost:8081/login/oauth2/code/github
```

---

## 🗄️ Database Schema (Key Tables)

```sql
-- users
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  UNIQUE NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255),                   -- NULL for OAuth2 users
    first_name VARCHAR(50)  NOT NULL,
    last_name  VARCHAR(50)  NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    provider   VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',  -- LOCAL | GOOGLE | GITHUB
    version    BIGINT       NOT NULL DEFAULT 0          -- Optimistic locking
);

-- refresh_tokens
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    token       TEXT         NOT NULL UNIQUE,
    expiry_date TIMESTAMPTZ  NOT NULL
);

-- posts
CREATE TABLE posts (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    category   VARCHAR(100),
    premium    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

---

## 🔮 Planned Features

- **Premium posts** — content behind a paywall, visible only to subscribed users
- **Category subscriptions** — users choose which categories appear in their feed and set priority ordering
- **Post priority feed** — personalized feed sorted by user-defined category preferences
- **Comment system** — threaded comments per post
- **Admin dashboard metrics** — post views, user growth, subscription stats via RabbitMQ events

---

## 🛡️ Security Design Decisions

**Why separate validation groups?**
Local users require strict username rules (8 chars, upper + lower + special) and a BCrypt password. OAuth2 users are auto-registered with `google_{sub}` or `github_{id}` as their username and no password. Using `LocalUserValidation` as a Jakarta group ensures these constraints are enforced only for `POST /register`, not for OAuth2 auto-registration — without splitting the `User` entity into two tables.

**Why Redis for the token denylist?**
JWTs are stateless by design, but logout requires invalidation. Redis stores revoked tokens with a TTL equal to the token's remaining lifetime, so the denylist self-cleans automatically without a cron job or database purge.

**Why refresh token rotation?**
Each use of a refresh token issues a new pair and revokes the old one. If a refresh token is stolen and used by an attacker, the legitimate user's next refresh attempt will fail, signalling the compromise.

**Why `AuthProvider` isolation?**
Without provider isolation, an attacker who controls a victim's Google account could access their local-password account by signing in via Google with the same email. The `provider` field on `User` enforces that `google_*` tokens can only authenticate `GOOGLE` accounts and vice versa.

---

## 🏃 Running Locally

```bash
# 1. Start infrastructure
docker compose up -d   # starts PostgreSQL, Redis, RabbitMQ

# 2. Clone and build
git clone https://github.com/ParvezHossain/blog-post
cd blog-post
cp .env.example .env   # fill in your credentials

# 3. Run
./mvnw spring-boot:run

# API is available at http://localhost:8081
```

---

## 👤 Author

**Parvez Hossain**
Java Spring Boot Developer
[GitHub](https://github.com/ParvezHossain) · [LinkedIn](https://www.linkedin.com/in/parvez-hossain)
