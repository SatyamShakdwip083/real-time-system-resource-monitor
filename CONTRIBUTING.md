# Contributing to Real-Time System Resource Monitor

## Development setup

1. **Prerequisites**: Java 17, Node.js 18+, Maven 3.6+.
2. **Backend**: `cd backend && mvn spring-boot:run` (API at http://localhost:8081).
3. **Frontend**: `cd frontend && npm install && npm run dev` (dashboard at http://localhost:3000).

Copy `backend/.env.example` and `frontend/.env.example` to `.env` if you need to override defaults.

## Code quality

- **Backend**: `mvn verify` runs unit and integration tests.
- **Frontend**:
  - `npm run lint` — ESLint (run before committing).
  - `npm run format` — Prettier (format code).
  - `npm run typecheck` — TypeScript check.
  - `npm run test` — Vitest unit tests.

Pre-commit (Husky + lint-staged) runs ESLint and Prettier on staged files when the repo is a git repository.

## Submitting changes

1. Fork the repo and create a branch.
2. Make your changes; ensure tests and lint pass.
3. Open a pull request with a short description of the change.

## Project layout

- `backend/` — Spring Boot API, WebSocket, OSHI, LibreHardwareMonitor integration.
- `frontend/` — React app (Vite), charts, WebSocket client.
- `.github/workflows/ci.yml` — CI (backend `mvn verify`, frontend lint/test/build).
