package pushservice.configuration;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

	@Value("${redis.host}")
	String host;
	
	@Value("${redis.port:6379}")
	int port;
	
	@Value("${redis.db:0}")
	int db;
	
	@Value("${redis.secret:}")
	String secret;

    @Value("${redis_sentinel_master}")
    private String sentinelMaster;

    @Value("${redis_sentinel_nodes}")
    private String sentinelNodes;
	
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        if (StringUtils.isNotBlank(sentinelMaster) && StringUtils.isNotBlank(sentinelNodes)) {
            RedisSentinelConfiguration configure = new RedisSentinelConfiguration();
            configure.setMaster(sentinelMaster);
            configure.setSentinels(Arrays.stream(sentinelNodes.split(","))
                .map(this::buildRedisNode)
                .collect(Collectors.toList()));
            configure.setDatabase(db);
            if (StringUtils.isNotBlank(secret)) {
                configure.setPassword(secret);
            }
            return new LettuceConnectionFactory(configure);
        } else {
            RedisStandaloneConfiguration configure = new RedisStandaloneConfiguration(host, port);
            configure.setDatabase(db);
            if (StringUtils.isNotBlank(secret)) {
                configure.setPassword(secret);
            }
            return new LettuceConnectionFactory(configure);
        }
    }
	
	@Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        Jackson2JsonRedisSerializer<Object> Jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer.setObjectMapper(om);
        //string序列化配置
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        /*配置具体的序列化方式*/
        //key采用string的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        //hash的key采用string的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        //value序列化方式采用 jackson
        template.setValueSerializer(Jackson2JsonRedisSerializer);
        //hash的value序列化方式采用 jackson
        template.setHashValueSerializer(Jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }
	
	@Bean
	public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
	    return new RedisLockRegistry(redisConnectionFactory, "redis-lock",
	        TimeUnit.MINUTES.toMillis(10));
	}

    /**
     * 建立 RedisNode 物件
     */
    private RedisNode buildRedisNode(String sentinelNodeStr) {
        String[] strArr = StringUtils.split(sentinelNodeStr, ":");
        return new RedisNode(strArr[0], Integer.parseInt(strArr[1]));
    }
}
