package com.example.ElasticCommerce.domain.coupon.exception;

import com.example.ElasticCommerce.global.exception.ExceptionType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponExceptionType implements ExceptionType {

    COUPON_NOT_FOUND               (3001, "해당 쿠폰을 찾을 수 없습니다."),
    COUPON_EXPIRED                 (3002, "이미 만료된 쿠폰입니다."),
    COUPON_OUT_OF_STOCK            (3003, "쿠폰 재고가 없습니다."),
    COUPON_MINIMUM_AMOUNT_NOT_MET  (3004, "최소 주문 금액에 도달하지 못하여 쿠폰을 사용할 수 없습니다."),
    COUPON_APPLICATION_FAILED      (3005, "쿠폰 적용에 실패했습니다."),
    COUPON_DUPLICATE_ISSUE         (3006, "이미 발급받은 쿠폰입니다."),
    COUPON_INTERNAL_ERROR          (3007, "쿠폰 발급 중 오류가 발생했습니다.");

    private final int statusCode;
    private final String message;
}
