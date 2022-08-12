package com.machinalny.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.model.AuctionRecord;
import com.machinalny.service.AuctionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuctionKafkaConsumer {

    private final ObjectMapper objectMapper;

    private final AuctionService auctionService;

    public AuctionKafkaConsumer(ObjectMapper objectMapper, AuctionService auctionService) {
        this.objectMapper = objectMapper;
        this.auctionService = auctionService;
    }

    @KafkaListener(topics = "${auction-sniper.auction-topic}", groupId = "auctionSniper")
    public void receive(ConsumerRecord<String, String> consumerRecord) throws JsonProcessingException {
        log.info(consumerRecord.toString());
        AuctionRecord auctionRecord = objectMapper.readValue(consumerRecord.value(), AuctionRecord.class);
        this.auctionService.updateAuction(auctionRecord);
    }

}
