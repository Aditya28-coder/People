# Employee Management System

A Spring Boot application for managing employee records. The frontend uses HTML, CSS, and JavaScript and is served by the same application as the REST API.

## Features

- Employee creation, details, editing, and deletion with confirmation.
- Name/email search, department and status filters, pagination, and sorting by name, employee ID, department, or joining date.
- Employee fields: ID, full name, email, phone, department, designation, joining date, salary, and active/inactive status.
- Administrator login/logout and HTTP Basic authentication for API clients.
- Department creation and renaming. Renaming updates assigned employees; deletion is allowed only for empty departments.
- Filtered CSV export.
- Responsive layouts, validation messages, and duplicate-email checks.

## Stack

Java 17+, Spring Boot, Spring Data JPA, PostgreSQL, Maven, Flyway, Spring Security, JUnit 5, Mockito, and Swagger/OpenAPI. Java 21 was used for testing.

## Run with Docker

Install Docker with Compose, then run from the project directory:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and set `APP_ADMIN_PASSWORD` to a password of at least 12 characters. The password must fit within BCrypt's 72-byte limit. Choose a database password before the first database startup as well.

```sh
docker compose up --build -d
```

Open http://localhost:8080 and sign in using `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`. The username defaults to `admin`. If `PORT` was changed in `.env`, use that port instead. The initial PostgreSQL database is empty; sample data can be loaded below.

```sh
docker compose logs -f app
docker compose down
```

The database uses a named volume, retained by `docker compose down`. Changing `DB_PASSWORD` in `.env` does not update the password of an existing PostgreSQL database.

The standard Dockerfile builds the application and runs its backend tests. If a tested JAR is already available locally, a runtime-only image is also provided:

```sh
mvn verify
docker compose -f compose.yml -f compose.prebuilt.yml up --build -d
```

Rebuild the JAR before rebuilding the runtime image after source changes.

## Run with Java and Maven

Requirements: Java 17 or later, Maven 3.6.3+, and PostgreSQL. Set `JAVA_HOME` to the JDK directory. Maven needs network access for the first dependency download.

To create the database using an existing PostgreSQL installation:

```sh
psql -U postgres -f database/setup.sql
```

The setup script creates the `employees` database and role. Configure the connection and administrator password in the shell before starting the app:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/employees'
$env:DB_USERNAME = 'employees'
$env:DB_PASSWORD = 'employees'
$env:APP_ADMIN_PASSWORD = 'ReplaceWithYourPassword123!'
mvn spring-boot:run
```

Flyway applies the schema migrations automatically. No source-code changes are required.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/employees` | JDBC URL for Java/Maven launches |
| `DB_USERNAME` | `employees` | Database user |
| `DB_PASSWORD` | `employees` | Database password |
| `PORT` | `8080` | Application port |
| `APP_ADMIN_USERNAME` | `admin` | Administrator username |
| `APP_ADMIN_PASSWORD` | Required | Administrator password |
| `POSTGRES_DB` | `employees` | Compose database name |
| `DB_PORT` | `5432` | Compose database host port |
| `SESSION_COOKIE_SECURE` | `false` | Set to true when serving over HTTPS |

Compose reads `.env`; direct Java/Maven launches read environment variables. When running only PostgreSQL in Docker, set `DB_URL` to its published host port and use the credentials from `.env`.

## Demo without PostgreSQL

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Open http://localhost:8080. The demo login is **admin / DemoAdmin123!**, unless overridden with environment variables. This profile uses persistent H2 storage at `data/employee-demo` and seeds 24 fictional employees when the table is empty. Run only one process against that file. PostgreSQL remains the default database.

## Sample data

`database/sample-data.sql` contains 24 fictional employees across six departments, with 21 active and three inactive records. To load it into the Compose PostgreSQL database from PowerShell:

```powershell
Get-Content -Raw database/sample-data.sql | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U employees -d employees
```

Adjust the database/user arguments if configured differently. The script runs in one transaction and skips existing employee emails and department names. Existing records and edits are preserved. All sample emails use `example.com`.

## Tests and build

```sh
mvn verify
```

The last backend build passed 36 tests: 12 service unit tests, 15 employee API tests, five authentication tests, and four department API tests. Integration tests use isolated in-memory H2 storage in PostgreSQL compatibility mode.

Frontend tests require Node 18+:

```sh
node --test tests/frontend/directory-state.test.cjs
```

The eight frontend tests cover sort changes, URL/filter state, CSV query parameters, invalid parameters, and HTML escaping. See [test results](docs/VERIFICATION.md) for the checks completed against PostgreSQL and Docker.

The packaged application includes the frontend and migrations:

```sh
java -jar target/employee-management-1.0.0.jar
```

Set the database and administrator environment variables first. Add `--spring.profiles.active=demo` to use H2 instead.

## API

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- [HTTP request examples](docs/api.http)
- [Postman collection](docs/employee-management.postman_collection.json)
- [Authentication details](docs/AUTHENTICATION.md)

Use the configured port if it differs from 8080. Sign in through the website or supply HTTP Basic credentials. All writes require a CSRF token and its session cookie. The Postman collection obtains a token before each write; set its `baseUrl`, `username`, and `password` variables locally.

| Method | Route | Purpose |
| --- | --- | --- |
| POST / GET | `/api/employees` | Create / list employees |
| GET / PUT / DELETE | `/api/employees/{id}` | Details / update / delete |
| GET | `/api/employees/summary` | Global counts |
| GET | `/api/employees/departments` | Department names for forms/filters |
| GET | `/api/employees/export` | Filtered CSV export |
| GET / POST | `/api/departments` | List / create departments |
| PUT / DELETE | `/api/departments/{id}` | Rename / delete an empty department |
| GET | `/api/auth/csrf` | Obtain a CSRF token |
| POST | `/api/auth/login` | Form login |
| GET | `/api/auth/me` | Current administrator |
| POST | `/api/auth/logout` | End the session |

List parameters are `search`, `department`, `status`, `page`, `size`, `sortBy`, and `direction`. Pages start at 0; size defaults to 10 and must be 1–100. Search matches name or email without regard to case. Department/status filters combine with search using AND. Status is `ACTIVE` or `INACTIVE`.

Supported API sort fields are `id`, `fullName`, `email`, `department`, `designation`, `dateOfJoining`, `salary`, and `status`. Direction is `asc` or `desc`. CSV uses the same filters and sorting without pagination.

Example employee body:

```json
{
  "fullName": "Ananya Sharma",
  "email": "ananya@example.com",
  "phoneNumber": "+91 98765 43210",
  "department": "Engineering",
  "designation": "Backend Engineer",
  "dateOfJoining": "2025-01-10",
  "salary": 1250000.25,
  "status": "ACTIVE"
}
```

Errors use `application/problem+json`. Invalid input returns 400, unauthenticated access 401, CSRF/permission failures 403, missing records 404, and duplicate records/concurrent-write conflicts 409. Field validation errors include an `errors` map.

## Implementation notes

- Employee IDs are generated by the database and displayed as `EMP-0001`. Deleted or failed inserts can leave gaps.
- Salary is an annual INR amount, stored as `NUMERIC(14,2)` and handled with `BigDecimal`. The browser submits decimal text to preserve precision.
- Joining dates are date-only. Future joining dates are accepted; audit timestamps use UTC.
- Emails are trimmed and stored lowercase, with a database uniqueness constraint.
- Department names are unique without regard to case. Assignment reuses the canonical name or creates a new department. Renames update employees in the same transaction.
- Overview cards show global counts. Filters affect the table and export.
- Authentication uses one configured administrator, BCrypt hashing, HTTP-only session cookies, CSRF protection, and a 30-minute idle timeout. Public registration is not included.

## Project layout

```text
src/main/java/com/kloudspot/employees/
  employee/       Employee entity, service, repository, DTOs, and controller
  department/     Department management
  common/         Exceptions and API error handling
  config/         Authentication, OpenAPI, and demo seeding
src/main/resources/
  static/         HTML, CSS, and JavaScript
  db/migration/   Flyway schema migrations
src/test/         Backend unit and integration tests
tests/frontend/   Frontend unit tests
database/         PostgreSQL setup and sample data
docs/             API examples, walkthrough, and screenshots
```

## Screenshots and source archive

See the [walkthrough](docs/DEMO.md) and [screenshot index](docs/screenshots/README.md). Eight screenshots cover the employee screens, mobile directory, login, and departments.

To generate a source archive:

```sh
python scripts/package_submission.py
```

This creates `output/employee-management-source.zip` with source, configuration, tests, documentation, and screenshots. Local credentials, database files, build output, and Git metadata are excluded.
