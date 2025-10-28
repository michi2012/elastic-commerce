// domain/order/entity/OrderItem.java
package com.example.ElasticCommerce.domain.order.entity;

import com.example.ElasticCommerce.domain.order.dto.response.OrderItemDto;
import com.example.ElasticCommerce.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long price;

    @Builder
    public OrderItem(Product product, int quantity, long price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public long getTotalPrice() {
        return this.price * this.quantity;
    }

    public OrderItemDto toDto() {
        return OrderItemDto.from(this);
    }
}
