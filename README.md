# Finance Dashboard Backend

A Spring Boot 3 backend for a finance dashboard system focused on clean API design, role-based access control, financial record management, and summary analytics.

This project was built to satisfy the assignment as a backend assessment, with emphasis on correctness, clarity, maintainability, and explicit business rules rather than unnecessary complexity.

## What This Submission Demonstrates

- Clear separation of concerns across controllers, services, repositories, DTOs, security, and persistence
- Explicit role-based behavior for `VIEWER`, `ANALYST`, and `ADMIN`
- Financial record CRUD with filtering, pagination, sorting, search, and soft delete
- Dashboard-focused aggregate APIs beyond basic CRUD
- Defensive validation and structured error responses
- JWT-based stateless authentication
- Swagger/OpenAPI documentation for manual verification
- Automated tests covering core business and security behavior

## Assignment Coverage

| Assignment requirement | Implementation in this project |
| --- | --- |
| User and role management | User creation, listing, lookup, and update APIs with `Role` and `AccountStatus` |
| Financial records management | Record CRUD with filtering by date, type, category, and text search |
| Dashboard summary APIs | Total income, total expenses, net balance, category totals, monthly summaries, and recent transactions |
| Access control logic | Spring Security request rules plus method-level authorization by role |
| Validation and error handling | Bean validation, consistent JSON error responses, and appropriate HTTP status codes |
| Data persistence | PostgreSQL with Spring Data JPA and Hibernate |
| Enhancements | JWT auth, pagination, search, soft delete, rate limiting, Swagger/OpenAPI, unit/controller tests |

## Technical Stack

- Java 17
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- JWT (`jjwt`)
- Springdoc OpenAPI / Swagger UI
- JUnit 5 / Mockito / MockMvc

## Architecture

The codebase follows a layered structure:

- `controller`
  Handles HTTP requests, request validation binding, response status codes, and endpoint exposure.
- `service`
  Contains business rules, access-aware behavior, filtering logic, and orchestration.
- `repository`
  Encapsulates persistence operations and aggregate queries.
- `entity`
  Defines the database-backed domain model.
- `dto`
  Keeps API contracts separate from persistence models.
- `security`
  Implements JWT parsing, authentication, authorization, and security-specific error handling.
- `config`
  Contains security, CORS, rate limiting, OpenAPI, and application behavior configuration.
- `exception`
  Centralizes structured error handling for predictable API responses.

This structure is intentionally straightforward. The goal is to make the reasoning and data flow easy to follow during evaluation.

## Domain Model

### Users

The `users` table is modeled by `User` and includes:

- `id`
- `name`
- `email` with a unique constraint
- `password`
- `role`
- `status`
- `createdAt`
- `updatedAt`

### Financial Records

The `financial_records` table is modeled by `FinancialRecord` and includes:

- `id`
- `amount`
- `type` (`INCOME` or `EXPENSE`)
- `category`
- `date`
- `description`
- `user_id` foreign key to the creating user
- `deleted`
- `deletedAt`
- `createdAt`
- `updatedAt`

Financial records use soft delete so deleted data does not disappear from the database permanently.

## Role Model

| Role | Capabilities |
| --- | --- |
| `VIEWER` | Can access dashboard summary data only |
| `ANALYST` | Can access dashboard data and read financial records |
| `ADMIN` | Can manage users and perform full financial record CRUD |

## Access Matrix

| Endpoint area | Public | Viewer | Analyst | Admin |
| --- | --- | --- | --- | --- |
| Auth (`/api/auth/**`) | Yes | Yes | Yes | Yes |
| Dashboard (`GET /api/dashboard/summary`) | No | Yes | Yes | Yes |
| Record read (`GET /api/financial-records/**`) | No | No | Yes | Yes |
| Record write (`POST/PUT/DELETE /api/financial-records/**`) | No | No | No | Yes |
| User management (`/api/users/**`) | No | No | No | Yes |
| Swagger / OpenAPI | Yes | Yes | Yes | Yes |

## Important Design Decision: Self-Registration

Public registration is intentionally constrained by configuration.

The API accepts `role` and `status` in the register payload, but the allowed values are controlled by application policy. With the default configuration:

- self-registration is limited to `VIEWER`
- self-registration is limited to `ACTIVE`

Why this matters:

- It prevents anonymous users from assigning themselves elevated roles
- It keeps the access-control model internally consistent
- It separates public signup from admin-managed role assignment

Elevated roles such as `ANALYST` and `ADMIN` are created through the protected `/api/users` flow by an authenticated admin.

## API Overview

### Authentication

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Public signup subject to self-registration policy |
| `POST` | `/api/auth/login` | Returns JWT bearer token |

Register example:

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "password123",
  "role": "VIEWER",
  "status": "ACTIVE"
}
```

Login example:

```json
{
  "email": "jane@example.com",
  "password": "password123"
}
```

Typical login response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@example.com",
    "role": "VIEWER",
    "status": "ACTIVE"
  }
}
```

The returned token is used as:

```text
Authorization: Bearer <jwt-token>
```

### User Management

Admin-only user management endpoints:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/users` | Create a user with explicit role and status |
| `GET` | `/api/users` | List users |
| `GET` | `/api/users/{id}` | Get a user by id |
| `PUT` | `/api/users/{id}` | Update name, email, password, role, or status |

This project intentionally does not expose a hard-delete user endpoint. User lifecycle is handled through `status`, which keeps access-control behavior explicit and safer for an assessment project.

Create user example:

`POST /api/users`

```json
{
  "name": "Analyst User",
  "email": "analyst.user@example.com",
  "password": "password123",
  "role": "ANALYST",
  "status": "ACTIVE"
}
```

Typical response:

```json
{
  "id": 2,
  "name": "Analyst User",
  "email": "analyst.user@example.com",
  "role": "ANALYST",
  "status": "ACTIVE"
}
```

List users example:

`GET /api/users`

Typical response:

```json
[
  {
    "id": 1,
    "name": "Admin User",
    "email": "admin.user@example.com",
    "role": "ADMIN",
    "status": "ACTIVE"
  },
  {
    "id": 2,
    "name": "Analyst User",
    "email": "analyst.user@example.com",
    "role": "ANALYST",
    "status": "ACTIVE"
  }
]
```

Get single user example:

`GET /api/users/2`

Typical response:

```json
{
  "id": 2,
  "name": "Analyst User",
  "email": "analyst.user@example.com",
  "role": "ANALYST",
  "status": "ACTIVE"
}
```

Update user example:

`PUT /api/users/2`

```json
{
  "name": "Analyst User Updated",
  "status": "INACTIVE"
}
```

Typical response:

```json
{
  "id": 2,
  "name": "Analyst User Updated",
  "email": "analyst.user@example.com",
  "role": "ANALYST",
  "status": "INACTIVE"
}
```

### Financial Records

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/financial-records` | Create a financial record |
| `GET` | `/api/financial-records` | List records with filters and pagination |
| `GET` | `/api/financial-records/{id}` | Get a record by id |
| `PUT` | `/api/financial-records/{id}` | Update a record |
| `DELETE` | `/api/financial-records/{id}` | Soft delete a record |

Alias routes are also exposed under `/api/records`.

Supported query parameters for listing:

- `fromDate`
- `toDate`
- `type`
- `category`
- `search`
- `page`
- `size`
- `sortBy`
- `sortDirection`

Record example:

```json
{
  "amount": 2500.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "description": "Monthly salary"
}
```

Create record example:

`POST /api/financial-records`

```json
{
  "amount": 2500.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "description": "Monthly salary"
}
```

Typical response:

```json
{
  "id": 10,
  "amount": 2500.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "description": "Monthly salary",
  "userId": 1,
  "userName": "Admin User",
  "userEmail": "admin.user@example.com",
  "createdAt": "2026-04-05T10:15:30.000000",
  "updatedAt": "2026-04-05T10:15:30.000000"
}
```

List records example:

`GET /api/financial-records?page=0&size=10&sortBy=date&sortDirection=DESC`

Typical paginated response:

```json
{
  "content": [
    {
      "id": 10,
      "amount": 2500.00,
      "type": "INCOME",
      "category": "Salary",
      "date": "2026-04-01",
      "description": "Monthly salary",
      "userId": 1,
      "userName": "Admin User",
      "userEmail": "admin.user@example.com",
      "createdAt": "2026-04-05T10:15:30.000000",
      "updatedAt": "2026-04-05T10:15:30.000000"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

Filtered search example:

`GET /api/financial-records?category=Salary&type=INCOME&search=salary&fromDate=2026-04-01&toDate=2026-04-30`

Get single record example:

`GET /api/financial-records/10`

Typical response:

```json
{
  "id": 10,
  "amount": 2500.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "description": "Monthly salary",
  "userId": 1,
  "userName": "Admin User",
  "userEmail": "admin.user@example.com",
  "createdAt": "2026-04-05T10:15:30.000000",
  "updatedAt": "2026-04-05T10:15:30.000000"
}
```

Update record example:

`PUT /api/financial-records/10`

```json
{
  "amount": 2750.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "description": "Updated salary"
}
```

Typical response:

```json
{
  "id": 10,
  "amount": 2750.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "description": "Updated salary",
  "userId": 1,
  "userName": "Admin User",
  "userEmail": "admin.user@example.com",
  "createdAt": "2026-04-05T10:15:30.000000",
  "updatedAt": "2026-04-05T10:20:45.000000"
}
```

Delete record example:

`DELETE /api/financial-records/10`

Behavior:

- returns `204 No Content`
- marks the record as deleted through soft delete
- excluded from normal record queries afterward

### Dashboard

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/dashboard/summary` | Returns aggregate dashboard data |

The summary response includes:

- `totalIncome`
- `totalExpenses`
- `netBalance`
- `categoryTotals`
- `monthlySummaries`
- `recentTransactions`

This endpoint supports `recentLimit` for controlling recent activity size.

Dashboard summary example:

`GET /api/dashboard/summary?recentLimit=5`

Typical response:

```json
{
  "totalIncome": 2750.00,
  "totalExpenses": 300.00,
  "netBalance": 2450.00,
  "categoryTotals": [
    {
      "category": "Salary",
      "total": 2750.00
    },
    {
      "category": "Food",
      "total": 300.00
    }
  ],
  "monthlySummaries": [
    {
      "month": "2026-04",
      "income": 2750.00,
      "expense": 300.00
    }
  ],
  "recentTransactions": [
    {
      "id": 10,
      "amount": 2750.00,
      "type": "INCOME",
      "category": "Salary",
      "date": "2026-04-01",
      "description": "Updated salary",
      "userName": "Admin User"
    }
  ]
}
```

## Validation and Error Handling

The API returns structured JSON errors with:

- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `details`

Typical behavior:

- `400 Bad Request`
  Validation failures, malformed JSON, invalid enum values, or invalid operations such as an inverted date range
- `401 Unauthorized`
  Invalid credentials, invalid JWT, expired token, or disabled user access
- `403 Forbidden`
  Authenticated user lacks permission for the requested action
- `404 Not Found`
  Missing user or financial record
- `409 Conflict`
  Duplicate email or other data conflicts
- `429 Too Many Requests`
  Rate limiting triggered

## Security

- JWT bearer authentication for protected routes
- Stateless session policy
- Role-based authorization at both request-rule and method level
- Configurable self-registration policy
- CORS configuration for local frontend access
- Rate limiting for both auth endpoints and general API traffic
- Inactive accounts are blocked from authenticated use

## Rate Limiting

Default behavior:

- `POST /api/auth/register` and `POST /api/auth/login`
  `5` requests per `60` seconds per IP
- Other `/api/**` routes
  `120` requests per `60` seconds per authenticated user, or by IP when unauthenticated
- `OPTIONS` preflight requests are excluded

## Persistence

- Database: PostgreSQL
- ORM: Spring Data JPA / Hibernate
- Current schema management approach: Hibernate `ddl-auto: update`

For this assignment, the entity model is the primary schema source. This keeps the setup lightweight, though a production-oriented version would typically use explicit migration files.

## API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Local Setup

1. Create a PostgreSQL database named `finance_dashboard`.
2. Configure the datasource in `src/main/resources/application.yml` or override via environment variables.
   The committed configuration now uses environment-variable based values with safe local defaults instead of personal machine credentials.
3. Run the test suite:

```bash
mvn test
```

4. Start the application:

```bash
mvn spring-boot:run
```

5. Open Swagger UI and test the endpoints.

## Configuration

Useful overrides:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `APP_JWT_EXPIRATION_MINUTES`

Example local PowerShell overrides:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/finance_dashboard"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "your-local-password"
$env:APP_JWT_SECRET = "your-base64-encoded-32-byte-secret"
```

Self-registration policy is configurable through:

- `app.auth.registration.default-role`
- `app.auth.registration.default-status`
- `app.auth.registration.allowed-roles`
- `app.auth.registration.allowed-statuses`

## Testing

The project includes automated coverage for:

- authentication and JWT behavior
- duplicate email handling
- self-registration policy enforcement
- authorization for `VIEWER`, `ANALYST`, and `ADMIN`
- financial record create/read/update/delete flows
- filtering and invalid date-range rejection
- dashboard aggregation logic
- rate limiting
- CORS preflight handling
- OpenAPI configuration
- controller mappings
- soft delete configuration
