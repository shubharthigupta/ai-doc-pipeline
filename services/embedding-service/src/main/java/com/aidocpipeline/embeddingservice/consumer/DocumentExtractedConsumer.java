package com.aidocpipeline.embeddingservice.consumer;

import com.aidocpipeline.embeddingservice.event.DocumentExtractedEvent;
import com.aidocpipeline.embeddingservice.service.EmbeddingService;
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
public class DocumentExtractedConsumer {

    private final EmbeddingService embeddingService;

    @KafkaListener(
            topics = "${kafka.topics.document-extracted}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload DocumentExtractedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received DocumentExtractedEvent. " +
                        "DocumentId: {}, TotalChunks: {}, Partition: {}, Offset: {}",
                event.getDocumentId(), event.getTotalChunks(), partition, offset);

        try {
            embeddingService.generateEmbeddings(event);
        } catch (Exception e) {
            log.error("Failed to generate embeddings for document: {}. Error: {}",
                    event.getDocumentId(), e.getMessage(), e);
        }
    }
}