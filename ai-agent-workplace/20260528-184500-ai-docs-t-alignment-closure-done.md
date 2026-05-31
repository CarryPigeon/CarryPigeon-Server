任务名称：
docs-t API 对齐最终收口记录

任务目标：
记录本轮 `docs/t` API 协议整改的最终完成状态、覆盖批次、验证证据与边界说明。

任务背景：
本仓库在 rewrite 阶段存在大量与 `docs/t` 协议面不一致的 HTTP / WebSocket API。经过多批次实现与验证后，需要一份最终收口记录，明确这轮 backlog 已完成到什么程度，以及哪些遗留 `current` 任务单不属于本轮范围。

覆盖范围：
- `docs/t/SERVER_API.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

已完成批次：
- batch9：message forward
- batch10：mentions inbox / mentions read
- batch11：message edit / message model / `message.updated`
- batch12：base contract cleanup（ban path、kick 204、user background upload）
- batch13：notification preferences
- batch14：remote discover
- batch15：audit logs

此前已完成能力（本轮前置）：
- files
- read_state / unreads
- channel applications
- bans list
- pins

关键验证命令（已通过）：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=MessageApplicationServiceSendTests,MessageApplicationServiceForwardTests,ChannelMessageQueryControllerTests,MessageForwardControllerTests,NettyMessageRealtimePublisherTests,DatabaseBackedMessageRepositoryTests,MybatisPlusMessageDatabaseServiceTests,ApplicationStarterSmokeTests -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl chat-domain -am -Dtest=ChannelControllerTests,ChannelBansControllerTests,UserProfileControllerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=NotificationPreferenceApplicationServiceTests,NotificationPreferenceControllerTests,ChannelControllerTests,NotificationPreferenceDatabaseServiceContractTests,MybatisPlusNotificationPreferenceDatabaseServiceTests,DatabaseServiceAutoConfigurationTests,ApplicationStarterSmokeTests -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl -am -Dtest=ChannelDiscoverApplicationServiceTests,ChannelDiscoverControllerTests,ChannelControllerTests,DatabaseServiceAutoConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl -am -Dtest=AuditLogApplicationServiceTests,AuditLogControllerTests,DatabaseBackedChannelAuditLogRepositoryTests,MybatisPlusChannelAuditLogDatabaseServiceTests,DatabaseServiceAutoConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false test`

Oracle 最终复核结论：
- `PASS`
- 结论边界：基于本轮明确识别出的 `docs/t` backlog 范围，未发现新的显式协议级缺口。

当前状态：
- `ai-agent-workplace/` 中已无 `*docs-t*-current.md`
- 本轮 docs/t backlog 已全部归档为 `done`

非本轮范围的残留 `current`：
- `20260514-212042-ai-swagger-openapi-docs-current.md`
- `20260514-215417-ai-controller-docs-refinement-current.md`

说明：
- 上述两个 `current` 为更早的 Swagger / OpenAPI 文档规划任务，不属于本轮 `docs/t` 协议整改 backlog 的未完成项。

残留风险：
- 若未来再发现最初 backlog 盘点遗漏的协议 family，需要作为新问题重新开批次，而不是视为本轮未收口。
- 当前结论仅针对 `docs/t` backlog 对齐，不等于仓库内所有历史文档任务均已完成。

完成定义：
- 本轮 docs/t backlog 已完成实现、验证、归档与最终复核。
