package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.*;
import com.example.ElasticCommerce.domain.coupon.dto.request.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.request.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.request.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.response.CompanyCouponDto;
import com.example.ElasticCommerce.domain.coupon.dto.response.UserCouponDto;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.exception.CouponExceptionType;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.coupon.service.kafka.CouponKafkaProducerService;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final CouponStockRepository couponStockRepository;
    private final CouponKafkaProducerService couponKafkaProducerService;
    private final Clock clock;

    @Transactional
    public Long issueCompanyCoupon(IssueCouponRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (couponRepository.findByCouponCode(request.couponCode()).isPresent()) {
            throw new BadRequestException(CouponExceptionType.COUPON_APPLICATION_FAILED);
        }

        if (request.discountValue() == null || request.discountValue() <= 0 ||
                request.minimumOrderAmount() == null || request.minimumOrderAmount() < 0 ||
                request.expirationDate() == null || request.expirationDate().isBefore(now) ||
                request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException(CouponExceptionType.COUPON_APPLICATION_FAILED);
        }

        Coupon coupon = Coupon.builder()
                              .couponCode(request.couponCode())
                              .discountType(request.discountType())
                              .discountValue(request.discountValue())
                              .minimumOrderAmount(request.minimumOrderAmount())
                              .expirationDate(request.expirationDate())
                              .quantity(request.quantity())
                              .build();

        couponRepository.save(coupon);
        couponStockRepository.setIfAbsent(request.couponCode(), request.quantity());

        return coupon.getCouponId();
    }

    @Transactional
    public void issueUserCoupon(IssueUserCouponRequest dto) {
        Long userId    = dto.userId();
        String couponCode = dto.couponCode();
        LocalDateTime now = LocalDateTime.now(clock);

        Coupon coupon = couponRepository.findByCouponCode(couponCode)
                                        .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

        if (coupon.isExpired(now)) {
            throw new BadRequestException(CouponExceptionType.COUPON_EXPIRED);
        }

        userCouponRepository.findByUserIdAndCouponCode(userId, couponCode)
                            .ifPresent(uc -> { throw new BadRequestException(CouponExceptionType.COUPON_DUPLICATE_ISSUE); });

        couponStockRepository.setIfAbsent(couponCode, coupon.getQuantity());

        Long newStock = couponStockRepository.decrement(couponCode);
        if (newStock < 0) {
            throw new BadRequestException(CouponExceptionType.COUPON_OUT_OF_STOCK);
        }

        CouponKafkaDTO kafkaDTO = CouponKafkaDTO.from(userId, coupon, now);
        couponKafkaProducerService.sendCoupon("coupon-topic", kafkaDTO);
    }

    @Transactional
    public Long applyCoupon(ApplyCouponRequest applyCouponRequest) {
        Long userId = applyCouponRequest.userId();
        String couponCode = applyCouponRequest.couponCode();
        Long orderAmount = applyCouponRequest.orderAmount();
        LocalDateTime now = LocalDateTime.now(clock);

        UserCoupon userCoupon = userCouponRepository
                .findByUserIdAndCouponCode(userId, couponCode)
                .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

        if (userCoupon.isUsed()) {
            throw new BadRequestException(CouponExceptionType.COUPON_APPLICATION_FAILED);
        }

        Coupon coupon = userCoupon.getCoupon();

        if (coupon.isExpired(now)) {
            throw new BadRequestException(CouponExceptionType.COUPON_EXPIRED);
        }

        if (orderAmount < coupon.getMinimumOrderAmount()) {
            throw new BadRequestException(CouponExceptionType.COUPON_MINIMUM_AMOUNT_NOT_MET);
        }

        userCoupon.apply();
        userCouponRepository.save(userCoupon);

        return coupon.calculateDiscountAmount(orderAmount);
    }

    @Transactional
    public void issueUserCouponInsertOnly(IssueUserCouponRequest request) {
        Long userId = request.userId();
        String couponCode = request.couponCode();
        LocalDateTime now = LocalDateTime.now(clock);

        Coupon coupon = couponRepository.findByCouponCode(couponCode)
                                        .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

        if (coupon.isExpired(now)) {
            throw new BadRequestException(CouponExceptionType.COUPON_EXPIRED);
        }

        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        CouponKafkaDTO kafkaDTO = CouponKafkaDTO.from(userId, coupon, now);
        couponKafkaProducerService.sendCoupon("coupon-topic", kafkaDTO);
    }

    public Page<CompanyCouponDto> getAllCompanyCoupons(int page, int size) {
        return couponRepository.findAll(PageRequest.of(page, size))
                               .map(CompanyCouponDto::from);
    }

    public CompanyCouponDto getCompanyCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                                        .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));
        return CompanyCouponDto.from(coupon);
    }

    public List<UserCouponDto> getUserCoupons(Long userId) {
        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        List<UserCoupon>  userCoupons = userCouponRepository.findAllByUserId(userId);

        return userCoupons.stream()
                          .map(UserCouponDto::from)
                          .collect(Collectors.toList());
    }
}
