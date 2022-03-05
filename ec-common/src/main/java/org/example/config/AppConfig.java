package org.example.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AppConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private String redisPort;

    @Value("${spring.redis.password}")
    private String redisPwd;


    /**
     * 配置分布式锁的redisson
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        //单机方式
        config.useSingleServer().setPassword(redisPwd).setAddress("redis://" + redisHost + ":" + redisPort);

        //集群
        //config.useClusterServers().addNodeAddress("redis://192.31.21.1:6379","redis://192.31.21.2:6379")

        return Redisson.create(config);
    }


}
