package com.machinalny.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.model.AuctionRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuctionKafkaProducer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${auction-sniper.auction-topic}")
    private String auctionTopic;

    public AuctionKafkaProducer(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String auction, AuctionRecord auctionRecord) throws JsonProcessingException {
        kafkaTemplate.send(auctionTopic, auction, objectMapper.writeValueAsString(auctionRecord));
    }

}
