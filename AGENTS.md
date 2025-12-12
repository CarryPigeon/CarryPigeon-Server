# Repository Guidelines

## Project Structure & Modules
- Root is a Maven multi-module Spring Boot backend (`pom.xml`, Java 21).
- Core modules:
  - `api/`: protocol types (`CPPacket`, `CPResponse`) and public routes (see `doc/api.md`).
  - `chat-domain/`: Netty controllers and business logic.
  - `dao/`: persistence layer and database access.
  - `common/`: shared utilities and base models.
  - `connection/`: TCP/Netty transport, encryption, heartbeats.
  - `external-service/`: integrations (e.g., email).
  - `application-starter/`: runnable Spring Boot app, main class `team.carrypigeon.backend.starter.ApplicationStarter`.
  - `distribution/`: packaging / distribution artifacts.
- Tests live under `*/src/test/**`.

## Build, Test & Run
- Build all modules:
  - `mvn clean install`
- Run the application locally:
  - `mvn -pl application-starter -am spring-boot:run`
- Run tests (skipped by default in the root POM):
  - `mvn test -DskipTests=false`

## Coding Style & Naming
- Language: Java 21, Spring Boot 3.5.x, Lombok, Log4j2.
- Use 4-space indentation, Unix line endings, and UTF-8 encoding.
- Package pattern: `team.carrypigeon.backend.<module>[.<feature>]`.
- Follow existing naming:
  - Protocol/VO/result: `CP*VO`, `CP*Result`, `CP*ResultItem`.
  - Controllers annotated with `@CPControllerTag(path = "/core/...")`.
- Prefer constructor or builder-based initialization over public field mutation.

## Testing Guidelines
- Framework: JUnit 5 (`org.junit.jupiter.api`), Spring Boot test starter.
- Co-locate tests next to the module under `src/test/java`.
- Name test classes `<Name>Tests` and methods `methodName_condition_expected()`.
- For business logic, test both success and error codes (`CPResponse.code` 100/200/300/404/500).

## Commit & Pull Request Guidelines
- Use Conventional Commit style; the repo primarily uses `feat: ...`.
  - Examples: `feat: add channel ban list API`, `fix: handle invalid token login`.
- Keep commits focused and buildable.
- PRs should include:
  - Clear description, motivation, and scope.
  - Links to related issues or docs (e.g., sections in `doc/api.md`).
  - Notes on config changes (ports, security settings) and how you tested.

## Security & Configuration
- Sensitive settings (ports, keys, email configs) should come from Spring Boot configuration, not hard-coded.
- When adding routes or connection features, verify they respect the ECC + AES handshake and `CPResponse` error conventions described in `doc/api.md`.

