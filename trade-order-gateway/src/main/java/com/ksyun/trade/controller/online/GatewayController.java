package com.ksyun.trade.controller.online;

import com.github.rholder.retry.RetryException;
import com.ksyun.req.trace.ReqTraceConsts;
import com.ksyun.trade.bootstrap.RedisConfig;
import com.ksyun.trade.dto.VoucherDeductDTO;
import com.ksyun.trade.service.GatewayService;
import com.ksyun.trade.service.RedisLeakyBucketService;
import com.ksyun.trade.util.RedisUtils;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@RestController
public class GatewayController {
    @Autowired
    private GatewayService gatewayService;
    @Autowired
    HttpServletRequest s;
    public static JedisPool jedisPool;
    RedisLeakyBucketService redisLeakyBucketService;
    @Autowired
    public GatewayController(RedisConfig redisConfig,RedisLeakyBucketService redisLeakyBucketService){
        this.redisLeakyBucketService=redisLeakyBucketService;
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(100); // 最大连接数
        jedisPoolConfig.setMaxIdle(50); // 最大空闲连接数
        String host = redisConfig.getHost(); // 获取配置文件中的host
        int port = redisConfig.getPort(); // 获取配置文件中的port
        String password = redisConfig.getPassWord(); // 获取配置文件中的password
        int db = redisConfig.getDb();
        jedisPool = new JedisPool(jedisPoolConfig, host, port,0, password, db);
    }

    /**
     * 查询订单详情 (GET)
     */
    @RequestMapping(value="/online/queryOrderInfo",produces = "application/json")
    public Object queryOrderInfo(Integer id) {
            String requestId = s.getHeader(ReqTraceConsts.REQUEST_ID);
            if(requestId!=null){
                MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
            }else{
                MDC.put(ReqTraceConsts.TRACE_KEY,UUID.randomUUID().toString());
            }
        return gatewayService.loadLalancing(id);
    }
    /**
     * 根据机房Id查询机房名称 (GET)
     */
    @RequestMapping(value = "/online/queryRegionName", produces = "application/json")
    public Object queryRegionName(Integer regionId) throws IOException, RetryException {
        String requestId = s.getHeader("X-KSY-REQUEST-ID");
        if(requestId!=null){
            MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
        }else{
            requestId=UUID.randomUUID().toString();
            MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
        }
        //MDC.put("TT",requestId);
        //System.out.println("!!!!!!!!!!!!:"+requestId);
        return gatewayService.queryRegionName(regionId,requestId);
    }

    /**
     * 订单优惠券抵扣 (POST json)
     */
    @RequestMapping(value = "/online/voucher/deduct", produces = "application/json")
    public Object deduct(@RequestBody VoucherDeductDTO param) {
        String requestId = s.getHeader(ReqTraceConsts.REQUEST_ID);
        if(requestId!=null){
            MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
        }else{
            requestId=UUID.randomUUID().toString();
            System.out.println("sasdhkasdk");
            MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
        }
        //MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
        return gatewayService.voucherDeduct(param);
    }

    /**
     * 基于Redis实现漏桶限流算法，并在API调用上体现
     */
    @RequestMapping(value = "/online/listUpstreamInfo", produces = "application/json")
    public Object listUpstreamInfo() {
        String requestId;
        if(s.getHeader("X-KSY-REQUEST-ID")==null){
            requestId =UUID.randomUUID().toString();
            MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
        }else{
            requestId=s.getHeader("X-KSY-REQUEST-ID");
            MDC.put(ReqTraceConsts.TRACE_KEY,requestId);
        }
        redisLeakyBucketService.jedis = jedisPool.getResource();
        if(redisLeakyBucketService.isAllowed()){
            Map<String,Object> resultMap =new LinkedHashMap<>();
            resultMap.put("code",200);
            resultMap.put("msg","ok");
            resultMap.put("requestId",requestId);
            List<String>listUrl=new ArrayList<>();
            listUrl.add("campus.query1.ksyun.com");
            listUrl.add("campus.query2.ksyun.com");
            resultMap.put("data",listUrl);
            return resultMap;
        }else{
            Map<String,Object> resultMap =new LinkedHashMap<>();
            resultMap.put("code",429);
            resultMap.put("msg","对不起, 系统压力过大, 请稍后再试!");
            resultMap.put("requestId",requestId);
            return resultMap;
        }
    }
}
