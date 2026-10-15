# Test results

## Automated tests

The last backend build completed successfully with Java 21 and Maven 3.9.16:

```sh
mvn -B -ntp verify -DargLine=-Xmx384m
```

| Test suite | Passed |
| --- | ---: |
| EmployeeServiceTest | 12 |
| EmployeeApiTest | 15 |
| SecurityApiTest | 5 |
| DepartmentApiTest | 4 |
| **Total** | **36** |

There were no failures, errors, or skipped tests. Integration tests use in-memory H2 in PostgreSQL compatibility mode; they do not use the application database.

The following frontend checks passed:

```sh
node --check src/main/resources/static/app.js
node --check src/main/resources/static/directory-state.js
node --test tests/frontend/directory-state.test.cjs
```

All eight frontend tests passed. They cover employee ID sort toggling, column changes, URL/filter round trips, invalid parameters, CSV query parameters, HTML escaping, and field limits.

## PostgreSQL and Docker

The runtime Docker image was built from the tested JAR using `Dockerfile.runtime` and `compose.prebuilt.yml`. PostgreSQL 17.11 started successfully, Hibernate schema validation passed, and Flyway migrations V1 and V2 were applied.

The following check passed against the PostgreSQL-backed application:

```sh
python scripts/verify_running_app.py --restart-containers
```

It verified:

- Session login/logout, incorrect-password rejection, HTTP Basic, unauthenticated denial, and CSRF enforcement.
- Employee CRUD, ID sorting in both directions, combined filters, salary precision, and CSV export.
- Department rename propagation, duplicate-name rejection, and deletion protection for departments containing employees.
- Persistence of saved employee records after restarting the application and PostgreSQL containers.

The script removed its temporary records after completion. It restarts only the `people-enhancements` Compose project when the restart flag is supplied.

The standard multi-stage Dockerfile was not executed during these checks. Verification used the prebuilt-JAR image path. The Postman collection was checked for valid structure but was not executed in Postman.

## Interface checks

The employee directory, search/filter combinations, create/edit forms, details page, delete confirmation, pagination, and sorting were checked using the H2 demo. A 390-pixel mobile viewport was checked for page overflow. The directory table scrolls within its container.

Automated frontend coverage is limited to the shared state and escaping rules; it is not an end-to-end browser suite. The login and department screens are shown in the [screenshot index](screenshots/README.md).

## Sample data

The PostgreSQL sample-data script inserted 24 fictional employees across six departments. Authenticated API reads confirmed 21 active and three inactive employees, and employee ID sorting returned the expected order. The script skips existing employee emails and department names.
