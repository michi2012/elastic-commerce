package com.example.ElasticCommerce.domain.coupon.dto;

public record CouponKafkaDTO(
        Long userId,
        String couponCode
) {
}