package com.example.ElasticCommerce.domain.coupon.entity;

import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_coupons", indexes = {
        @Index(name = "idx_user_coupon_check", columnList = "user_id, coupon_code", unique = true)
})
public class UserCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCouponId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private boolean used = false;

    @Builder
    public UserCoupon(Coupon coupon, User user) {
        this.coupon = coupon;
        this.user = user;
    }

    public boolean isUsed() {
        return used;
    }

    public void apply() {
        this.used = true;
    }
}
