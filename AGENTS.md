# Repository Guidelines

## Project Status
- This repository is in a rewrite-style refactor stage.
- Current architecture and rules are defined in `docs/`.
- Do not restore old module structure or old dependencies by default.
- Do not introduce new architecture ideas unless explicitly approved.

## Project Structure & Modules
- Root is a Maven multi-module Spring Boot backend (`pom.xml`, Java 21).
- Current active modules:
  - `application-starter/`: startup and runtime assembly only.
  - `chat-domain/`: core business domain module.
  - `infrastructure-basic/`: fixed global infrastructure.
  - `infrastructure-service/`: pluggable external service infrastructure parent module.
  - `distribution/`: reserved for later packaging, not part of current core work.

## Module Responsibilities
- `application-starter`
  - Starts Spring Boot.
  - Assembles runtime beans and configuration.
  - Must not carry core business rules.
- `chat-domain`
  - Holds core business logic.
  - Uses clean-architecture style package boundaries.
  - Spring Boot and Lombok are treated as internal capabilities and may be used reasonably.
- `infrastructure-basic`
  - Holds fixed infrastructure such as logging, JSON, ID, time, configuration support, and infrastructure exceptions.
  - Must not hold replaceable external service implementations.
- `infrastructure-service`
  - Holds replaceable external service infrastructure.
  - Must be split by service into `{service-name}-api` and `{service-name}-impl`.

## Dependency Rules
- Allowed:
  - `application-starter` -> `chat-domain`
  - `application-starter` -> `infrastructure-basic`
  - `application-starter` -> `infrastructure-service/*-api`
  - `application-starter` -> `infrastructure-service/*-impl`
  - `chat-domain` -> `infrastructure-basic`
  - `chat-domain` -> `infrastructure-service/*-api`
  - `*-impl` -> corresponding `*-api`
  - `*-impl` -> `infrastructure-basic`
- Forbidden:
  - `chat-domain` -> any `*-impl`
  - `chat-domain` -> `application-starter`
  - `infrastructure-basic` -> `chat-domain`
  - `infrastructure-basic` -> any `*-impl`
  - `*-api` -> `chat-domain`
  - `*-api` -> `application-starter`
  - `*-api` -> any other `*-impl`
  - `*-impl` -> `chat-domain`

## Package Structure Rules
- Base packages:
  - `team.carrypigeon.backend.starter`
  - `team.carrypigeon.backend.chat.domain`
  - `team.carrypigeon.backend.infrastructure.basic`
  - `team.carrypigeon.backend.infrastructure.service`
- `chat-domain` must be organized by `features` first, then by layer.
- Keep `shared`.
- Keep feature-level `config`.
- Keep `repository` in the domain layer as business-semantic abstraction.
- Database reads and writes must be implemented through `infrastructure-service`, not directly in `chat-domain`.

## Build, Test & Run
- Build all modules:
  - `mvn clean install`
- Run all tests:
  - `mvn test -DskipTests=false`
- Run the application locally:
  - `mvn -pl application-starter -am spring-boot:run`
- Note:
  - Repository-local Maven config is enabled through `.mvn/maven.config`.
  - Local repo currently points to `/tmp/carrypigeon-m2/repository` to avoid readonly mount issues in this environment.

## Dependency Policy
- Current baseline dependencies are kept minimal and introduced gradually.
- Do not add new dependencies unless there is a concrete current need and a clear module owner.
- Spring Boot and Lombok are the baseline.
- Hutool is already approved for Snowflake ID generation.
- Replaceable external service dependencies must go to `infrastructure-service/*-impl`, not `infrastructure-basic`.

## Configuration Rules
- Runtime config entry is `application-starter/src/main/resources/application.yaml`.
- Keep configuration minimal.
- Do not add placeholder config for future features.
- Project custom config prefix is `cp`.
- Current fixed infra config already includes Snowflake ID settings under `cp.infrastructure.id.*`.

## Logging & Infrastructure
- Logging is unified on Log4j2.
- Default log output directory is `./service-logs`.
- It can be overridden by:
  - JVM property: `-Dcp.log.home=...`
  - Env var: `CP_LOG_HOME=...`
- Fixed infrastructure facade is `InfrastructureBasics`.

## External Services & Docker
- Docker currently provides only external services, not the application container itself.
- Current compose services:
  - MySQL
  - Redis
  - MinIO
- Docker files:
  - `docker-compose.yaml`
  - `.env.example`
- Do not containerize the application by default unless explicitly requested.

## Coding Style & Naming
- Language: Java 21.
- Use 4-space indentation, Unix line endings, and UTF-8.
- Prefer constructor-based initialization.
- Keep naming explicit and stable.
- Do not create vague utility buckets.

## Comment Rules
- Follow `docs/注释规范.md`.
- Important classes and boundary methods should explain responsibility, boundary, inputs, outputs, or constraints.
- Test classes and test methods also require comments describing what contract is being validated.

## Testing Rules
- Framework: JUnit 5 and Spring Boot test starter.
- Tests live under each module's `src/test/java`.
- Test class naming: `<Name>Tests`
- Test method naming: `methodName_condition_expected()`
- Business tests should cover both success and failure paths.
- Response-code related tests should cover `CPResponse.code` values `100`, `200`, `300`, `404`, and `500` where applicable.

## AI Collaboration Rules
- AI must follow the rules in:
  - `docs/AI协作开发规范.md`
  - `docs/变更审核清单.md`
  - `docs/任务单模板.md`
- AI temporary materials must go to:
  - `ai-agent-workplace/`
- Use `ai-agent-workplace/` for:
  - task drafts
  - analysis notes
  - comparison drafts
  - intermediate generated artifacts
- Do not scatter temporary AI files into the repository root or source directories.
- Final rules belong in `docs/`.
- Final code belongs in the proper module source directories.
- Before substantial implementation, AI must first form a task sheet under `ai-agent-workplace/` or explicitly align with one already confirmed by the user.
- A task sheet must at minimum record:
  - task goal
  - affected modules
  - allowed modification scope
  - forbidden boundaries
  - governing docs
  - acceptance criteria
- For tasks involving module structure, dependency changes, new external-service integrations, configuration expansion, or architecture-sensitive refactors, AI must obtain explicit user confirmation before coding.
- AI must keep implementation within the confirmed task sheet boundary. If the actual impact expands, update the task sheet first and regain confirmation when needed.
- After implementation, AI must self-check against `docs/变更审核清单.md` and clearly state:
  - what changed
  - why it changed
  - affected modules/files
  - whether tests were added and run
  - whether docs were added or updated
  - unresolved risks or unfinished items
- If no new long-term project rule is introduced, do not modify `docs/` just to repeat task-local decisions.
- AI workflow artifacts intended for traceability should use stable text files in `ai-agent-workplace/` and should not be left only in chat output.
- Task sheet filenames in `ai-agent-workplace/` must use:
  - `{{time}}-{{author}}-{{task}}-{{state}}.md`
- Naming fields:
  - `time`: timestamp like `20260420-132451`
  - `author`: stable author identifier such as `ai` or agreed human/agent name
  - `task`: short kebab-case task identifier
  - `state`: enum-like marker used to show task status
- Allowed `state` values:
  - `current`
  - `done`
- While a task is active, use `current`; after completion, rename the file to `done`.

## Review & Change Control
- Any architectural change must be approved first.
- Any long-term rule change must be written back to `docs/`.
- Do not treat “it compiles” as sufficient acceptance.
- Review against architecture, dependency, configuration, exception, comment, and test rules before considering work complete.

## Recommended Reference Docs
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/注释规范.md`
- `docs/基建文档.md`
- `docs/配置规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/Docker配置.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
