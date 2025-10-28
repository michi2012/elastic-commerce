package com.example.ElasticCommerce.domain.order.service;

import com.example.ElasticCommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.ElasticCommerce.domain.order.dto.request.UpdateOrderStatusRequest;
import com.example.ElasticCommerce.domain.order.dto.response.OrderDto;
import com.example.ElasticCommerce.domain.order.entity.Address;
import com.example.ElasticCommerce.domain.order.entity.Order;
import com.example.ElasticCommerce.domain.order.entity.OrderItem;
import com.example.ElasticCommerce.domain.order.entity.OrderStatus;
import com.example.ElasticCommerce.domain.order.exception.OrderExceptionType;
import com.example.ElasticCommerce.domain.order.repository.OrderRepository;
import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.product.repository.ProductRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderService orderService;

    private User testUser;
    private Product productA;
    private OrderItem itemA;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                       .username("testuser")
                       .email("test@example.com")
                       .password("password")
                       .role("USER")
                       .birthDay("1990-01-01")
                       .build();
        ReflectionTestUtils.setField(testUser, "userId", 1L);

        productA = Product.builder()
                          .productCode("P-A")
                          .name("Product A")
                          .category("Cat")
                          .stockQuantity(5)
                          .brand("BrandA")
                          .imageUrl("url")
                          .description("desc")
                          .price(100L)
                          .build();
        ReflectionTestUtils.setField(productA, "id", 10L);

        itemA = OrderItem.builder()
                         .product(productA)
                         .quantity(2)
                         .price(productA.getPrice())
                         .build();
    }

    private Order createDummyOrderWithAddress(Long orderId, List<OrderItem> items) {
        Address dummyAddress = Address.builder()
                                      .order(null)
                                      .recipientName("테스트수령인")
                                      .street("테스트로 1")
                                      .city("테스트시")
                                      .postalCode("12345")
                                      .phoneNumber("010-0000-0000")
                                      .build();

        Order order = Order.builder()
                           .user(testUser)
                           .items(items)
                           .address(dummyAddress)
                           .build();

        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(dummyAddress, "order", order);
        return order;
    }

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrder {
        @Test
        @DisplayName("성공: 올바른 사용자와 상품, 그리고 배송지 정보가 주어졌을 때 주문 생성")
        void 성공_올바른_사용자와_상품_및_배송지_정보가_주어졌을_때_주문_생성() {
            CreateOrderRequest.OrderItemRequest reqItem =
                    new CreateOrderRequest.OrderItemRequest(productA.getId(), 2);
            CreateOrderRequest.AddressRequest addrReq =
                    new CreateOrderRequest.AddressRequest(
                            "홍길동", "서울시 강남구 테헤란로 123", "서울", "06236", "010-1234-5678"
                    );
            CreateOrderRequest req = new CreateOrderRequest(
                    List.of(reqItem),
                    addrReq
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productA.getId())).thenReturn(Optional.of(productA));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderDto dto = orderService.createOrder(1L, req);

            assertThat(dto).isNotNull();
            assertThat(dto.userId()).isEqualTo(1L);
            assertThat(dto.items()).hasSize(1);
            assertThat(dto.items().get(0).productId()).isEqualTo(productA.getId());
            assertThat(productA.getStockQuantity()).isEqualTo(3);

            assertThat(dto.address()).satisfies(addr -> {
                assertThat(addr.recipientName()).isEqualTo("홍길동");
                assertThat(addr.street()).isEqualTo("서울시 강남구 테헤란로 123");
                assertThat(addr.city()).isEqualTo("서울");
                assertThat(addr.postalCode()).isEqualTo("06236");
                assertThat(addr.phoneNumber()).isEqualTo("010-1234-5678");
            });

            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 NotFoundException")
        void 실패_사용자가_없으면_NotFoundException() {
            CreateOrderRequest.AddressRequest addrReq =
                    new CreateOrderRequest.AddressRequest(
                            "홍길동", "서울시 강남구 테헤란로 123", "서울", "06236", "010-1234-5678"
                    );
            CreateOrderRequest req = new CreateOrderRequest(
                    List.of(),
                    addrReq
            );

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(1L, req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(UserExceptionType.NOT_FOUND_USER.getMessage());
        }
    }

    @Nested
    @DisplayName("단일 주문 조회 테스트")
    class GetOrder {
        @Test
        @DisplayName("성공: 존재하는 주문 조회")
        void 성공_존재하는_주문_조회() {
            Order order = createDummyOrderWithAddress(100L, List.of(itemA));
            when(orderRepository.findByIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(order));

            OrderDto dto = orderService.getOrder(1L, 100L);

            assertThat(dto.orderId()).isEqualTo(100L);
            assertThat(dto.items()).hasSize(1);
        }

        @Test
        @DisplayName("실패: 주문이 없으면 NotFoundException")
        void 실패_주문이_없으면_NotFoundException() {
            when(orderRepository.findByIdAndUserId(100L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrder(1L, 100L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(OrderExceptionType.ORDER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("주문 목록 조회 테스트")
    class ListOrders {
        @Test
        @DisplayName("성공: 페이징된 주문 목록 조회")
        void 성공_페이징된_주문_목록_조회() {
            Order o1 = createDummyOrderWithAddress(101L, List.of(itemA));
            Page<Order> page = new PageImpl<>(List.of(o1));

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(orderRepository.findAllByUserUserId(eq(1L), any(PageRequest.class)))
                    .thenReturn(page);

            Page<OrderDto> result = orderService.listOrders(1L, 0, 5);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).orderId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 NotFoundException")
        void 실패_사용자가_없으면_NotFoundException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.listOrders(1L, 0, 5))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(UserExceptionType.NOT_FOUND_USER.getMessage());
        }
    }

    @Nested
    @DisplayName("주문 상태 변경 테스트")
    class UpdateStatus {
        @Test
        @DisplayName("성공: 상태 변경")
        void 성공_상태_변경() {
            Order order = createDummyOrderWithAddress(200L, List.of(itemA));
            when(orderRepository.findByIdAndUserId(200L, 1L))
                    .thenReturn(Optional.of(order));

            UpdateOrderStatusRequest req = new UpdateOrderStatusRequest("PAID");
            OrderDto dto = orderService.updateOrderStatus(1L, 200L, req);

            assertThat(dto.status()).isEqualTo(OrderStatus.PAID.name());
        }

        @Test
        @DisplayName("실패: 잘못된 상태 전환 시 BadRequestException")
        void 실패_잘못된_상태_전환() {
            Order order = createDummyOrderWithAddress(200L, List.of(itemA));
            when(orderRepository.findByIdAndUserId(200L, 1L))
                    .thenReturn(Optional.of(order));

            UpdateOrderStatusRequest req = new UpdateOrderStatusRequest("DELIVERED");
            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, 200L, req))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 테스트")
    class CancelOrder {
        @Test
        @DisplayName("성공: 재고 복원 및 상태 CANCELLED")
        void 성공_재고_복원_및_취소() {
            Order order = createDummyOrderWithAddress(300L, List.of(itemA));
            when(orderRepository.findByIdAndUserId(300L, 1L))
                    .thenReturn(Optional.of(order));

            productA.updateStockQuantity(5);
            OrderDto dto = orderService.cancelOrder(1L, 300L);

            assertThat(dto.status()).isEqualTo(OrderStatus.CANCELLED.name());
            assertThat(productA.getStockQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("실패: 주문이 없으면 NotFoundException")
        void 실패_주문이_없으면_NotFoundException() {
            when(orderRepository.findByIdAndUserId(400L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 400L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(OrderExceptionType.ORDER_NOT_FOUND.getMessage());
        }
    }
}
