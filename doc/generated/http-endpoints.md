# HTTP Endpoints (Generated)

> Auto-generated from controller code. Do not edit manually.

Total: 37

| Method | Path | Auth | Chain | Handler | Request Body |
|---|---|---|---|---|---|
| `POST` | `/api/auth/email_codes` | `public` | `api_auth_email_codes` | `ApiAuthController#emailCodes` | `SendEmailCodeRequest` |
| `POST` | `/api/auth/refresh` | `public` | `api_auth_refresh` | `ApiAuthController#refresh` | `RefreshRequest` |
| `POST` | `/api/auth/revoke` | `public` | `api_auth_revoke` | `ApiAuthController#revoke` | `RevokeRequest` |
| `POST` | `/api/auth/tokens` | `public` | `api_auth_tokens` | `ApiAuthController#tokens` | `TokenRequest` |
| `GET` | `/api/channels` | `required` | `api_channels_list` | `ApiChannelMessageController#channels` | `-` |
| `POST` | `/api/channels` | `required` | `api_channels_create` | `ApiChannelController#create` | `ChannelCreateRequest` |
| `DELETE` | `/api/channels/{cid}` | `required` | `api_channels_delete` | `ApiChannelController#delete` | `-` |
| `GET` | `/api/channels/{cid}` | `required` | `api_channels_get` | `ApiChannelController#get` | `-` |
| `PATCH` | `/api/channels/{cid}` | `required` | `api_channels_patch` | `ApiChannelController#patch` | `ChannelPatchRequest` |
| `DELETE` | `/api/channels/{cid}/admins/{uid}` | `required` | `api_channel_admins_delete` | `ApiChannelMemberController#adminDelete` | `-` |
| `PUT` | `/api/channels/{cid}/admins/{uid}` | `required` | `api_channel_admins_put` | `ApiChannelMemberController#adminPut` | `-` |
| `GET` | `/api/channels/{cid}/applications` | `required` | `api_channel_applications_list` | `ApiChannelModerationController#listApplications` | `-` |
| `POST` | `/api/channels/{cid}/applications` | `required` | `api_channel_applications_create` | `ApiChannelModerationController#createApplication` | `ChannelApplicationCreateRequest` |
| `POST` | `/api/channels/{cid}/applications/{application_id}/decisions` | `required` | `api_channel_application_decision` | `ApiChannelModerationController#decide` | `ChannelApplicationDecisionRequest` |
| `GET` | `/api/channels/{cid}/bans` | `required` | `api_channel_bans_list` | `ApiChannelModerationController#bans` | `-` |
| `DELETE` | `/api/channels/{cid}/bans/{uid}` | `required` | `api_channel_bans_delete` | `ApiChannelModerationController#banDelete` | `-` |
| `PUT` | `/api/channels/{cid}/bans/{uid}` | `required` | `api_channel_bans_put` | `ApiChannelModerationController#banPut` | `ChannelBanUpsertRequest` |
| `GET` | `/api/channels/{cid}/members` | `required` | `api_channel_members_list` | `ApiChannelMemberController#list` | `-` |
| `DELETE` | `/api/channels/{cid}/members/{uid}` | `required` | `api_channel_members_kick` | `ApiChannelMemberController#kick` | `-` |
| `GET` | `/api/channels/{cid}/messages` | `required` | `api_messages_list` | `ApiChannelMessageController#messages` | `-` |
| `POST` | `/api/channels/{cid}/messages` | `required` | `api_messages_create` | `ApiChannelMessageController#create` | `CreateMessageBody` |
| `PUT` | `/api/channels/{cid}/read_state` | `required` | `api_read_state_update` | `ApiChannelMessageController#updateReadState` | `ReadStateBody` |
| `GET` | `/api/contracts/{plugin_id}/{domain}/{domain_version}` | `public` | `-` | `ApiPluginRepositoryController#contract` | `-` |
| `GET` | `/api/domains/catalog` | `public` | `api_domains_catalog` | `ApiServerController#domainsCatalog` | `-` |
| `GET` | `/api/files/download/{share_key}` | `public` | `-` | `ApiFileController#download` | `-` |
| `PUT` | `/api/files/upload/{file_id}` | `required` | `-` | `ApiFileController#upload` | `-` |
| `POST` | `/api/files/uploads` | `required` | `api_files_uploads_create` | `ApiFileController#applyUpload` | `FileUploadApplyRequest` |
| `POST` | `/api/gates/required/check` | `public` | `api_required_gate_check` | `ApiAuthController#requiredGateCheck` | `RequiredGateCheckRequest` |
| `DELETE` | `/api/messages/{mid}` | `required` | `api_messages_delete` | `ApiChannelMessageController#delete` | `-` |
| `GET` | `/api/plugins/catalog` | `public` | `api_plugins_catalog` | `ApiServerController#pluginsCatalog` | `-` |
| `GET` | `/api/plugins/download/{plugin_id}/{version}` | `public` | `-` | `ApiPluginRepositoryController#download` | `-` |
| `GET` | `/api/server` | `public` | `api_server` | `ApiServerController#server` | `-` |
| `GET` | `/api/unreads` | `required` | `api_unreads_list` | `ApiChannelMessageController#unreads` | `-` |
| `GET` | `/api/users` | `required` | `api_users_batch` | `ApiUserController#batch` | `-` |
| `GET` | `/api/users/me` | `required` | `api_users_me` | `ApiUserController#me` | `-` |
| `PATCH` | `/api/users/me` | `required` | `api_users_me_patch` | `ApiUserController#patchMe` | `UserMePatchRequest` |
| `GET` | `/api/users/{uid}` | `required` | `api_users_get` | `ApiUserController#get` | `-` |
