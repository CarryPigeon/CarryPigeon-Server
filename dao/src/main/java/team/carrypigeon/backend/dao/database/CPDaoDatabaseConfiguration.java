package team.carrypigeon.backend.dao.database;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库配置，用于配置扫描路径和mybatis-plus相关配置
 * */
@Slf4j
@Configuration
@MapperScan({"team.carrypigeon.backend.dao.database.mapper"})
public class CPDaoDatabaseConfiguration {

    @PostConstruct
    public void init() {
        log.info("CPDaoDatabaseConfiguration initialized, mapperScan='team.carrypigeon.backend.dao.database.mapper'");
    }
}
