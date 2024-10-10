package edu.stanford.slac.core_work_management.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import edu.stanford.slac.core_work_management.model.Domain;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("hazelcast-instance");
//        config.getSerializationConfig()
//                .addSerializerConfig(
//                        new SerializerConfig()
//                                .setTypeClass(Domain.class)
//                                .setImplementation(new JacksonS())
//                );
        // Configure network and clustering
        NetworkConfig network = config.getNetworkConfig();
        network.setPort(5701).setPortAutoIncrement(true);

        JoinConfig join = network.getJoin();
        // Multicast configuration for automatic cluster discovery
        join.getMulticastConfig().setEnabled(true);
        join.getTcpIpConfig().setEnabled(false); // Disable TCP/IP for this example

        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastCacheManager(hazelcastInstance);
    }
}
