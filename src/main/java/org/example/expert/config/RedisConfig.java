package org.example.expert.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // <String, Object> -> Key, Value
        // Key -> String, Value -> Object

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 추가 Java 8에서 추가된 LocalDate, LocalDateTime을 커버하기 위한 설정
        // ObjectMapper 커스터마이징
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//                .activateDefaultTyping(
//                        LaissezFaireSubTypeValidator.instance,
//                        ObjectMapper.DefaultTyping.NON_FINAL,
//                        JsonTypeInfo.As.PROPERTY
//                );

        /* activateDefaultTyping -> Redis에 저장할 때 타입 정보를 JSON에 같이 저장하는 설정,
           activateDefaultTyping는 배열 형태로 저장되며 첫 번쨰 타입이 문자열을 기대하고 있는데,
           현재 UserCacheService코드를 보면 List<UserSearchResponse>를 저장하고 있음,
           이렇게 되면 문자열이 아닌 객체가 와버리는 문제가 생김 -> Unexpected token 에러 발생
           UserSearchResponse에 LocalDateTime 필드를 다루지 않으므로 타입 정보를 굳이 저장할 필요 없을것 같음
           -> 제거
           */

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

         /* 자바에서 Redis에 값을 넣을 때는 자바에서 사용한 타입을 그대로 사용 못함
         자바에는 Long, long, int, Integer -> Redis에서는 이 타입들을 지원하지 않음
         자바에서 어떤 타읍으로 보내든 Redis에서는 "문자열"로 저장을 함*/

        /* 자바에서는 특정 타입으로 보냈는데 -> Redis에서는 문자열로 저장한다.
         나중에 Redis에서 값을 받아올 때 문자열로 받아옴 -> 자바에서 타입 불일치가 발생할 수 있음
         JSON을 파싱해주는 설정을 넣어줘여 함*/

        // 1. String data type 대비

        /* Key부분을 문자열로 파싱해주는 메서드 (Key의 문자열 전용 파싱)
         위에서 Key-Value값을 String, Object로 잡아줬는데, Key는 String 타입이므로,
         StringRedisSerializer()를 사용*/
        template.setKeySerializer(new StringRedisSerializer());
        /* Value는 Object로 잡아놔서 String처럼 명확하지 않음
         이럴 경우에는 GenericJackson2JsonRedisSerializer()를 사용 -> JSON 타입을 파싱해 줌
         String 타입 뿐만 아니라 어떠한 타입이든 알아서 맞게끔 타입을 변환해주겠다는 의미*/
        template.setValueSerializer(serializer);


        // 2. Hash data type 대비

        /* Redis에 DTO객체들을 넣은건데, DTO 객체는 String으로 받지 못함,
         DTO객체는 보통 Redis의 Hash 타입으로 받음,*/

        // 위에서 String 타입으로 받는것 외에도 Key를 Hash 타입으로도 받을 수 있게끔 만들어줘야 함

        // Key를 Hash타입으로 받을 수 있게 setHashKeySerializer
        template.setHashKeySerializer(new StringRedisSerializer());
        // Value를 Hash 타입으로 받을 수 있게 setHashValueSerializer
        template.setHashValueSerializer(serializer);

        // 위의 설정들을 정상적으로 반영하겠다는 의미
        template.afterPropertiesSet();
        return template;
    }
}
