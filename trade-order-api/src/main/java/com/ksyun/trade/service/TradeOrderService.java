package com.ksyun.trade.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ksyun.trade.dto.VoucherDeductDTO;
import com.ksyun.trade.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDateTime;

@Service
@Slf4j
public class TradeOrderService {


    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private HttpServletRequest request;

//    @Value("${spring.redis.host}")
//    private String host;
//    @Value("${spring.redis.port}")
//    private String port;
    @Value("${meta.url}")
    private String metaURL;

    @Autowired
    RedisTest redisTest;


    public Object query(Integer id) {
        //TODO
        log.info("映射成功");
        //获取8089前的url
        Map<String,Object> resultsMap=new LinkedHashMap<>();
        String beforeURL= request.getRequestURL().toString();
        String[] resultURLs=beforeURL.split(":",0);
        String upstreamURL=resultURLs[0]+resultURLs[1];
        resultsMap.put("upsteam",upstreamURL);
        log.info("resultMap为{}",resultsMap);
        //数据库执行订单查询
        String sql = String.format("SELECT user_id,price_value FROM KSC_TRADE_ORDER WHERE id = %d", id);
        Map<String,Object> results = jdbcTemplate.queryForMap(sql);
        resultsMap.putAll(results);
        String resultMetaURl=metaURL+"/online/user/"+id;
        log.info("resultMetaURl:{}",resultMetaURl);
//        Map<String,Object>hashmap=new HashMap<>();
//        hashmap.put("111","222");
//        RedisUtils.set("hello",hashmap);
//        System.out.println("RedisUtils.get(hello):"+RedisUtils.get("hello"));
//       // redisTest.jedis.hmset("resultsMap",resultsMap);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(resultMetaURl).build();
        try {
            Response response = null;
            response = client.newCall(request).execute();
            String responseBody = response.body().string();
            log.info("responseBody:{}",responseBody);
           // System.out.println("responseBody:"+responseBody);
            Gson gson = new Gson();
            // http://campus.meta.ksyun.com:8090/online/user/1获取第三方接口的数据
            Map<String, Object> jsonObject = gson.fromJson(responseBody, Map.class);
            // 获取"data"字段对应的值，即内层的JSON对象
            Map<String, Object> jsonData = (Map<String, Object>) jsonObject.get("data");
            jsonData.remove("id");
            log.info("jsonData:{}",jsonData);
            //System.out.println("jsonData:"+jsonData);
            resultsMap.put("user", jsonData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取http://campus.meta.ksyun.com:8090/online/region/list，并存到redis中
        String resultRegionURl="http://campus.meta.ksyun.com:8090/online/region/list";
        OkHttpClient clientRegion = new OkHttpClient();
        Request requestRegion = new Request.Builder().url(resultRegionURl).build();
        try {
            Response responseRegion = null;
            responseRegion = clientRegion.newCall(requestRegion).execute();
            String responseBodyRegion = responseRegion.body().string();
            System.out.println("responseBodyRegion:"+responseBodyRegion);
            Gson gson = new Gson();
            Map<String, Object> jsonObject = gson.fromJson(responseBodyRegion, Map.class);

            // 获取"data"字段对应的值，即内层的JSON对象
            List<Map<String, Object>> jsonData = ( List<Map<String, Object>>) jsonObject.get("data");

            // 使用Stream API过滤出某id的Map
            Optional<Map<String, Object>> filterMap = jsonData.stream()
                    .filter(map -> map.containsKey("id") && (Double) map.get("id") == id.doubleValue())
                    .findFirst();
            Map<String, Object> filterResultMap = filterMap.orElse(new HashMap<>());
            filterResultMap.remove("id");
            filterResultMap.remove("status");
            log.info("filterMap:{}",filterMap);
            // 检查是否找到了满足条件的Map，然后进行相应的处理
            resultsMap.put("region", filterResultMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sql = String.format("SELECT item_no,item_name,unit,value FROM ksc_trade_product_config WHERE order_id = %d", id);
        List<Map<String,Object>> queryConfigSql = jdbcTemplate.queryForList(sql);
        log.info("queryConfigSql:{}",queryConfigSql);
        resultsMap.put("config",queryConfigSql);
        if(!results.isEmpty()){
            log.info("最终查询结果为：{}",resultsMap);
           // System.out.println("最终查询结果为：" + resultsMap);
            return resultsMap;
        }else{
            return null;
        }
    }

    public Object queryVoucherId(VoucherDeductDTO param) {
        int rowsInserted=0;
        LocalDateTime createTime = LocalDateTime.now();
        String querypriceValueSql=String.format("SELECT price_value FROM ksc_trade_order WHERE id =%d",param.getOrderId());
        BigDecimal priceValue= jdbcTemplate.queryForObject(querypriceValueSql,BigDecimal.class);
        log.info("priceValue为{}",priceValue);
        param.setCreateTime(createTime);
        String voucherDeductSql = String.format("SELECT order_id FROM ksc_voucher_deduct WHERE order_id=%d ORDER BY create_time DESC LIMIT 1", param.getOrderId());
        List<Map<String, Object>>results =new ArrayList<>();
        results=jdbcTemplate.queryForList(voucherDeductSql);
        if(results.isEmpty()){
            param.setBeforeDeductAmount(priceValue);
            if(param.getBeforeDeductAmount().compareTo(param.getAmount())<0){
                param.setAfterDeductAmount(new BigDecimal("0"));
                param.setBeforeDeductAmount(new BigDecimal("0"));
            }else{
                param.setAfterDeductAmount(param.getBeforeDeductAmount().subtract(param.getAmount()));
            }
            param.setUpdateTime(LocalDateTime.now());
            String sql = "INSERT INTO ksc_voucher_deduct (order_id, voucher_no, amount, before_deduct_amount, after_deduct_amount,create_time,update_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
            Object[] params = {
                    param.getOrderId(),
                    param.getVoucherNo(),
                    param.getAmount(),
                    priceValue,
                    param.getAfterDeductAmount(),
                    param.getCreateTime(),
                    param.getUpdateTime()
            };
            rowsInserted = jdbcTemplate.update(sql, params);
            log.info("优惠券插入结果为{}",param);
        }
        else{
            String queryAfterpriceSql = String.format("SELECT after_deduct_amount FROM ksc_voucher_deduct WHERE order_id=? ORDER BY create_time DESC LIMIT 1", param.getOrderId());
            BigDecimal afterDeductAmount = jdbcTemplate.queryForObject(queryAfterpriceSql, BigDecimal.class, param.getOrderId());
            param.setBeforeDeductAmount(afterDeductAmount);
            if(afterDeductAmount.compareTo(param.getAmount())<0){
                param.setAfterDeductAmount(new BigDecimal("0"));
                param.setBeforeDeductAmount(new BigDecimal("0"));
            }else{
                param.setAfterDeductAmount(afterDeductAmount.subtract(param.getAmount()));
            }
            param.setUpdateTime(LocalDateTime.now());
            String sql = "INSERT INTO ksc_voucher_deduct (order_id, voucher_no, amount, before_deduct_amount, after_deduct_amount,create_time,update_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
            Object[] params = {
                    param.getOrderId(),
                    param.getVoucherNo(),
                    param.getAmount(),
                    param.getBeforeDeductAmount(),
                    param.getAfterDeductAmount(),
                    param.getCreateTime(),
                    param.getUpdateTime()
            };
            rowsInserted = jdbcTemplate.update(sql, params);
            log.info("优惠券插入结果为{}",param);
        }
        if (rowsInserted > 0) {

            System.out.println("Data inserted successfully.");
            return "success";
        } else {
            System.out.println("Data insertion failed.");
            return "failed";
        }
    }



}