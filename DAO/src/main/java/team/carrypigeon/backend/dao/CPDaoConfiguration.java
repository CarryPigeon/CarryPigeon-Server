package team.carrypigeon.backend.dao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库配置，用于配置扫描路径和mybatis-plus相关配置
 * */
@Configuration
@MapperScan({"team.carrypigeon.backend.dao.mapper"})
public class CPDaoConfiguration {}