package edu.stanford.slac.core_work_management.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = Config.loadDefault();
        config.setInstanceName("hazelcast-instance");
        config.getNetworkConfig().setPort(5701);
        MapConfig mapConfig = new MapConfig();
        mapConfig.setName("default");
        mapConfig.setTimeToLiveSeconds(60); // Set TTL to 1 hour
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastCacheManager(hazelcastInstance);
    }
}
