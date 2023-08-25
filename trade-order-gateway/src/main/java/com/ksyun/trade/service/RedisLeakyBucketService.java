package com.ksyun.trade.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import redis.clients.jedis.Jedis;
@Component
@Slf4j
public class RedisLeakyBucketService {

    private static final String BUCKET_KEY = "leaky_bucket_lbxUse";
    private static final long BUCKET_CAPACITY = 5; // QPS 设置为 5，每秒支持 5 个请求
    private static final long INTERVAL_TIME = 1000; // 限流时间间隔为 1000 毫秒（1 秒）

    public Jedis jedis;


//    public RedisLeakyBucketService(Jedis jedis) {
//        this.jedis = jedis;
//    }

    public boolean isAllowed() {
        long currentTimestamp = System.currentTimeMillis();
        String currentTimestampStr = String.valueOf(currentTimestamp);
        // 添加当前时间戳到 Redis 漏桶中

        this.jedis.lpush(BUCKET_KEY, currentTimestampStr);
        // 删除超过 INTERVAL_TIME 时间范围的时间戳
        jedis.lrange(BUCKET_KEY, 0, -1)
                .stream()
                .map(Long::parseLong)
                .filter(timestamp -> currentTimestamp - timestamp > INTERVAL_TIME)
                .forEach(timestamp -> jedis.lrem(BUCKET_KEY, 0, timestamp.toString()));

        // 获取漏桶中的时间戳数量
        long timestampCount = jedis.llen(BUCKET_KEY);
        log.info("timestampCount:{}",timestampCount);
        log.info("是否可以访问:{}",timestampCount <= BUCKET_CAPACITY);
        // 判断是否超过 QPS
        return timestampCount <= BUCKET_CAPACITY;
    }
}
