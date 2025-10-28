package com.example.ElasticCommerce.domain.product.service.kafka;

import com.example.ElasticCommerce.domain.product.dto.kafka.ProductElasticDTO;
import com.example.ElasticCommerce.domain.product.entity.FailedEvent;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;
import com.example.ElasticCommerce.domain.product.repository.FailedEventRepository;
import com.example.ElasticCommerce.domain.product.repository.ProductDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductElasticDltConsumer {

    private final ProductDocumentRepository productDocumentRepository;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = "product-topic.DLT",
            groupId          = "${spring.kafka.consumer.group-id}.dlt",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "6"
    )
    @Transactional
    public void consumeDlt(String message, Acknowledgment ack) {
        log.info("[DLT] 메시지 수신: {}", message);

        ProductElasticDTO dto;
        try {
            dto = objectMapper.readValue(message, ProductElasticDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[DLT] JSON 파싱 실패, 메시지 버림: {}", message, e);

            FailedEvent failedEvent = FailedEvent.builder()
                                                 .payload(message)
                                                 .topic("product-topic.DLT")
                                                 .errorMessage(truncate(e.getMessage(), 500))
                                                 .retryCount(0)
                                                 .build();
            failedEventRepository.save(failedEvent);

            ack.acknowledge();
            return;
        }

        try {
            ProductDocument doc = ProductDocument.builder()
                                                 .id(dto.id())
                                                 .productCode(dto.productCode())
                                                 .name(dto.name())
                                                 .description(dto.description())
                                                 .price(dto.price())
                                                 .category(dto.category())
                                                 .stockQuantity(dto.stockQuantity())
                                                 .brand(dto.brand())
                                                 .imageUrl(dto.imageUrl())
                                                 .build();
            productDocumentRepository.save(doc);

            ack.acknowledge();
            log.info("[DLT] 재처리 성공, 오프셋 커밋: {}", dto.id());

        } catch (Exception ex) {
            log.error("[DLT] 재처리 중 예외 발생: {}", message, ex);

            FailedEvent failedEvent = FailedEvent.builder()
                                                 .payload(message)
                                                 .topic("product-topic.DLT")
                                                 .errorMessage(truncate(ex.getMessage(), 500))
                                                 .retryCount(1)
                                                 .build();
            failedEventRepository.save(failedEvent);

            ack.acknowledge();
        }
    }

    private String truncate(String message, int maxLen) {
        if (message == null) {
            return null;
        }
        return message.length() <= maxLen ? message : message.substring(0, maxLen);
    }
}
