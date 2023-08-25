package com.ksyun.trade.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.redis")
public class RedisConfig {

    private String host;
    private int port;
    private int db;
    private String passWord;
    // Getters and Setters for the properties (host, port, db)
    // Getter and Setter methods for 'host'
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    // Getter and Setter methods for 'port'
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord){
        this.passWord=passWord;
    }

    // Getter and Setter methods for 'db'
    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
    }
}
