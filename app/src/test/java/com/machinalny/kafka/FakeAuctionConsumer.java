package com.machinalny.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class FakeAuctionConsumer {

    private CountDownLatch latch = new CountDownLatch(1);
    private String payload;
    
    @KafkaListener(topics = {"item-54321", "item-65432"}, groupId = "auctionService")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        payload = consumerRecord.toString();
        latch.countDown();
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }


    public void startSellingItem() {
    }

    public void hasReceivedJoinRequestFromSniper() throws InterruptedException {
        boolean messageConsumed = this.latch.await(10, TimeUnit.SECONDS);
        if (!messageConsumed){
            throw new RuntimeException("Didn't got any message");
        }

    }

    public void announceClosed() {
    }
}
