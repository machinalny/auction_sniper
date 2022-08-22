package com.machinalny.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.service.AuctionSniper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class AuctionKafkaConsumerTest {

    @MockBean
    private AuctionKafkaProducer auctionKafkaProducer;

    @MockBean
    private AuctionSniper auctionSniper;

    private ObjectMapper objectMapper = new ObjectMapper();

    private AuctionKafkaConsumer auctionKafkaConsumer;

    @Value("${auction-sniper.auction-topic}")
    private String auctionTopic;

    @Test
    void reportsLostWhenAuctionCloses() throws JsonProcessingException {
        auctionKafkaConsumer = new AuctionKafkaConsumer(objectMapper, auctionSniper);
        ConsumerRecord<String, String> auctionMessage =
                new ConsumerRecord<>(auctionTopic, 0, 0l, "", """
                        {
                        "bidder": "bidder"
                        }
                        """);
        auctionKafkaConsumer.receive(auctionMessage);

        verify(auctionSniper).updateAuction(any());
    }
}