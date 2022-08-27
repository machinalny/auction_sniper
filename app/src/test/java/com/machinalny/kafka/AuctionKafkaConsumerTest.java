package com.machinalny.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.service.AuctionSniper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class AuctionKafkaConsumerTest {

    @MockBean
    private AuctionSniper auctionSniper;

    private ObjectMapper objectMapper = new ObjectMapper();

    private AuctionKafkaConsumer auctionKafkaConsumer;

    @Value("${auction-sniper.auction-topic}")
    private String auctionTopic;


    @BeforeEach
    void setUp() {
        auctionKafkaConsumer = new AuctionKafkaConsumer(objectMapper, auctionSniper);
    }

    @Test
    void invokesActionOnAuctionNotification() {
        ConsumerRecord<String, String> auctionMessage =
                new ConsumerRecord<>(auctionTopic, 0, 0l, "AUCTION", """
                        {
                        "bidder": "bidder"
                        }
                        """);
        auctionKafkaConsumer.receive(auctionMessage);

        verify(auctionSniper).updateAuction(any(), any());
    }
}