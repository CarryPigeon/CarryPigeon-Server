package team.carrypigeon.backend.infrastructure.basic;

import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 基础设施能力门面。
 * 职责：为上层模块提供简洁的基础能力入口。
 * 边界：这里只聚合固定基础设施能力，不暴露任何外部服务实现。
 */
public class InfrastructureBasics {

    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final JsonProvider jsonProvider;

    public InfrastructureBasics(IdGenerator idGenerator, TimeProvider timeProvider, JsonProvider jsonProvider) {
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.jsonProvider = jsonProvider;
    }

    public IdGenerator ids() {
        return idGenerator;
    }

    public TimeProvider time() {
        return timeProvider;
    }

    public JsonProvider json() {
        return jsonProvider;
    }
}
