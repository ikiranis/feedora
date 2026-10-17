package eu.apps4net.feedora.services;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLockServiceTest {
    private final OperationLockService lockService = new OperationLockService();

    @Test
    void lockCanBeAcquiredAndReleased() {
        assertThat(lockService.isFeedOperationRunning()).isFalse();

        assertThat(lockService.tryLockFeedOperation()).isTrue();
        assertThat(lockService.isFeedOperationRunning()).isTrue();

        lockService.unlockFeedOperation();
        assertThat(lockService.isFeedOperationRunning()).isFalse();
    }

    @Test
    void anotherThreadCantAcquireOrReleaseAHeldLock() throws Exception {
        assertThat(lockService.tryLockFeedOperation()).isTrue();

        boolean acquiredElsewhere = CompletableFuture.supplyAsync(() -> {
            boolean acquired = lockService.tryLockFeedOperation();
            lockService.unlockFeedOperation();
            return acquired;
        }).get();

        assertThat(acquiredElsewhere).isFalse();
        assertThat(lockService.isFeedOperationRunning()).isTrue();
        lockService.unlockFeedOperation();
    }

    @Test
    void unlockingWithoutHoldingTheLockDoesNothing() {
        lockService.unlockFeedOperation();
        assertThat(lockService.isFeedOperationRunning()).isFalse();
    }

    @Test
    void progressIsCountedAndResetOnUnlock() {
        assertThat(lockService.tryLockFeedOperation()).isTrue();
        lockService.startProgress(3);
        lockService.incrementProgress();
        lockService.incrementProgress();
        assertThat(lockService.getProgressTotal()).isEqualTo(3);
        assertThat(lockService.getProgressCompleted()).isEqualTo(2);

        lockService.unlockFeedOperation();
        assertThat(lockService.getProgressTotal()).isZero();
        assertThat(lockService.getProgressCompleted()).isZero();
    }
}
