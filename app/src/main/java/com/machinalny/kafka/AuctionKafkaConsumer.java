package com.machinalny.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.model.AuctionRecord;
import com.machinalny.service.AuctionSniper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuctionKafkaConsumer {

    private final ObjectMapper objectMapper;

    private final AuctionSniper auctionSniper;

    public AuctionKafkaConsumer(ObjectMapper objectMapper, AuctionSniper auctionSniper) {
        this.objectMapper = objectMapper;
        this.auctionSniper = auctionSniper;
    }

    @KafkaListener(topics = "${auction-sniper.auction-topic}", groupId = "auctionSniper")
    public void receive(ConsumerRecord<String, String> consumerRecord) {
        log.info(consumerRecord.toString());
        try {
            AuctionRecord auctionRecord = objectMapper.readValue(consumerRecord.value(), AuctionRecord.class);
            this.auctionSniper.updateAuction(consumerRecord.key(), auctionRecord);
        } catch (JsonProcessingException e) {
            log.warn("Received message that didn't match ActionRecord {}, {}", consumerRecord.key(), consumerRecord.value());
        }
    }

}
