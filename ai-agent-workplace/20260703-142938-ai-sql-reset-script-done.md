# SQL 结构清理脚本任务单

## 任务目标

补充用于污染数据库重新初始化前的表结构清理脚本，支持先清空当前项目表，再执行 `00-all-in-one.sql` 重建结构。

## 任务类型

实现类任务，修改正式 SQL 文档，不修改 Java 代码。

## 影响模块

- `docs/sql/`

## 允许修改范围

- `docs/sql/*.sql`
- `docs/sql/README.md`
- 本任务单

## 禁止边界

- 不修改 Java 代码。
- 不新增依赖。
- 不改变数据库运行时装配。
- 不引入迁移框架。

## 治理文档

- `docs/standards/AI协作开发规范.md`
- `docs/standards/变更审核清单.md`
- `docs/operations/数据库部署手册.md`

## 执行计划

1. 新增一个明确标记为破坏性的 SQL reset 脚本。
2. 按外键依赖反向删除表。
3. 在 README 中补充执行顺序和风险说明。
4. 做 SQL 文本静态检查。

## 验收标准

- 脚本只清理当前项目管理的表，不执行 `DROP DATABASE`。
- 脚本可重复执行。
- README 明确警告数据会被删除。

## 实际修改

- 新增 `docs/sql/99-reset-schema.sql`，用于删除当前项目管理的全部表。
- 更新 `docs/sql/README.md`，补充污染库重置执行顺序和破坏性说明。

## 验证记录

```bash
python3 - <<'PY'
from pathlib import Path
import re
created=set(re.findall(r'CREATE TABLE\s+([a-zA-Z0-9_]+)', Path('docs/sql/00-all-in-one.sql').read_text()))
dropped=set(re.findall(r'DROP TABLE IF EXISTS\s+([a-zA-Z0-9_]+)', Path('docs/sql/99-reset-schema.sql').read_text()))
print('missing', sorted(created-dropped))
print('extra', sorted(dropped-created))
PY
```

结果：`missing []`、`extra []`。

## 残留风险

- 当前环境没有 MySQL CLI，未直接在真实 MySQL 上执行 reset + init。
