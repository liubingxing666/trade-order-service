package com.ksyun.trade.controller.online;

import com.google.gson.Gson;
import com.ksyun.req.trace.RequestTraceContextSlf4jMDCHolder;
import com.ksyun.trade.bootstrap.RedisConfig;
import com.ksyun.trade.dto.VoucherDeductDTO;
import com.ksyun.trade.rest.RestResult;
import com.ksyun.trade.service.TradeOrderService;
import com.ksyun.trade.util.RedisUtils;
import com.ksyun.trade.util.date.BetweenFormater;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/online/trade_order", produces = {MediaType.APPLICATION_JSON_VALUE})
@Slf4j
public class TradeOrderController {

    @Autowired
    private TradeOrderService orderService;
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private String port;
    @Value("${meta.url}")
    private String metaURL;
    @Autowired
    private RedisConfig redisConfig;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private Map<String,Object> localCache;

    @RequestMapping("/{id}")
    public RestResult query(@PathVariable("id") Integer id,HttpServletRequest request) {
        String headerValue = request.getHeader("Header-Name");
        MDC.put("requestId",headerValue);
        //判断redis中是否有id为"id"的键
        if(localCache.containsKey(id.toString())){
            log.info("用到了本地缓存");
            String sss=RedisUtils.getString(id.toString());
            Gson gson = new Gson();
            Object queryResult1 = gson.fromJson(sss, Object.class);
            return RestResult.success().requestId(headerValue).data(queryResult1);
        }
        else if(RedisUtils.exists(id.toString())){
            log.info("用到了redis缓存！！");
            String sss=RedisUtils.getString(id.toString());
            Gson gson = new Gson();
            Object queryResult1 = gson.fromJson(sss, Object.class);
            return RestResult.success().requestId(headerValue).data(queryResult1);
        }else {
            //没有就查数据库
            log.info("本地和redis都没有，去查数据库！！！");
            Object queryResult=orderService.query(id);
            Gson gson = new Gson();
            String jsonResult = gson.toJson(queryResult);
            RedisUtils.setString(id.toString(),jsonResult);
            localCache.put(id.toString(),jsonResult);
            if(queryResult!=null){
                // 这里的Object.class应该替换为你的对象类型
               //System.out.println("redisService.getHost():"+redisConfig.getHost());
                return RestResult.success().requestId(headerValue).data(queryResult);
            }
        }
        return RestResult.failure().data(orderService.query(id));
    }

    @RequestMapping(value="/voucherId/{vid}",method = RequestMethod.POST)
    public Map<String, Object> queryVoucherId(@PathVariable("vid") Integer vid, @RequestBody VoucherDeductDTO param,HttpServletRequest request) {
        String requestId=request.getHeader("Header-Name");
        MDC.put("requestId",requestId);
        //System.out.println(RestResult.success().data(orderService.query(id)));

        Object queryResult=orderService.queryVoucherId(param);

        if(queryResult.equals("success")){
            long start = System.currentTimeMillis();
            RestResult s= RestResult.success();
            s.setRequestId(requestId);
            s.setDescr(StringUtils.defaultString(new BetweenFormater(System.currentTimeMillis() - start).simpleFormat(), s.getDescr()));
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("code", s.getCode());
            resultMap.put("RequestId",requestId);
            resultMap.put("msg", s.getMsg());
            System.out.println("s.getDescr():"+s.getDescr());
            resultMap.put("descr", s.getDescr());
            log.info("返回结果为{}",resultMap);
            return resultMap;
        }
        else{
            Map<String,Object> faiureResult=(Map<String,Object>)RestResult.failure();
            return faiureResult;
        }
    }

}
