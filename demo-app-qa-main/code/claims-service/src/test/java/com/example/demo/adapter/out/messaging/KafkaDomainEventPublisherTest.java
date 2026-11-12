package com.example.demo.adapter.out.messaging;

import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.claim.events.ClaimStatusChanged;
import com.example.demo.domain.claim.events.ClaimSubmitted;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaMessageSchemaValidator schemaValidator;

    private ObjectMapper objectMapper;
    private KafkaDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new KafkaDomainEventPublisher(kafkaTemplate, objectMapper, schemaValidator);
    }

    @Test
    void should_PublishClaimSubmittedEventEnvelope_When_EventProvided() {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );

        doNothing().when(schemaValidator).validate(anyString(), anyString());
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"eventType\":\"claim-submitted\"");
        assertThat(payload).contains("\"eventId\":\"" + event.eventId() + "\"");
        assertThat(payload).contains("\"claimId\":\"" + claimId + "\"");
        assertThat(payload).contains("\"userId\":\"");
        verify(schemaValidator).validate(eq(payload), eq("ClaimSubmittedMessage"));
    }

    @Test
    void should_PublishClaimStatusChangedEnvelope_When_EventProvided() {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimStatusChanged(
                UUID.randomUUID().toString(),
                claimId,
                ClaimStatus.SUBMITTED,
                ClaimStatus.UNDER_REVIEW,
                UUID.randomUUID(),
                Instant.now()
        );

        doNothing().when(schemaValidator).validate(anyString(), anyString());
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"eventType\":\"claim-status-changed\"");
        assertThat(payload).contains("\"eventId\":\"" + event.eventId() + "\"");
        assertThat(payload).contains("\"newStatus\":\"UNDER_REVIEW\"");
        assertThat(payload).contains("\"oldStatus\":\"SUBMITTED\"");
        verify(schemaValidator).validate(eq(payload), eq("ClaimStatusChangedMessage"));
    }

    @Test
    void should_ThrowIllegalStateException_When_EnvelopeSerializationFails() throws JsonProcessingException {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );

        ObjectMapper failingMapper = spy(new ObjectMapper());
        doThrow(new JsonProcessingException("test") {})
                .when(failingMapper).writeValueAsString(any());

        KafkaDomainEventPublisher failingPublisher = new KafkaDomainEventPublisher(kafkaTemplate, failingMapper, schemaValidator);

        assertThatThrownBy(() -> failingPublisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize domain event");
    }

    @Test
    void should_PropagateSchemaValidationException_When_ValidatorRejectsMessage() {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );

        doThrow(new KafkaMessageValidationException("claimId: is missing but it is required"))
                .when(schemaValidator).validate(anyString(), eq("ClaimSubmittedMessage"));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("claimId");
    }
}
