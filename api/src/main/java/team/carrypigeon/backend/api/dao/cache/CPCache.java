package team.carrypigeon.backend.api.dao.cache;

/**
 * 缓存接口
 * @author midreamsheep
 * */
public interface CPCache {
    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间，单位秒
     * */
    void set(String key, String value,int expireTime);
    /**
     * 获取缓存
     * @param key 缓存键
     * */
    String get(String key);
    /**
     * 获取并删除缓存
     * @param key 缓存键
     * */
    String getAndDelete(String key);
    /**
     * 获取并设置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间，单位秒
     * */
    String getAndSet(String key, String value,int expireTime);
    /**
     * 判断缓存是否存在
     * @param key 缓存键
     * */
    boolean exists(String key);
    /**
     * 删除缓存
     * @param key 缓存键
     * */
    boolean delete(String key);
}
