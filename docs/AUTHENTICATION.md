# Authentication

The browser uses Spring Security form login and an HTTP-only session cookie. HTTP Basic is available for API clients. Both grant the configured administrator access to employee and department management. The application has one administrator account and no public registration.

## Administrator configuration

Set `APP_ADMIN_USERNAME` (default `admin`) and `APP_ADMIN_PASSWORD` before starting the default PostgreSQL profile. The password must contain at least 12 characters and fit within BCrypt's 72-byte limit. The application refuses to start with a missing/short password. Credentials are loaded at startup and the password is BCrypt-hashed in memory. Change the environment configuration and restart to rotate credentials; this is not a database user-management system.

Docker Compose reads these values from `.env`. Copy `.env.example`, choose a password, and keep `.env` out of source control and the submission ZIP. Direct Maven/Java launches need actual environment variables.

For the convenience H2 `demo` profile only, the default login is **admin / DemoAdmin123!**. Set `APP_ADMIN_PASSWORD` to override it. This demo credential does not apply to the default PostgreSQL profile.

## Session login

1. GET `/api/auth/csrf`; retain the `JSESSIONID` cookie and the JSON `token`/`headerName`.
2. POST `/api/auth/login` as `application/x-www-form-urlencoded`, with fields `username` and `password`, plus the returned CSRF header. Success is 204; invalid credentials return 401.
3. Fetch a fresh CSRF token after login. GET `/api/auth/me` returns the signed-in username and role. The session ID changes on login to protect against session fixation.
4. Keep the session cookie on subsequent requests. Send the CSRF header for POST, PUT, PATCH, and DELETE.
5. POST `/api/auth/logout` with the token to invalidate the session and expire its cookie. Idle sessions expire after 30 minutes. Restarting the application also ends browser sessions.

The frontend handles these steps and returns to sign-in if the session expires. It does not put credentials or authentication tokens in localStorage/sessionStorage.

## HTTP Basic and CSRF

Send `Authorization: Basic <base64(username:password)>` on API requests. Basic credentials alone do not bypass CSRF protection. Before a write, GET `/api/auth/csrf`, keep its cookie, then send the returned `X-CSRF-TOKEN` and cookie with the write. Fetch a fresh token if it expires. The Postman collection initializes a fresh token before each mutation and inherits Basic authentication from collection variables. Set the password locally; it is not included in the collection.

Swagger is accessible after browser login. To call mutations from Swagger, retrieve a token using the same browser session and put it in the `csrf` authorization field. Basic authentication can also be entered using Swagger's Authorize dialog.

CSRF remains enabled for login, logout, and all authenticated mutations, following [Spring Security's CSRF guidance](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html). API access supports [Spring Security HTTP Basic](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/basic.html).

For deployment beyond localhost, terminate HTTPS and set `SESSION_COOKIE_SECURE=true`. Basic authentication encodes credentials; TLS provides transport protection. This local assessment binds exposed Compose ports to 127.0.0.1.
