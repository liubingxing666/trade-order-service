package com.ksyun.trade.util;
import com.ksyun.trade.bootstrap.RedisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.util.HashMap;
import java.util.Map;

@Service
public class RedisUtils {

    private RedisConfig redisConfig;
    public static JedisPool jedisPool;
    @Autowired
    public RedisUtils(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(100); // 最大连接数
        jedisPoolConfig.setMaxIdle(50); // 最大空闲连接数

        String host = redisConfig.getHost(); // 获取配置文件中的host
        int port = redisConfig.getPort(); // 获取配置文件中的port
        String password = redisConfig.getPassWord(); // 获取配置文件中的password
        int db = redisConfig.getDb();
        System.out.println("lbxTESTREDIS:"+redisConfig.getHost());
        System.out.println("port:"+port);
        System.out.println(password);
        System.out.println(db);





        // 初始化Redis连接池
        jedisPool = new JedisPool(jedisPoolConfig, host, port,0, password, db);

        // jedisPool = new JedisPool(jedisPoolConfig, "localhost", 6379);

    }

    // 初始化Redis连接池配置

    // 存储数据到Redis的Hash中
    public static void set(String hashKey, Map<String, Object> data) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> mapData = new HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                mapData.put(entry.getKey(), entry.getValue().toString());
            }
            jedis.hmset(hashKey, mapData);
        }
    }

    // 获取Redis中的Hash数据，并转换为Map<String, Object>类型
    public static Map<String, Object> get(String hashKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> mapData = jedis.hgetAll(hashKey);
            Map<String, Object> resultMap = new HashMap<>();
            for (Map.Entry<String, String> entry : mapData.entrySet()) {
                resultMap.put(entry.getKey(), entry.getValue());
            }
            return resultMap;
        }
    }

    // 存储字符串数据到Redis中
    public static void setString(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }
    }

    // 获取Redis中的字符串数据
    public static String getString(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    // 判断键是否存在于Redis中
    public static boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        }
    }


    // 关闭Redis连接池
    public static void close() {
        jedisPool.close();
    }
}

