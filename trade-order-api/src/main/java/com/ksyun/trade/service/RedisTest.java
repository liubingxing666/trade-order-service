package com.ksyun.trade.service;




import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisTest {
    public Jedis jedis;
    public  void test() {
        jedis=new Jedis("localhost");
        jedis.del("list");
        //String 增删改查
        jedis.set("name","lbx");
        jedis.get("name");
        System.out.println("值为："+jedis.get("name"));
        jedis.set("name","hahah");
        System.out.println("修改后的值为："+jedis.get("name"));
        jedis.del("name");
        System.out.println("删除后的值为："+jedis.get("name"));

        //List增删改查
        jedis.rpush("list","1");
        jedis.rpush("list","2");
        jedis.lpush("list","3");
        jedis.rpush("list","2");
        List<String> elements = jedis.lrange("list", 0, -1);
        System.out.println(elements);

        //count > 0: 从头往尾移除值为 value 的元素，count为移除的个数。
        // count < 0: 从尾往头移除值为 value 的元素，count为移除的个数。
        // count = 0: 移除所有值为 value 的元素
        jedis.lrem("list",2,"2");
        System.out.println(jedis.lrange("list",0,-1));
        // 删除区间以外的元素
        System.out.println(jedis.ltrim("list", 0, 0));
        System.out.println("all elements: " + jedis.lrange("list", 0, -1));



        //set增删改查
        String setkey="set";
        jedis.sadd(setkey,"aaa","bbb","ccc");
        jedis.sadd("set2","bbb","ccc","ddd");
        //查询
        System.out.println(jedis.smembers(setkey));
        boolean exists = jedis.sismember(setkey, "element1");
        System.out.println("Element exists: " + exists);
        //删除
        jedis.del(setkey);
        jedis.srandmember(setkey);


        //zset有序集合
        String zSetkey="zortedSetkey";
        jedis.zadd(zSetkey,1.0,"1");
        jedis.zadd(zSetkey,2.0,"2");
        jedis.zadd(zSetkey,3.0,"3");
        //查询
        System.out.println(jedis.zrange(zSetkey,0,-1));

        // 获取有序集合的成员数量
        long count = jedis.zcard(zSetkey);
        System.out.println(count);

        // 修改有序集合中元素的分数
        jedis.zadd(zSetkey, 4.0, "1");
        // 删除有序集合中的元素
        jedis.zrem(zSetkey,"1");

        // 获取有序集合中指定范围的元素（带分数）
        Set<Tuple> elementsWithScores = jedis.zrangeWithScores(zSetkey, 0, -1);
        System.out.println("Elements with Scores: " + elementsWithScores);



        //hash操作
        String hashKey="hash";
        jedis.hset(hashKey, "field1", "value1");
        jedis.hset(hashKey, "field2", "value2");
        jedis.hset(hashKey, "field3", "value3");

        // 获取Hash中的所有字段和值
        Map<String, String> hashValues = jedis.hgetAll(hashKey);
        System.out.println("Hash Values: " + hashValues);

        // 获取Hash中指定字段的值
        String value = jedis.hget(hashKey, "field2");
        System.out.println("Value for field2: " + value);

        // 修改Hash中的字段值
        jedis.hset(hashKey, "field2", "newvalue2");
        // 删除Hash中的字段
        jedis.hdel(hashKey, "field3");
        hashValues = jedis.hgetAll(hashKey);
        System.out.println(hashValues);
    }
}
