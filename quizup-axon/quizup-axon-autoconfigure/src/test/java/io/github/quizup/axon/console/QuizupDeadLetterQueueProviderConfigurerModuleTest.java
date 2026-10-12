package io.github.quizup.axon.console;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizupDeadLetterQueueProviderConfigurerModuleTest {

    private static final List<String> INSTANCE_SCOPED = List.of("notification-ws", "presence-notification");

    @Test
    void dlqIsEnabledByDefaultForKebabCaseGroups() {
        QuizupAxonDlqProperties properties = new QuizupAxonDlqProperties(null, null, null, null, null);

        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "game-projection", properties, INSTANCE_SCOPED)).isTrue();
    }

    @Test
    void dlqIsDisabledWhenGloballyDisabled() {
        QuizupAxonDlqProperties properties = new QuizupAxonDlqProperties(false, null, null, null, null);

        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "game-projection", properties, INSTANCE_SCOPED)).isFalse();
    }

    @Test
    void dlqIsDisabledForExplicitlyExcludedGroups() {
        QuizupAxonDlqProperties properties =
                new QuizupAxonDlqProperties(true, List.of("game-projection"), null, null, null);

        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "game-projection", properties, INSTANCE_SCOPED)).isFalse();
        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "room-projection", properties, INSTANCE_SCOPED)).isTrue();
    }

    @Test
    void dlqIsDisabledForSagaFqcnGroups() {
        QuizupAxonDlqProperties properties = new QuizupAxonDlqProperties(null, null, null, null, null);

        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "io.github.quizup.game.GameFlowSaga", properties, INSTANCE_SCOPED)).isFalse();
    }

    @Test
    void dlqIsDisabledForInstanceScopedGroups() {
        QuizupAxonDlqProperties properties = new QuizupAxonDlqProperties(null, null, null, null, null);

        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "notification-ws", properties, INSTANCE_SCOPED)).isFalse();
        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "notification-ws-bff-7c9d4f", properties, INSTANCE_SCOPED)).isFalse();
        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                "notification-push", properties, INSTANCE_SCOPED)).isTrue();
    }

    @Test
    void dlqIsDisabledForBlankGroup() {
        QuizupAxonDlqProperties properties = new QuizupAxonDlqProperties(null, null, null, null, null);

        assertThat(QuizupDeadLetterQueueProviderConfigurerModule.isDlqEnabled(
                " ", properties, INSTANCE_SCOPED)).isFalse();
    }
}
