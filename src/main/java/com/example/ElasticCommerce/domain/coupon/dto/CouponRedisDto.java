package com.example.ElasticCommerce.domain.coupon.dto;

import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponRedisDto(
        Long couponId,
        String couponCode,
        DiscountType discountType,
        Long discountValue,
        Long minimumOrderAmount,
        LocalDateTime expirationDate,
        Integer totalQuantity // 초기 발행 총량 (참고용)
) {
    public static CouponRedisDto from(Coupon coupon) {
        return new CouponRedisDto(
                coupon.getCouponId(),
                coupon.getCouponCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getExpirationDate(),
                coupon.getQuantity()
        );
    }


    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expirationDate);
    }
}