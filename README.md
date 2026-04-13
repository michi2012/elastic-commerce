# ElasticCommerce

ElasticCommerce는 엘라스틱서치, 카프카를 활용한 e-커머스 플랫폼입니다.

## 기술 스택
- **언어**: Java 17
- **프레임워크**: Spring Boot
- **데이터베이스 ORM**: Spring Data JPA, Mysql(AWS RDS)
- **분산 잠금**: Redis(AWS ElasticCache)
- **검색 엔진**: Elasticsearch(Elastic Cloud)
- **메시징 시스템**: Apache Kafka(AWS Msk)
- **인증/인가**: Spring Security(JWT)

---

## 주요 기능

### Cart
- **추가**: `POST /api/cart/{userId}/items`
- **조회**: `GET /api/cart/{userId}`
- **수량 수정**: `PATCH /api/cart/{userId}/items/{productId}`
- **아이템 삭제**: `DELETE /api/cart/{userId}/items/{productId}`
- **전체 비우기**: `DELETE /api/cart/{userId}`

### Coupon
- **회사 쿠폰 발행**: `POST /api/coupons/company`
- **사용자 쿠폰 할당**: `POST /api/coupons/issue`
- **쿠폰 적용**: `POST /api/coupons/apply`
- **회사 쿠폰 목록 조회**: `GET /api/coupons/company?page={page}&size={size}`
- **회사 쿠폰 상세 조회**: `GET /api/coupons/company/{couponId}`
- **사용자 쿠폰 조회**: `GET /api/coupons/user/{userId}`

### Order
- **주문 생성**: `POST /users/{userId}/orders`
- **주문 단건 조회**: `GET /users/{userId}/orders/{orderId}`
- **주문 목록 조회**: `GET /users/{userId}/orders?page={page}&size={size}`
- **주문 상태 업데이트**: `PUT /users/{userId}/orders/{orderId}/status`
- **주문 취소**: `DELETE /users/{userId}/orders/{orderId}`

### Payment
- **결제 생성**: `POST /users/{userId}/orders/{orderId}/payments`
- **결제 조회**: `GET /users/{userId}/orders/{orderId}/payments`

### Product
- **목록 조회**: `GET /api/products?page={page}&size={size}`
- **검색**: `GET /api/products/search?query={query}&category={category}&minPrice={minPrice}&maxPrice={maxPrice}`
- **자동완성**: `GET /api/products/suggestions?query={query}`
- **상품 생성**: `POST /api/products`
- **단건 조회**: `GET /api/products/{productId}`
- **수정**: `PATCH /api/products/{productId}`
- **삭제**: `DELETE /api/products/{productId}`
- **재고 업데이트**: `PATCH /api/products/{productId}/stock`
- **공개 처리**: `POST /api/products/{productId}/open`
- **비공개 처리**: `POST /api/products/{productId}/close`

### Review
- **리뷰 작성**: `POST /api/reviews`
- **리뷰 조회 (단건)**: `GET /api/reviews/{id}`
- **상품별 리뷰 목록**: `GET /api/reviews?productId={productId}`
- **리뷰 수정**: `PUT /api/reviews/{id}`
- **리뷰 삭제**: `DELETE /api/reviews/{id}`
- **리뷰 검색**: `GET /api/reviews/search?searchQuery={keyword}&rating={minRating}&page={page}&size={size}`

### User
- **회원 가입**: `POST /api/users/join`
- **여러 사용자 조회**: `GET /api/users?userIds={id1,id2,...}`
- **단건 사용자 조회**: `GET /api/users/{userId}`
- **정보 수정**: `POST /api/users/update`
- **탈퇴**: `POST /api/users/delete`

## 아키텍쳐

![아키텍처 다이어그램](https://github.com/201912025/elastic-commerce/blob/main/docs/images/aws-architecture.png)
![아키텍처 세부 다이어그램](https://github.com/201912025/elastic-commerce/blob/main/docs/images/aws-architecture-out.png)

