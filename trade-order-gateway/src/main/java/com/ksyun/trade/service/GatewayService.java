package com.ksyun.trade.service;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.gson.Gson;
import com.ksyun.trade.dto.VoucherDeductDTO;
import com.ksyun.trade.rest.RestResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;

@Service
@Slf4j
public class GatewayService {
    @Autowired
    HttpServletRequest request;
    @Value("${actions}")
    private String urlString;
    private List<String> urlList = new ArrayList<>();
    int retryNum=0;
    //随机算法获取两个url中的一个
    public String getRandomUrl() {
        String[] urls = urlString.split(",");
        for (String url : urls) {
            urlList.add(url);
            //System.out.println("url:"+url.toString());
        }
        Random random = new Random();
        int index = random.nextInt(urlList.size());
        return urlList.get(index);
    }


    public Object loadLalancing(Object param) {
        // 1. 模拟路由 (负载均衡) 获取接口
//        String requestURL = request.getRequestURL().toString();
//        System.out.println(requestURL);
        String headerValue=MDC.get("TT");
        System.out.println("headervalue:"+headerValue);
        String id = param.toString();
        String randomURL = getRandomUrl();
        // 2. 请求转发
        String resultUrl = randomURL + "/online/trade_order/" + param.toString();
        log.info("负载均衡：转发去了{}",resultUrl);
        //System.out.println("resultUrl:" + resultUrl);
        // 处理发生异常的情况，返回空对象或其他信息
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(resultUrl)
                .header("Header-Name", headerValue) // 设置请求头
                .build();
        try {
            Response response = null;
            response = client.newCall(request).execute();
            String responseBody = null;
            responseBody = response.body().string();
            System.out.println("responseBody:" + responseBody);
            return responseBody;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //bak
//    public RestResult queryRegionName(Object param) {
//        String requestID= MDC.get("TT");
//        Integer id = (Integer) param;
//        Map<String, Object> resultsMap = new LinkedHashMap<>();
//        String resultRegionURl = "http://campus.meta.ksyun.com:8090/online/region/list";
//        OkHttpClient clientRegion = new OkHttpClient();
//        Request requestRegion = new Request.Builder().url(resultRegionURl).build();
//        try {
//            Response responseRegion = null;
//            responseRegion = clientRegion.newCall(requestRegion).execute();
//            String responseBodyRegion = responseRegion.body().string();
//            System.out.println("responseBody:" + responseBodyRegion);
//            Gson gson = new Gson();
//            Map<String, Object> jsonObject = gson.fromJson(responseBodyRegion, Map.class);
//            // 获取"data"字段对应的值，即内层的JSON对象
//            List<Map<String, Object>> jsonData = (List<Map<String, Object>>) jsonObject.get("data");
//            // 使用Stream API过滤出id为1.0的Map
//            Optional<Map<String, Object>> filterMap = jsonData.stream()
//                    .filter(map -> map.containsKey("id") && (Double) map.get("id") == id.doubleValue())
//                    .findFirst();
//            Map<String, Object> filterResultMap = filterMap.orElse(new HashMap<>());
//            System.out.println("filterResultMap:" + filterResultMap);
//            String regionName = filterResultMap.get("name").toString();
//            System.out.println("filterResultMap.get(id.doubleValue()):" + filterResultMap.get(id.doubleValue()));
//            if (!filterResultMap.isEmpty()) {
//                RestResult.success().requestId(requestID).data(regionName);
//                return RestResult.success().requestId(requestID).data(regionName);
//            } else {
//                return RestResult.failure().requestId(requestID).data(regionName);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public Object queryRegionName(Object param,String requestId) throws IOException, RetryException, com.github.rholder.retry.RetryException {
        Integer id = (Integer) param;
        //重试的回调函数
        Callable<Map<String, Object>> regionCallable = () -> {
            retryNum++;
            log.info("http://campus.meta.ksyun.com:8090/online/region/name/id重试第{}次",retryNum);
            String resultRegionURl = "http://campus.meta.ksyun.com:8090/online/region/name/" + id;
            OkHttpClient clientRegion = new OkHttpClient();
            Request requestRegion = new Request.Builder().url(resultRegionURl).build();
            Response responseRegion = clientRegion.newCall(requestRegion).execute();
            String responseBodyRegion = responseRegion.body().string();
            log.info("从第三方接口获取到的数据为:{}",responseBodyRegion);
            //System.out.println("responseBody:" + responseBodyRegion);
            Gson gson = new Gson();
            //设置输出格式
            Map<String,Object>reslutMap=new LinkedHashMap<>();
            reslutMap=gson.fromJson(responseBodyRegion, Map.class);
            reslutMap.remove("descr");
            reslutMap.put("requestId",requestId);
            log.info("输出内容为:{}",reslutMap);
            return reslutMap;
        };
        Retryer<Map<String, Object>> retryer = RetryerBuilder.<Map<String, Object>>newBuilder()
                .retryIfResult(result -> {
                    double codeDouble = (double) result.get("code");
                    int code = (int) codeDouble;
                    return code != 200; // 重试条件：code不等于200时重试
                })
                .withWaitStrategy(WaitStrategies.fixedWait(500L, TimeUnit.MILLISECONDS)) // 重试间隔500毫秒
                .withStopStrategy(StopStrategies.stopAfterAttempt(10)) // 最大重试次数为10次
                .build();
        try {
            return retryer.call(regionCallable);
        } catch (ExecutionException e) {
            // 处理重试失败的异常
            e.printStackTrace();
        }

        return null;
    }



    public Object voucherDeduct(VoucherDeductDTO param) {
        String headerValue=MDC.get("TT");
        String jsonParam = new Gson().toJson(param);
        String id = param.toString();
        String randomURL = getRandomUrl();
        // 2. 请求转发
        String resultUrl = randomURL + "/online/trade_order/voucherId/" + param.getOrderId();
        log.info("resultUrl:{}",resultUrl);
        //System.out.println("resultUrl:" + resultUrl);
        // 使用OkHttp发送POST请求
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaType,jsonParam);
        Request request = new Request.Builder()
                .url(resultUrl)
                .post(requestBody)
                .addHeader("Header-Name", headerValue)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            log.info("responseBody:{}",responseBody);
           // System.out.println("responseBody:" + responseBody);
            return responseBody;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
