package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.CouponRedisDto;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.exception.CouponExceptionType;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CouponCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 이벤트 시작 전, 관리자가 이 메서드를 호출해 DB 데이터를 Redis에 밀어넣음
    public void putCouponCache(Coupon coupon) {
        CouponRedisDto dto = CouponRedisDto.from(coupon);
        redisTemplate.opsForValue().set("coupon_meta:" + coupon.getCouponCode(), dto);
    }

    // 선착순 발급 로직에서 호출
    public CouponRedisDto getCouponMeta(String couponCode) {
        CouponRedisDto dto = (CouponRedisDto) redisTemplate.opsForValue().get("coupon_meta:" + couponCode);

        if (dto == null) {
            // 선착순 이벤트 중에는 DB 조회를 안 하는 게 원칙이므로 예외 발생
            throw new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND);
        }
        return dto;
    }
}

