package com.machinalny.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.model.AuctionRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FakeAuctionConsumer {

    private CountDownLatch latch = new CountDownLatch(1);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${test.auction1}")
    private String auction;

    @Value("${auction-sniper.auction-topic}")
    private String auctionTopic;

    private String payload;

    @KafkaListener(topics = {"${auction-sniper.auction-topic}"}, groupId = "auctionService")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        payload = consumerRecord.toString();
        log.info(payload);
        latch.countDown();
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }


    public void startSellingItem() {
        this.resetLatch();
    }

    public void hasReceivedJoinRequestFromSniper() throws InterruptedException {
        boolean messageConsumed = this.latch.await(20, TimeUnit.SECONDS);
        if (!messageConsumed) {
            throw new RuntimeException("Didn't got any message");
        }
    }

    public void announceClosed() throws JsonProcessingException {
        kafkaTemplate.send(auctionTopic, objectMapper.writeValueAsString(AuctionRecord.builder()
                .messageType("LOST")
                .itemIdentification(auction)
                .build()));
    }
}
