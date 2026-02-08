-- CarryPigeon Backend - MySQL 测试数据（可重复执行）
-- 依赖表结构参考：doc/domain/database-schema.md
--
-- 用法示例：
--   mysql -uroot -pcarrypigeon carrypigeon < doc/sql/carrypigeon_test_data_mysql.sql
--
-- 注意：仅用于测试库。本脚本会清理并重建下列 id 区间的数据：
--   file_info          710000000000000001 ~ 710000000000000010
--   user               720000000000000001 ~ 720000000000000008
--   channel            730000000000000001 ~ 730000000000000004
--   channel_member     740000000000000001 ~ 740000000000000050
--   channel_application 750000000000000001 ~ 750000000000000010
--   channel_ban        760000000000000001 ~ 760000000000000010
--   message            770000000000000001 ~ 770000000000000082
--   channel_read_state 780000000000000001 ~ 780000000000000050
--   user_token         790000000000000001 ~ 790000000000000020
--
-- 预置 token（可用于 /core/user/login/token）：
--   admin  : cp_test_token_admin
--   alice  : cp_test_token_alice
--   bob    : cp_test_token_bob
--   chen   : cp_test_token_chen
--   eve    : cp_test_token_eve
--   newbie : cp_test_token_newbie
--   guest  : cp_test_token_guest
--   bot    : cp_test_token_bot

SET NAMES utf8mb4;
SET @seed_now := NOW();
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- ----------------------------
-- 0) 常量定义（id、时间基准）
-- ----------------------------
SET @fid_base := 710000000000000000;
SET @uid_base := 720000000000000000;
SET @cid_base := 730000000000000000;
SET @cmid_base := 740000000000000000;
SET @appid_base := 750000000000000000;
SET @banid_base := 760000000000000000;
SET @mid_base := 770000000000000000;
SET @rsid_base := 780000000000000000;
SET @tid_base := 790000000000000000;

SET @uid_admin := @uid_base + 1;
SET @uid_alice := @uid_base + 2;
SET @uid_bob := @uid_base + 3;
SET @uid_chen := @uid_base + 4;
SET @uid_eve := @uid_base + 5;
SET @uid_newbie := @uid_base + 6;
SET @uid_guest := @uid_base + 7;
SET @uid_bot := @uid_base + 8;

SET @cid_announcement := @cid_base + 1; -- 固有频道（owner = -1）
SET @cid_dev := @cid_base + 2;
SET @cid_feedback := @cid_base + 3;
SET @cid_meeting := @cid_base + 4;

SET @t_dev_start := DATE_SUB(@seed_now, INTERVAL 12 HOUR);
SET @t_fb_start := DATE_SUB(@seed_now, INTERVAL 3 DAY);
SET @t_ann_start := DATE_SUB(@seed_now, INTERVAL 14 DAY);
SET @t_meet_start := DATE_SUB(@seed_now, INTERVAL 2 DAY);

-- ----------------------------
-- 1) 清理旧测试数据（按外键依赖顺序）
-- ----------------------------
DELETE FROM `message`
WHERE `id` BETWEEN 770000000000000001 AND 770000000000000082;

DELETE FROM `channel_ban`
WHERE `id` BETWEEN 760000000000000001 AND 760000000000000010;

DELETE FROM `channel_application`
WHERE `id` BETWEEN 750000000000000001 AND 750000000000000010;

DELETE FROM `channel_read_state`
WHERE `id` BETWEEN 780000000000000001 AND 780000000000000050;

DELETE FROM `channel_member`
WHERE `id` BETWEEN 740000000000000001 AND 740000000000000050;

DELETE FROM `user_token`
WHERE `id` BETWEEN 790000000000000001 AND 790000000000000020;

DELETE FROM `channel`
WHERE `id` BETWEEN 730000000000000001 AND 730000000000000004;

DELETE FROM `user`
WHERE `id` BETWEEN 720000000000000001 AND 720000000000000008;

DELETE FROM `file_info`
WHERE `id` BETWEEN 710000000000000001 AND 710000000000000010;

-- ----------------------------
-- 2) file_info（头像/频道头像/示例文件）
-- object_name 约定为 file_{fileId}（与 HTTP Files 模块一致）
-- ----------------------------
INSERT INTO `file_info`
(`id`, `share_key`, `owner_uid`, `access_scope`, `scope_cid`, `scope_mid`, `filename`, `sha256`, `size`, `object_name`, `content_type`, `uploaded`, `uploaded_time`, `create_time`) VALUES
    (@fid_base + 1,  CONCAT('shr_', @fid_base + 1),  @uid_admin,  'AUTH', 0, 0, 'avatar_admin.png', REPEAT('1', 64), 12543,  CONCAT('file_', @fid_base + 1),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 180 DAY), DATE_SUB(@seed_now, INTERVAL 180 DAY)),
    (@fid_base + 2,  CONCAT('shr_', @fid_base + 2),  @uid_alice,  'AUTH', 0, 0, 'avatar_alice.png', REPEAT('2', 64), 16420,  CONCAT('file_', @fid_base + 2),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 180 DAY), DATE_SUB(@seed_now, INTERVAL 180 DAY)),
    (@fid_base + 3,  CONCAT('shr_', @fid_base + 3),  @uid_bob,    'AUTH', 0, 0, 'avatar_bob.png',   REPEAT('3', 64), 13210,  CONCAT('file_', @fid_base + 3),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 180 DAY), DATE_SUB(@seed_now, INTERVAL 180 DAY)),
    (@fid_base + 4,  CONCAT('shr_', @fid_base + 4),  @uid_chen,   'AUTH', 0, 0, 'avatar_chen.png',  REPEAT('4', 64), 14210,  CONCAT('file_', @fid_base + 4),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 180 DAY), DATE_SUB(@seed_now, INTERVAL 180 DAY)),
    (@fid_base + 5,  CONCAT('shr_', @fid_base + 5),  @uid_eve,    'AUTH', 0, 0, 'avatar_eve.png',   REPEAT('5', 64), 15310,  CONCAT('file_', @fid_base + 5),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 180 DAY), DATE_SUB(@seed_now, INTERVAL 180 DAY)),
    (@fid_base + 6,  CONCAT('shr_', @fid_base + 6),  @uid_admin,  'AUTH', 0, 0, 'channel_dev.png',  REPEAT('6', 64), 10420,  CONCAT('file_', @fid_base + 6),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 180 DAY), DATE_SUB(@seed_now, INTERVAL 180 DAY)),
    (@fid_base + 7,  CONCAT('shr_', @fid_base + 7),  @uid_admin,  'OWNER', 0, 0, 'misc_01.png',      REPEAT('7', 64), 17420,  CONCAT('file_', @fid_base + 7),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 90 DAY),  DATE_SUB(@seed_now, INTERVAL 90 DAY)),
    (@fid_base + 8,  CONCAT('shr_', @fid_base + 8),  @uid_admin,  'OWNER', 0, 0, 'misc_02.png',      REPEAT('8', 64), 18420,  CONCAT('file_', @fid_base + 8),  'image/png', 1, DATE_SUB(@seed_now, INTERVAL 60 DAY),  DATE_SUB(@seed_now, INTERVAL 60 DAY)),
    (@fid_base + 9,  CONCAT('shr_', @fid_base + 9),  @uid_admin,  'OWNER', 0, 0, 'misc_doc.pdf',     REPEAT('9', 64), 524288, CONCAT('file_', @fid_base + 9),  'application/pdf', 1, DATE_SUB(@seed_now, INTERVAL 30 DAY),  DATE_SUB(@seed_now, INTERVAL 30 DAY)),
    (@fid_base + 10, CONCAT('shr_', @fid_base + 10), @uid_admin,  'OWNER', 0, 0, 'misc_big.jpg',     REPEAT('a', 64), 4194304, CONCAT('file_', @fid_base + 10), 'image/jpeg', 1, DATE_SUB(@seed_now, INTERVAL 10 DAY),  DATE_SUB(@seed_now, INTERVAL 10 DAY));

-- ----------------------------
-- 3) user（8 个典型用户：管理员/频道主/普通成员/被禁言/新用户/游客/机器人）
-- avatar 字段为 long，不可为 NULL（0 表示无头像）
-- ----------------------------
INSERT INTO `user`
(`id`, `username`, `avatar`, `email`, `sex`, `brief`, `birthday`, `register_time`) VALUES
    (@uid_admin, 'admin', @fid_base + 1, 'admin@carrypigeon.test', 0, '系统管理员（测试数据）',
        DATE_SUB(@seed_now, INTERVAL 35 YEAR), DATE_SUB(@seed_now, INTERVAL 365 DAY)),
    (@uid_alice, 'alice', @fid_base + 2, 'alice@carrypigeon.test', 2, '频道主：负责开发讨论',
        DATE_SUB(@seed_now, INTERVAL 28 YEAR), DATE_SUB(@seed_now, INTERVAL 200 DAY)),
    (@uid_bob, 'bob', @fid_base + 3, 'bob@carrypigeon.test', 1, '后端同学，常驻开发讨论',
        DATE_SUB(@seed_now, INTERVAL 30 YEAR), DATE_SUB(@seed_now, INTERVAL 120 DAY)),
    (@uid_chen, '陈小龙', @fid_base + 4, 'chen@carrypigeon.test', 1, '前端同学，测试中文用户名',
        DATE_SUB(@seed_now, INTERVAL 26 YEAR), DATE_SUB(@seed_now, INTERVAL 110 DAY)),
    (@uid_eve, 'eve', @fid_base + 5, 'eve@carrypigeon.test', 0, '被禁言用户（用于测试 ban 逻辑）',
        DATE_SUB(@seed_now, INTERVAL 24 YEAR), DATE_SUB(@seed_now, INTERVAL 90 DAY)),
    (@uid_newbie, 'newbie', 0, 'newbie@carrypigeon.test', 0, '新注册用户（有待处理的入群申请）',
        DATE_SUB(@seed_now, INTERVAL 20 YEAR), DATE_SUB(@seed_now, INTERVAL 3 DAY)),
    (@uid_guest, 'guest', 0, 'guest@carrypigeon.test', 0, '游客账号（有历史申请/过期禁言）',
        DATE_SUB(@seed_now, INTERVAL 22 YEAR), DATE_SUB(@seed_now, INTERVAL 10 DAY)),
    (@uid_bot, 'cp-bot', 0, 'bot@carrypigeon.test', 0, '机器人账号（申请被拒绝示例）',
        DATE_SUB(@seed_now, INTERVAL 2 YEAR), DATE_SUB(@seed_now, INTERVAL 15 DAY));

-- ----------------------------
-- 4) channel（1 个固有频道 + 3 个普通频道）
-- 固有频道：owner = -1，会被 /core/channel/list 默认返回
-- ----------------------------
INSERT INTO `channel`
(`id`, `name`, `owner`, `brief`, `avatar`, `create_time`) VALUES
    (@cid_announcement, '公告', -1, '系统公告频道（固有频道）', @fid_base + 6, DATE_SUB(@seed_now, INTERVAL 365 DAY)),
    (@cid_dev, '开发讨论', @uid_alice, '研发同学日常交流', @fid_base + 7, DATE_SUB(@seed_now, INTERVAL 30 DAY)),
    (@cid_feedback, '产品反馈', @uid_admin, '收集产品反馈与 BUG', @fid_base + 8, DATE_SUB(@seed_now, INTERVAL 15 DAY)),
    (@cid_meeting, '临时会议', @uid_bob, '临时会议记录（小群）', 0, DATE_SUB(@seed_now, INTERVAL 2 DAY));

-- ----------------------------
-- 5) channel_member（成员关系、管理员角色）
-- authority: 0=成员，1=管理员
-- ----------------------------
INSERT INTO `channel_member`
(`id`, `uid`, `cid`, `name`, `authority`, `join_time`) VALUES
    -- 公告频道：所有用户都加入（便于测试固有频道消息/读状态）
    (@cmid_base + 1,  @uid_admin,  @cid_announcement, 'admin', 1, DATE_SUB(@seed_now, INTERVAL 200 DAY)),
    (@cmid_base + 2,  @uid_alice,  @cid_announcement, 'alice', 0, DATE_SUB(@seed_now, INTERVAL 180 DAY)),
    (@cmid_base + 3,  @uid_bob,    @cid_announcement, 'bob',   0, DATE_SUB(@seed_now, INTERVAL 170 DAY)),
    (@cmid_base + 4,  @uid_chen,   @cid_announcement, '陈小龙', 0, DATE_SUB(@seed_now, INTERVAL 160 DAY)),
    (@cmid_base + 5,  @uid_eve,    @cid_announcement, 'eve',   0, DATE_SUB(@seed_now, INTERVAL 150 DAY)),
    (@cmid_base + 6,  @uid_newbie, @cid_announcement, 'newbie',0, DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    (@cmid_base + 7,  @uid_guest,  @cid_announcement, 'guest', 0, DATE_SUB(@seed_now, INTERVAL 9 DAY)),
    (@cmid_base + 8,  @uid_bot,    @cid_announcement, 'cp-bot',0, DATE_SUB(@seed_now, INTERVAL 14 DAY)),

    -- 开发讨论：频道主 alice + 管理员 admin + 普通成员
    (@cmid_base + 9,  @uid_alice,  @cid_dev, 'alice', 1, DATE_SUB(@seed_now, INTERVAL 29 DAY)),
    (@cmid_base + 10, @uid_admin,  @cid_dev, 'admin', 1, DATE_SUB(@seed_now, INTERVAL 28 DAY)),
    (@cmid_base + 11, @uid_bob,    @cid_dev, 'bob',   0, DATE_SUB(@seed_now, INTERVAL 25 DAY)),
    (@cmid_base + 12, @uid_chen,   @cid_dev, '陈小龙', 0, DATE_SUB(@seed_now, INTERVAL 20 DAY)),
    (@cmid_base + 13, @uid_eve,    @cid_dev, 'eve',   0, DATE_SUB(@seed_now, INTERVAL 18 DAY)),
    (@cmid_base + 14, @uid_guest,  @cid_dev, 'guest', 0, DATE_SUB(@seed_now, INTERVAL 7 DAY)),

    -- 产品反馈：频道主 admin（同时管理员）+ 成员
    (@cmid_base + 15, @uid_admin,  @cid_feedback, 'admin', 1, DATE_SUB(@seed_now, INTERVAL 14 DAY)),
    (@cmid_base + 16, @uid_alice,  @cid_feedback, 'alice', 0, DATE_SUB(@seed_now, INTERVAL 13 DAY)),
    (@cmid_base + 17, @uid_bob,    @cid_feedback, 'bob',   0, DATE_SUB(@seed_now, INTERVAL 12 DAY)),
    (@cmid_base + 18, @uid_chen,   @cid_feedback, '陈小龙', 0, DATE_SUB(@seed_now, INTERVAL 11 DAY)),
    (@cmid_base + 19, @uid_guest,  @cid_feedback, 'guest', 0, DATE_SUB(@seed_now, INTERVAL 10 DAY)),

    -- 临时会议：频道主 bob（管理员）+ 成员
    (@cmid_base + 20, @uid_bob,   @cid_meeting, 'bob',   1, DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    (@cmid_base + 21, @uid_alice, @cid_meeting, 'alice', 0, DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    (@cmid_base + 22, @uid_chen,  @cid_meeting, '陈小龙', 0, DATE_SUB(@seed_now, INTERVAL 2 DAY));

-- ----------------------------
-- 6) channel_application（待处理/通过/拒绝）
-- state: 0=待处理，1=通过，2=拒绝
-- ----------------------------
INSERT INTO `channel_application`
(`id`, `uid`, `cid`, `state`, `msg`, `apply_time`) VALUES
    (@appid_base + 1, @uid_newbie, @cid_dev, 0, '想加入开发讨论学习交流', DATE_SUB(@seed_now, INTERVAL 2 HOUR)),
    (@appid_base + 2, @uid_guest,  @cid_dev, 1, '历史申请（已通过）',       DATE_SUB(@seed_now, INTERVAL 7 DAY)),
    (@appid_base + 3, @uid_bot,    @cid_feedback, 2, '机器人测试申请（被拒绝）', DATE_SUB(@seed_now, INTERVAL 1 DAY));

-- ----------------------------
-- 7) channel_ban（有效禁言 + 过期禁言）
-- duration: 秒
-- create_time 使用相对 @seed_now，保证“有效/过期”在执行时成立
-- ----------------------------
INSERT INTO `channel_ban`
(`id`, `cid`, `uid`, `aid`, `duration`, `create_time`) VALUES
    (@banid_base + 1, @cid_dev,      @uid_eve,   @uid_admin, 3600, DATE_SUB(@seed_now, INTERVAL 10 MINUTE)),
    (@banid_base + 2, @cid_feedback, @uid_guest, @uid_admin, 3600, DATE_SUB(@seed_now, INTERVAL 3 DAY));

-- ----------------------------
-- 8) message（Core:Text，覆盖分页>50、近期可删除窗口、不同发送者）
-- message.data 为 JSON 字符串：{"text":"..."}
-- ----------------------------
INSERT INTO `message`
(`id`, `uid`, `cid`, `domain`, `data`, `send_time`) VALUES
    -- 开发讨论：57 条历史消息（每 10 分钟一条，跨度约 9.5 小时）
    (@mid_base + 1,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-001 alice: 项目开会对齐"}', DATE_ADD(@t_dev_start, INTERVAL 0 MINUTE)),
    (@mid_base + 2,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-002 alice: 今日目标：完善链路"}', DATE_ADD(@t_dev_start, INTERVAL 10 MINUTE)),
    (@mid_base + 3,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-003 alice: 先把接口文档补全"}', DATE_ADD(@t_dev_start, INTERVAL 20 MINUTE)),
    (@mid_base + 4,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-004 alice: 有人看下 PR 吗"}', DATE_ADD(@t_dev_start, INTERVAL 30 MINUTE)),
    (@mid_base + 5,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-005 alice: MyBatis-Plus 映射确认"}', DATE_ADD(@t_dev_start, INTERVAL 40 MINUTE)),
    (@mid_base + 6,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-006 alice: 日志级别先调到 info"}', DATE_ADD(@t_dev_start, INTERVAL 50 MINUTE)),
    (@mid_base + 7,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-007 alice: 读状态功能进入联调"}', DATE_ADD(@t_dev_start, INTERVAL 60 MINUTE)),
    (@mid_base + 8,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-008 alice: 帮忙 review 一下缓存"}', DATE_ADD(@t_dev_start, INTERVAL 70 MINUTE)),
    (@mid_base + 9,  @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-009 alice: 先合并再修小问题"}', DATE_ADD(@t_dev_start, INTERVAL 80 MINUTE)),
    (@mid_base + 10, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-010 alice: 约一下灰度"}', DATE_ADD(@t_dev_start, INTERVAL 90 MINUTE)),
    (@mid_base + 11, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-011 alice: 数据库索引要补"}', DATE_ADD(@t_dev_start, INTERVAL 100 MINUTE)),
    (@mid_base + 12, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-012 alice: 记得更新 doc"}', DATE_ADD(@t_dev_start, INTERVAL 110 MINUTE)),
    (@mid_base + 13, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-013 alice: 收到，今晚发包"}', DATE_ADD(@t_dev_start, INTERVAL 120 MINUTE)),
    (@mid_base + 14, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-014 alice: 对齐一下错误码"}', DATE_ADD(@t_dev_start, INTERVAL 130 MINUTE)),
    (@mid_base + 15, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-015 alice: 连接层心跳 ok"}', DATE_ADD(@t_dev_start, INTERVAL 140 MINUTE)),
    (@mid_base + 16, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-016 alice: LiteFlow 配置在 application-starter"}', DATE_ADD(@t_dev_start, INTERVAL 150 MINUTE)),
    (@mid_base + 17, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-017 alice: 还缺一份测试数据"}', DATE_ADD(@t_dev_start, INTERVAL 160 MINUTE)),
    (@mid_base + 18, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-018 alice: 这个可以用 SQL seed"}', DATE_ADD(@t_dev_start, INTERVAL 170 MINUTE)),
    (@mid_base + 19, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-019 alice: 消息域先只支持 Core:Text"}', DATE_ADD(@t_dev_start, INTERVAL 180 MINUTE)),
    (@mid_base + 20, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-020 alice: 收工"}', DATE_ADD(@t_dev_start, INTERVAL 190 MINUTE)),

    (@mid_base + 21, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-021 bob: 我来补测试数据"}', DATE_ADD(@t_dev_start, INTERVAL 200 MINUTE)),
    (@mid_base + 22, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-022 bob: 需要覆盖 ban/application/read_state"}', DATE_ADD(@t_dev_start, INTERVAL 210 MINUTE)),
    (@mid_base + 23, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-023 bob: 还要有>50条消息测试分页"}', DATE_ADD(@t_dev_start, INTERVAL 220 MINUTE)),
    (@mid_base + 24, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-024 bob: 最近2分钟的消息用于删除测试"}', DATE_ADD(@t_dev_start, INTERVAL 230 MINUTE)),
    (@mid_base + 25, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-025 bob: OK"}', DATE_ADD(@t_dev_start, INTERVAL 240 MINUTE)),
    (@mid_base + 26, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-026 bob: channel_member 里 owner 也要 admin"}', DATE_ADD(@t_dev_start, INTERVAL 250 MINUTE)),
    (@mid_base + 27, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-027 bob: 我看下消息删除窗口限制"}', DATE_ADD(@t_dev_start, INTERVAL 260 MINUTE)),
    (@mid_base + 28, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-028 bob: 120 秒，seed 里放最近消息"}', DATE_ADD(@t_dev_start, INTERVAL 270 MINUTE)),
    (@mid_base + 29, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-029 bob: ban 有过期自动清理"}', DATE_ADD(@t_dev_start, INTERVAL 280 MINUTE)),
    (@mid_base + 30, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-030 bob: read_state 用毫秒时间戳"}', DATE_ADD(@t_dev_start, INTERVAL 290 MINUTE)),
    (@mid_base + 31, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-031 bob: 好，我继续"}', DATE_ADD(@t_dev_start, INTERVAL 300 MINUTE)),
    (@mid_base + 32, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-032 bob: 文件模块 token 只校验 op"}', DATE_ADD(@t_dev_start, INTERVAL 310 MINUTE)),
    (@mid_base + 33, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-033 bob: file_info object_name 是 fileId"}', DATE_ADD(@t_dev_start, INTERVAL 320 MINUTE)),
    (@mid_base + 34, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-034 bob: 头像用 fileId 直接 /file/raw"}', DATE_ADD(@t_dev_start, INTERVAL 330 MINUTE)),
    (@mid_base + 35, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-035 bob: ok"}', DATE_ADD(@t_dev_start, INTERVAL 340 MINUTE)),
    (@mid_base + 36, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-036 bob: 频道列表包含固定频道"}', DATE_ADD(@t_dev_start, INTERVAL 350 MINUTE)),
    (@mid_base + 37, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-037 bob: 但是消息需要 member"}', DATE_ADD(@t_dev_start, INTERVAL 360 MINUTE)),
    (@mid_base + 38, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-038 bob: seed 里让所有人加入公告"}', DATE_ADD(@t_dev_start, INTERVAL 370 MINUTE)),
    (@mid_base + 39, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-039 bob: done"}', DATE_ADD(@t_dev_start, INTERVAL 380 MINUTE)),
    (@mid_base + 40, @uid_bob, @cid_dev, 'Core:Text', '{"text":"dev-040 bob: 收尾"}', DATE_ADD(@t_dev_start, INTERVAL 390 MINUTE)),

    (@mid_base + 41, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-041 陈小龙: 前端这边需要测试中文/emoji"}', DATE_ADD(@t_dev_start, INTERVAL 400 MINUTE)),
    (@mid_base + 42, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-042 陈小龙: 文本长度也要覆盖"}', DATE_ADD(@t_dev_start, INTERVAL 410 MINUTE)),
    (@mid_base + 43, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-043 陈小龙: 这是一条稍长的消息，用于测试列表显示与截断逻辑"}', DATE_ADD(@t_dev_start, INTERVAL 420 MINUTE)),
    (@mid_base + 44, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-044 陈小龙: OK"}', DATE_ADD(@t_dev_start, INTERVAL 430 MINUTE)),
    (@mid_base + 45, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-045 陈小龙: read_state 通知 route=/core/channel/message/read/state"}', DATE_ADD(@t_dev_start, INTERVAL 440 MINUTE)),
    (@mid_base + 46, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-046 陈小龙: 接口参数都是毫秒"}', DATE_ADD(@t_dev_start, INTERVAL 450 MINUTE)),
    (@mid_base + 47, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-047 陈小龙: 继续"}', DATE_ADD(@t_dev_start, INTERVAL 460 MINUTE)),
    (@mid_base + 48, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-048 陈小龙: 👍"}', DATE_ADD(@t_dev_start, INTERVAL 470 MINUTE)),
    (@mid_base + 49, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-049 陈小龙: 这条包含符号 !@#$%^&*()"}', DATE_ADD(@t_dev_start, INTERVAL 480 MINUTE)),
    (@mid_base + 50, @uid_chen, @cid_dev, 'Core:Text', '{"text":"dev-050 陈小龙: 收到"}', DATE_ADD(@t_dev_start, INTERVAL 490 MINUTE)),

    (@mid_base + 51, @uid_eve, @cid_dev, 'Core:Text', '{"text":"dev-051 eve: 我说两句"}', DATE_ADD(@t_dev_start, INTERVAL 500 MINUTE)),
    (@mid_base + 52, @uid_eve, @cid_dev, 'Core:Text', '{"text":"dev-052 eve: （用于测试禁言用户的历史消息）"}', DATE_ADD(@t_dev_start, INTERVAL 510 MINUTE)),
    (@mid_base + 53, @uid_eve, @cid_dev, 'Core:Text', '{"text":"dev-053 eve: ok"}', DATE_ADD(@t_dev_start, INTERVAL 520 MINUTE)),
    (@mid_base + 54, @uid_eve, @cid_dev, 'Core:Text', '{"text":"dev-054 eve: ..."}', DATE_ADD(@t_dev_start, INTERVAL 530 MINUTE)),
    (@mid_base + 55, @uid_eve, @cid_dev, 'Core:Text', '{"text":"dev-055 eve: 以后不发了"}', DATE_ADD(@t_dev_start, INTERVAL 540 MINUTE)),
    (@mid_base + 56, @uid_eve, @cid_dev, 'Core:Text', '{"text":"dev-056 eve: bye"}', DATE_ADD(@t_dev_start, INTERVAL 550 MINUTE)),
    (@mid_base + 57, @uid_eve, @cid_dev, 'Core:Text', '{"text":"dev-057 eve: （ban 创建在最近 10 分钟）"}', DATE_ADD(@t_dev_start, INTERVAL 560 MINUTE)),

    -- 开发讨论：3 条近期消息（用于 /core/channel/message/delete 120 秒窗口测试）
    (@mid_base + 58, @uid_bob,   @cid_dev, 'Core:Text', '{"text":"dev-058 bob: delete-window test (bob)"}', DATE_SUB(@seed_now, INTERVAL 90 SECOND)),
    (@mid_base + 59, @uid_alice, @cid_dev, 'Core:Text', '{"text":"dev-059 alice: delete-window test (admin)"}', DATE_SUB(@seed_now, INTERVAL 45 SECOND)),
    (@mid_base + 60, @uid_chen,  @cid_dev, 'Core:Text', '{"text":"dev-060 陈小龙: delete-window test"}', DATE_SUB(@seed_now, INTERVAL 15 SECOND)),

    -- 产品反馈：12 条消息（跨度约 33 小时）
    (@mid_base + 61, @uid_admin, @cid_feedback, 'Core:Text', '{"text":"fb-001 admin: 欢迎反馈问题"}', DATE_ADD(@t_fb_start, INTERVAL 0 HOUR)),
    (@mid_base + 62, @uid_guest, @cid_feedback, 'Core:Text', '{"text":"fb-002 guest: 列表分页有点卡"}', DATE_ADD(@t_fb_start, INTERVAL 3 HOUR)),
    (@mid_base + 63, @uid_chen,  @cid_feedback, 'Core:Text', '{"text":"fb-003 陈小龙: 我这边复现了"}', DATE_ADD(@t_fb_start, INTERVAL 6 HOUR)),
    (@mid_base + 64, @uid_bob,   @cid_feedback, 'Core:Text', '{"text":"fb-004 bob: 我来查日志"}', DATE_ADD(@t_fb_start, INTERVAL 9 HOUR)),
    (@mid_base + 65, @uid_admin, @cid_feedback, 'Core:Text', '{"text":"fb-005 admin: 先收集信息"}', DATE_ADD(@t_fb_start, INTERVAL 12 HOUR)),
    (@mid_base + 66, @uid_alice, @cid_feedback, 'Core:Text', '{"text":"fb-006 alice: 需要截图"}', DATE_ADD(@t_fb_start, INTERVAL 15 HOUR)),
    (@mid_base + 67, @uid_guest, @cid_feedback, 'Core:Text', '{"text":"fb-007 guest: 已补充"}', DATE_ADD(@t_fb_start, INTERVAL 18 HOUR)),
    (@mid_base + 68, @uid_bob,   @cid_feedback, 'Core:Text', '{"text":"fb-008 bob: 可能是索引缺失"}', DATE_ADD(@t_fb_start, INTERVAL 21 HOUR)),
    (@mid_base + 69, @uid_admin, @cid_feedback, 'Core:Text', '{"text":"fb-009 admin: 下个版本修"}', DATE_ADD(@t_fb_start, INTERVAL 24 HOUR)),
    (@mid_base + 70, @uid_chen,  @cid_feedback, 'Core:Text', '{"text":"fb-010 陈小龙: ok"}', DATE_ADD(@t_fb_start, INTERVAL 27 HOUR)),
    (@mid_base + 71, @uid_alice, @cid_feedback, 'Core:Text', '{"text":"fb-011 alice: 顺便补测试"}', DATE_ADD(@t_fb_start, INTERVAL 30 HOUR)),
    (@mid_base + 72, @uid_admin, @cid_feedback, 'Core:Text', '{"text":"fb-012 admin: 结案"}', DATE_ADD(@t_fb_start, INTERVAL 33 HOUR)),

    -- 公告：5 条消息
    (@mid_base + 73, @uid_admin, @cid_announcement, 'Core:Text', '{"text":"ann-001 系统公告：欢迎使用 CarryPigeon"}', DATE_ADD(@t_ann_start, INTERVAL 0 DAY)),
    (@mid_base + 74, @uid_admin, @cid_announcement, 'Core:Text', '{"text":"ann-002 系统公告：本周维护窗口周六 02:00"}', DATE_ADD(@t_ann_start, INTERVAL 3 DAY)),
    (@mid_base + 75, @uid_admin, @cid_announcement, 'Core:Text', '{"text":"ann-003 系统公告：请勿泄露测试 token"}', DATE_ADD(@t_ann_start, INTERVAL 7 DAY)),
    (@mid_base + 76, @uid_admin, @cid_announcement, 'Core:Text', '{"text":"ann-004 系统公告：读状态功能已上线"}', DATE_ADD(@t_ann_start, INTERVAL 10 DAY)),
    (@mid_base + 77, @uid_admin, @cid_announcement, 'Core:Text', '{"text":"ann-005 系统公告：反馈请到 产品反馈"}', DATE_ADD(@t_ann_start, INTERVAL 13 DAY)),

    -- 临时会议：5 条消息
    (@mid_base + 78, @uid_bob,   @cid_meeting, 'Core:Text', '{"text":"meet-001 bob: 会议开始"}', DATE_ADD(@t_meet_start, INTERVAL 0 HOUR)),
    (@mid_base + 79, @uid_alice, @cid_meeting, 'Core:Text', '{"text":"meet-002 alice: 讨论任务拆分"}', DATE_ADD(@t_meet_start, INTERVAL 1 HOUR)),
    (@mid_base + 80, @uid_chen,  @cid_meeting, 'Core:Text', '{"text":"meet-003 陈小龙: 前端排期"}', DATE_ADD(@t_meet_start, INTERVAL 2 HOUR)),
    (@mid_base + 81, @uid_bob,   @cid_meeting, 'Core:Text', '{"text":"meet-004 bob: 后端排期"}', DATE_ADD(@t_meet_start, INTERVAL 3 HOUR)),
    (@mid_base + 82, @uid_alice, @cid_meeting, 'Core:Text', '{"text":"meet-005 alice: 会议结束"}', DATE_ADD(@t_meet_start, INTERVAL 4 HOUR));

-- ----------------------------
-- 9) channel_read_state（覆盖：从未读/部分已读/接近最新）
-- last_read_time: epoch millis
-- ----------------------------
INSERT INTO `channel_read_state`
(`id`, `uid`, `cid`, `last_read_time`) VALUES
    -- 公告
    (@rsid_base + 1,  @uid_admin,  @cid_announcement, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 1 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 2,  @uid_alice,  @cid_announcement, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 2 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 3,  @uid_bob,    @cid_announcement, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 3 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 4,  @uid_chen,   @cid_announcement, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 7 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 5,  @uid_eve,    @cid_announcement, 0),
    (@rsid_base + 6,  @uid_newbie, @cid_announcement, 0),
    (@rsid_base + 7,  @uid_guest,  @cid_announcement, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 5 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 8,  @uid_bot,    @cid_announcement, 0),

    -- 开发讨论（dev-058~060 为近期消息，因此这里留一点未读）
    (@rsid_base + 9,  @uid_alice, @cid_dev, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 2 MINUTE)) * 1000 AS SIGNED)),
    (@rsid_base + 10, @uid_admin, @cid_dev, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 6 HOUR)) * 1000 AS SIGNED)),
    (@rsid_base + 11, @uid_bob,   @cid_dev, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 10 MINUTE)) * 1000 AS SIGNED)),
    (@rsid_base + 12, @uid_chen,  @cid_dev, 0),
    (@rsid_base + 13, @uid_eve,   @cid_dev, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 12 HOUR)) * 1000 AS SIGNED)),
    (@rsid_base + 14, @uid_guest, @cid_dev, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 2 HOUR)) * 1000 AS SIGNED)),

    -- 产品反馈
    (@rsid_base + 15, @uid_admin, @cid_feedback, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 1 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 16, @uid_alice, @cid_feedback, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 2 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 17, @uid_bob,   @cid_feedback, 0),
    (@rsid_base + 18, @uid_chen,  @cid_feedback, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 3 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 19, @uid_guest, @cid_feedback, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 2 DAY)) * 1000 AS SIGNED)),

    -- 临时会议
    (@rsid_base + 20, @uid_bob,   @cid_meeting, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 1 DAY)) * 1000 AS SIGNED)),
    (@rsid_base + 21, @uid_alice, @cid_meeting, CAST(UNIX_TIMESTAMP(DATE_SUB(@seed_now, INTERVAL 36 HOUR)) * 1000 AS SIGNED)),
    (@rsid_base + 22, @uid_chen,  @cid_meeting, 0);

-- ----------------------------
-- 10) user_token（每个用户 1 个可用 token；另提供 1 个过期 token 示例）
-- expired_time 为 DATETIME
-- ----------------------------
INSERT INTO `user_token`
(`id`, `uid`, `token`, `expired_time`) VALUES
    (@tid_base + 1,  @uid_admin,  'cp_test_token_admin',  DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 2,  @uid_alice,  'cp_test_token_alice',  DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 3,  @uid_bob,    'cp_test_token_bob',    DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 4,  @uid_chen,   'cp_test_token_chen',   DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 5,  @uid_eve,    'cp_test_token_eve',    DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 6,  @uid_newbie, 'cp_test_token_newbie', DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 7,  @uid_guest,  'cp_test_token_guest',  DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 8,  @uid_bot,    'cp_test_token_bot',    DATE_ADD(@seed_now, INTERVAL 30 DAY)),
    (@tid_base + 9,  @uid_admin,  'cp_test_token_expired_example', DATE_SUB(@seed_now, INTERVAL 1 DAY));

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
