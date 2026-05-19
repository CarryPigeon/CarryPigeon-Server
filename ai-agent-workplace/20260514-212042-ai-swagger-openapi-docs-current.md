# Swagger/OpenAPI Documentation Refinement Task Sheet

## Task Goal
Refine Swagger/OpenAPI documentation across every HTTP controller in this repository so each endpoint has accurate tags, operation metadata, parameter and body docs, response semantics, auth behavior, and DTO schema depth.

## Affected Modules
- `chat-domain` (all HTTP controllers and controller DTOs)
- `application-starter` only if OpenAPI config or grouping requires alignment
- `docs/` for API documentation sync only

## Allowed Modification Scope
- Controller-level `@Tag` metadata
- Endpoint-level `@Operation` summaries/descriptions
- `@Parameter`, `@Schema`, `@RequestBody`, `@ApiResponse`, and related OpenAPI annotations
- Controller DTO schema annotations and field descriptions
- Auth and permission documentation at endpoint level
- API documentation files that mirror published behavior
- Verification assets needed to prove the docs are complete

## Forbidden Boundaries
- No business-logic changes
- No controller route changes unless required for documentation correctness
- No module boundary or dependency changes
- No new OpenAPI architecture or helper layer unless already present and clearly necessary
- No unrelated refactors or cosmetic changes outside controller documentation surfaces

## Governing Docs
- `AGENTS.md`
- `README.md`
- `docs/API.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## Current Controller Surface
- `chat-domain/.../auth/controller/http/AuthController.java`
- `chat-domain/.../channel/controller/http/ChannelController.java`
- `chat-domain/.../message/controller/http/ChannelMessageController.java`
- `chat-domain/.../server/controller/http/ServerController.java`
- `chat-domain/.../server/controller/http/ServerWellKnownController.java`
- `chat-domain/.../user/controller/http/UserProfileController.java`

## Execution Plan
1. Audit every controller endpoint and group them by module/feature so docs stay consistent inside each bounded context.
2. Add or tighten controller-level tags so each feature group is named clearly and matches the published API grouping.
3. Enrich every endpoint with precise `summary` and `description` text that states purpose, actor, and protocol constraints.
4. Document path/query/header/body parameters with explicit names, requiredness, validation constraints, and auth ownership rules where relevant.
5. Add request-body schema docs for every mutating endpoint, including multipart upload semantics and field-level constraints.
6. Add endpoint-specific success and failure response docs, including success payload shape, auth failures, validation failures, forbidden access, and domain-specific failure modes.
7. Make auth semantics explicit on protected endpoints, including which routes are anonymous, which require Bearer auth, and which are current-account-only.
8. Deepen DTO schemas with field descriptions, examples where useful, and nested object/collection detail for the published API surface.
9. Sync `docs/API.md` and any OpenAPI-facing documentation to match the finalized controller annotations and response semantics.
10. Run verification on changed controllers and DTOs, then confirm the generated OpenAPI output reflects the intended grouping and response coverage.

## Module / Feature Breakdown

### `auth`
- Document `register`, `login`, `refresh`, `logout`, and `me` as a single authentication/session surface.
- Mark anonymous vs authenticated usage explicitly, especially `me`.
- Describe token lifecycle behavior, response fields, and failure cases for invalid credentials, expired refresh tokens, and unauthenticated access.
- Add request-body docs for credential and token payloads.

### `channel`
- Group default/system channel reads, private channel creation, invites, membership management, ownership transfer, mute/unmute, kick, and ban flows under one channel governance tag.
- Add parameter docs for all channel and target-account path variables.
- Document ownership/member-role expectations and forbidden operations for non-owners/non-admins.
- Describe success payloads for channel, invite, member, ban, and ownership transfer responses.

### `message`
- Document history, search, attachment upload, and recall as the message surface.
- Add query docs for cursor, limit, keyword, and messageType semantics.
- Document multipart upload requirements and attachment filename/content-type behavior.
- Add failure responses for invalid uploads, read errors, validation errors, and unauthorized access.

### `server`
- Document echo and presence endpoints as infrastructure/support surfaces.
- Mark `echo` as a minimal connectivity check and `presence/me` as authenticated.
- Describe anonymous access for `/.well-known/carrypigeon-server` separately from `/api/server` routes.

### `user`
- Document current-user profile reads, account-id reads, list/page/search, and profile updates as a single user-profile surface.
- Make current-account-only restrictions explicit on `/{accountId}` and `/me`-scoped operations.
- Document pagination, search keyword, and update-body semantics.
- Add schema depth for profile list/page responses and update requests.

## Verification Criteria
- Every controller method has a tag, summary, and description.
- Every path/query/body parameter has explicit docs where behavior is non-trivial.
- Every mutating endpoint documents its request body and response shape.
- Protected routes clearly indicate auth requirements and permission constraints.
- DTOs exposed by controllers carry field-level schema docs deep enough for Swagger UI.
- `docs/API.md` matches the final controller contract.
- OpenAPI generation and any available tests/build checks pass after the documentation changes.

## Completion Rule
Rename this task sheet from `current` to `done` only after the docs are updated, verification is complete, and the final controller/OpenAPI surface matches the documented contract.
