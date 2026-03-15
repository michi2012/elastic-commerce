package com.example.ElasticCommerce.domain.coupon.service.kafka;

import com.example.ElasticCommerce.domain.coupon.dto.CouponKafkaDTO;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.exception.CouponExceptionType;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponKafkaConsumerService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final CouponStockRepository couponStockRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;


    @KafkaListener(
            topics = "coupon-topic",
            groupId = "coupon-group",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "6"
    )
    @Transactional
    public void consumeCoupon(String message, Acknowledgment ack) {
        log.info("[CONSUMER] 메시지 수신: {}", message);

        // DTO 파싱
        CouponKafkaDTO dto;
        try {
            dto = objectMapper.readValue(message, CouponKafkaDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[CONSUMER] JSON 파싱 실패, 메시지 버림", e);
            ack.acknowledge();
            return;
        }

        String code = dto.couponCode();
        Long userId = dto.userId();

        try {
            // 발급 기록 저장
            User user = userRepository.findById(dto.userId())
                                      .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));
            Coupon coupon = couponRepository.findByCouponCode(code)
                                            .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));
            userCouponRepository.save(
                    UserCoupon.builder()
                              .user(user)
                              .coupon(coupon)
                              .build()
            );
            log.info("[CONSUMER] 쿠폰 발급 완료: userId={}, code={}", userId, code);
        } catch (DataAccessException | NotFoundException ex) {
            log.error("[CONSUMER] 저장 실패, Redis 복구: {}", code, ex);
            // 롤백된 DB 재고→ 다시 복구
            couponStockRepository.increment(code);
            redisTemplate.opsForSet().remove("applied_user:" + code, String.valueOf(userId));
        } finally {
            ack.acknowledge();
        }
    }
}
