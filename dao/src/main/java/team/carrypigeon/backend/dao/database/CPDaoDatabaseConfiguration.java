package team.carrypigeon.backend.dao.database;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * DAO 模块数据库配置（MyBatis-Plus Mapper 扫描入口）。
 * <p>
 * 该配置负责扫描本模块下的 Mapper 接口（{@code team.carrypigeon.backend.dao.database.mapper}），
 * 让 DAO 实现类可以注入对应的 Mapper 并通过 MyBatis-Plus 访问数据库。
 */
@Slf4j
@Configuration
@MapperScan({"team.carrypigeon.backend.dao.database.mapper"})
public class CPDaoDatabaseConfiguration {

    /**
     * 初始化日志（用于确认 MapperScan 生效）。
     */
    @PostConstruct
    public void init() {
        log.info("CPDaoDatabaseConfiguration initialized, mapperScan='team.carrypigeon.backend.dao.database.mapper'");
    }
}
