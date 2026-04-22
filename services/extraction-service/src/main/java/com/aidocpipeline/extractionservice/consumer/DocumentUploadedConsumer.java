package com.aidocpipeline.extractionservice.consumer;

import com.aidocpipeline.extractionservice.event.DocumentUploadedEvent;
import com.aidocpipeline.extractionservice.service.ExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentUploadedConsumer {

    private final ExtractionService extractionService;

    /**
     * This method is called automatically every time a message
     * lands on the document.uploaded Kafka topic.
     *
     * @KafkaListener tells Spring: watch this topic with this consumer group.
     * The consumer group ID ensures that if you run multiple instances of
     * this service, only ONE instance processes each message.
     */
    @KafkaListener(
            topics = "${kafka.topics.document-uploaded}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            @Payload DocumentUploadedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received DocumentUploadedEvent. DocumentId: {}, " +
                        "File: {}, Partition: {}, Offset: {}",
                event.getDocumentId(), event.getFileName(), partition, offset);

        extractionService.processDocument(event);
    }
}