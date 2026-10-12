package io.github.quizup.axon.console;

import org.axonframework.config.Configuration;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.EventTrackerStatus;
import org.axonframework.eventhandling.Segment;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsoleStateTest {

    private final Configuration configuration = mock(Configuration.class);
    private final EventProcessingConfiguration eventProcessing = mock(EventProcessingConfiguration.class);
    private final MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "quizup-game");

    @Test
    @SuppressWarnings("unchecked")
    void buildsSnapshotWithSegmentsAndDlqStats() {
        StreamingEventProcessor processor = mock(StreamingEventProcessor.class);
        EventTrackerStatus status = mock(EventTrackerStatus.class);
        when(processor.isRunning()).thenReturn(true);
        when(processor.isError()).thenReturn(false);
        when(processor.isReplaying()).thenReturn(true);
        when(processor.supportsReset()).thenReturn(true);
        when(processor.maxCapacity()).thenReturn(16);
        when(processor.getTokenStoreIdentifier()).thenReturn("test-token-store");
        when(processor.processingStatus()).thenReturn(Map.of(0, status));
        when(status.getSegment()).thenReturn(Segment.ROOT_SEGMENT);
        when(status.isCaughtUp()).thenReturn(false);
        when(status.isReplaying()).thenReturn(true);
        when(status.isMerging()).thenReturn(false);
        when(status.isErrorState()).thenReturn(true);
        when(status.getError()).thenReturn(new IllegalStateException("boom"));
        when(status.getCurrentPosition()).thenReturn(OptionalLong.of(42));
        when(status.getResetPosition()).thenReturn(OptionalLong.empty());

        SequencedDeadLetterQueue<EventMessage<?>> queue = (SequencedDeadLetterQueue<EventMessage<?>>)
                mock(SequencedDeadLetterQueue.class);
        when(queue.size()).thenReturn(3L);
        when(queue.amountOfSequences()).thenReturn(1L);
        when(eventProcessing.deadLetterQueue("game-projection")).thenReturn(Optional.of(queue));
        when(eventProcessing.eventProcessors()).thenReturn(Map.of("game-projection", processor));

        ConsoleSnapshot snapshot = state().snapshot();

        assertThat(snapshot.version()).isEqualTo(ConsoleSnapshot.VERSION);
        assertThat(snapshot.application()).isEqualTo("quizup-game");
        assertThat(snapshot.processors()).hasSize(1);
        ConsoleSnapshot.ProcessorSnapshot processorSnapshot = snapshot.processors().getFirst();
        assertThat(processorSnapshot.name()).isEqualTo("game-projection");
        assertThat(processorSnapshot.mode()).isEqualTo("tracking");
        assertThat(processorSnapshot.running()).isTrue();
        assertThat(processorSnapshot.replaying()).isTrue();
        assertThat(processorSnapshot.supportsReset()).isTrue();
        assertThat(processorSnapshot.canPause()).isTrue();
        assertThat(processorSnapshot.canPlay()).isFalse();
        assertThat(processorSnapshot.canSplit()).isTrue();
        assertThat(processorSnapshot.canMerge()).isFalse();
        assertThat(processorSnapshot.tokenStoreIdentifier()).isEqualTo("test-token-store");
        assertThat(processorSnapshot.activeThreads()).isEqualTo(1);
        assertThat(processorSnapshot.capacity()).isEqualTo(16);
        assertThat(processorSnapshot.warnings())
                .containsExactly("1 segment(s) en erreur", "Replay en cours sur 1 segment(s)");
        assertThat(processorSnapshot.segments()).hasSize(1);
        ConsoleSnapshot.SegmentSnapshot segment = processorSnapshot.segments().getFirst();
        assertThat(segment.segmentId()).isZero();
        assertThat(segment.onePartOf()).isEqualTo(1);
        assertThat(segment.caughtUp()).isFalse();
        assertThat(segment.errorState()).isTrue();
        assertThat(segment.errorType()).isEqualTo(IllegalStateException.class.getName());
        assertThat(segment.errorMessage()).isEqualTo("boom");
        assertThat(segment.currentPosition()).isEqualTo(42L);
        assertThat(segment.resetPosition()).isNull();
        assertThat(processorSnapshot.dlq()).isEqualTo(new ConsoleSnapshot.DlqStats(3, 1));
    }

    @Test
    void snapshotHasNoDlqForGroupsWithoutQueue() {
        EventProcessor processor = mock(EventProcessor.class);
        when(eventProcessing.eventProcessors()).thenReturn(Map.of("io.github.quizup.game.GameFlowSaga", processor));
        when(eventProcessing.deadLetterQueue("io.github.quizup.game.GameFlowSaga")).thenReturn(Optional.empty());

        ConsoleSnapshot snapshot = state().snapshot();

        assertThat(snapshot.processors()).hasSize(1);
        assertThat(snapshot.processors().getFirst().dlq()).isNull();
        assertThat(snapshot.processors().getFirst().segments()).isEmpty();
    }

    @Test
    void caughtUpAndReplayingHelpersHandleNonStreamingProcessors() {
        EventProcessor processor = mock(EventProcessor.class);

        assertThat(ConsoleState.caughtUp(processor)).isTrue();
        assertThat(ConsoleState.replaying(processor)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void deadLetterQueueIsEmptyForSagaGroups() {
        SagaConfiguration<?> saga = mock(SagaConfiguration.class);
        when(saga.processingGroup()).thenReturn("game-flow-saga");
        when(eventProcessing.sagaConfigurations()).thenReturn(java.util.List.of(saga));
        when(eventProcessing.deadLetterQueue("game-flow-saga"))
                .thenReturn(Optional.of(mock(SequencedDeadLetterQueue.class)));

        assertThat(state().deadLetterQueue("game-flow-saga")).isEmpty();
    }

    private ConsoleState state() {
        when(configuration.eventProcessingConfiguration()).thenReturn(eventProcessing);
        QuizupAxonConsoleProperties properties =
                new QuizupAxonConsoleProperties(true, null, null, true, true, "test-instance");
        return new ConsoleState(provider(configuration), environment, provider(null), properties);
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }
        };
    }
}
