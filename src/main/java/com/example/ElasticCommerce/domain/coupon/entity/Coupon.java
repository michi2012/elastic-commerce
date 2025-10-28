package com.example.ElasticCommerce.domain.coupon.entity;

import com.example.ElasticCommerce.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(nullable = false, unique = true, length = 50)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiscountType discountType;

    @Column(nullable = false)
    private Long discountValue;

    @Column(nullable = false)
    private Long minimumOrderAmount;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private Integer quantity;

    @Builder
    public Coupon(String couponCode,
                  DiscountType discountType,
                  Long discountValue,
                  Long minimumOrderAmount,
                  LocalDateTime expirationDate,
                  Integer quantity) {
        this.couponCode = couponCode;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.expirationDate = expirationDate;
        this.quantity = quantity;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expirationDate);
    }

    public boolean hasStock() {
        return quantity != null && quantity > 0;
    }

    public Long calculateDiscountAmount(Long orderAmount) {
        if (discountType == DiscountType.FIXED) {
            return Math.min(discountValue, orderAmount);
        } else {
            return Math.floorDiv(orderAmount * discountValue, 100);
        }
    }

    public void decreaseQuantity() {
        this.quantity -= 1;
    }
}
