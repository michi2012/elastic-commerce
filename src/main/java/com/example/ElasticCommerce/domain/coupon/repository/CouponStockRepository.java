package com.example.ElasticCommerce.domain.coupon.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponStockRepository {
    private final RedisTemplate<String, String> redisTemplate;

    private String stockKey(String couponCode) {
        return "coupon-stock:" + couponCode;
    }

    public Long decrement(String couponCode) {
        return redisTemplate.opsForValue().decrement(stockKey(couponCode));
    }

    public Long increment(String couponCode) {
        return redisTemplate.opsForValue().increment(stockKey(couponCode));
    }

    public void setIfAbsent(String couponCode, int quantity) {
        redisTemplate.opsForValue().setIfAbsent(stockKey(couponCode), String.valueOf(quantity));
    }

    public void setInitialStock(String couponCode, int quantity) {
        redisTemplate.opsForValue().set(stockKey(couponCode), String.valueOf(quantity));
    }

    public Boolean existsKey(String couponCode) {
        return redisTemplate.hasKey(stockKey(couponCode));
    }

    public Long getStock(String couponCode) {
        String value = redisTemplate.opsForValue().get(stockKey(couponCode));
        return (value == null) ? null : Long.valueOf(value);
    }

    public void deleteAllKeys() {
        redisTemplate.keys("coupon-stock:*")
                     .forEach(redisTemplate::delete);
    }
}
