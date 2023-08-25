#  trade-order



## 说明

* 项目编码：UTF-8
* JDK:1.8
* IDE：不限
* 项目开发方式  
  - 分支开发，发布时合并主干
  - 环境隔离（profile：local、dev、uat、prod）

## 模块

* biz：公共业务模块（供其它模块调用）<br/>
* biz-base:基础业务 <br/>
* biz-req-trace:请求链路跟踪<br/>
* biz-glue:<br/>
* trade-order-api：业务代码 <br/>
* trade-order-gateway：路由服务 <br/>



## 功能一  实现类似于Nginx请求转发功能

1.通过applications.yml读取负载均衡的URL列表，actions如下

```
actions: http://campus.query1.ksyun.com:8089,http://campus.query2.ksyun.com:8089
```

2.通过随机负载均衡算法将trade-order-gateway的请求打至trade-order-api

3.采用postman发送http请求。

![image-20230724155308298](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724155308298.png)

4.**结果**

系统报的日志如下，可以看到两次请求发送去了两个不同的url

![image-20230724154946379](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724154946379.png)

## 功能二  查询订单详情

1.通过postman发送请求

![image-20230724155908860](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724155908860.png)

2.通过随机负载均衡算法去打请求

3.用到了多级缓存，先差本地缓存，再查redis缓存，没有就最后再去查数据库

**第一次查询结果如下，本地和redis都没有，去查数据库和第三方接口共用了6.14s：**

![image-20230724160212352](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724160212352.png)

<img src="C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724155947701.png" alt="image-20230724155947701" style="zoom:33%;" />

**第二次结果如下，只用了2ms，用到了本地缓存**

![image-20230724160225537](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724160225537.png)

<img src="C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724160051803.png" alt="image-20230724160051803" style="zoom:33%;" />



**重启程序，会用到redis缓存**

![image-20230724160325094](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724160325094.png)

## 功能三  根据机房Id查询机房名称

**本功能用到的知识点：重试，我用的guava-retrying（第三方接口不稳定，我这里重试次数设置为10次）****

1.postman获得的结果如下

<img src="C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724161023300.png" alt="image-20230724161023300" style="zoom: 50%;" />

**这次运气不好，一共重试了五次才获取成功，哈哈哈**。

![image-20230724161131250](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724161131250.png)



## 功能四 订单优惠券抵扣公摊

​         用postman的http的post请求发送，，结果如下，可以抵扣多次

<img src="C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724170202922.png" alt="image-20230724170202922" style="zoom: 50%;" />

<img src="C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724170332050.png" alt="image-20230724170332050" style="zoom:50%;" />

## 功能五  基于Redis实现漏桶限流算法，并在API调用上体现

1.实现思路

```java
1.写了一漏桶限流类  RedisLeakyBucketService，
2.定义了INTERVAL_TIME = 1000;表示一秒
3.定义了jedis的list变量，往里面存储时间戳，通过计算1s内时间戳的数量来判断当前有多少给http请求，如果请求数量大于设定的阈值(5),就返回false，表示现在http请求已经满了，不可访问了
```

用postman发送请求，我一次发了100个，从下图中可以看出在数量为6时就不可以访问了

![image-20230724173210636](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724173210636.png)

postman发送批量请求的，所以我在浏览器访问结果如下，所以是访问不了的

<img src="C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724173738375.png" alt="image-20230724173738375" style="zoom:67%;" />

## 简单的链路跟踪实现

   1.在postman发送http请求的时候带X-KSY-REQUEST-ID及requestId

2. 通过HttpServletRequest获取请求头的requestId，并放入MDC.put(traceId,requestId)放入日志中
3. 再通过把traceId放入logback的配置文件中就可以了

在日志中体现如下：

![image-20230724174833152](C:\Users\liubingxin\AppData\Roaming\Typora\typora-user-images\image-20230724174833152.png)
