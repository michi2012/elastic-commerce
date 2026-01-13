package com.example.ElasticCommerce.domain.coupon.service.kafka;

import com.example.ElasticCommerce.domain.coupon.dto.CouponKafkaDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponKafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendCoupon(String topic, CouponKafkaDTO dto) {
        String msg;
        try {
            msg = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("CouponKafkaDTO 직렬화 실패", e);
            throw new RuntimeException("JSON serialization failed", e);
        }

        try {
            SendResult<String, String> result = kafkaTemplate.send(topic, msg).get();

            RecordMetadata meta = result.getRecordMetadata();
            log.info("카프카 메시지 전송 성공 topic={} partition={} offset={}",
                    meta.topic(), meta.partition(), meta.offset());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Kafka 전송 실패", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException("Kafka send failed", e);
        }
    }
}