package eu.apps4net.feedora.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service to manage locks for preventing concurrent operations that could conflict.
 * This prevents OPML import operations from running at the same time as feed parsing operations.
 * It also keeps the progress of the running operation, so the frontend can show it.
 */
@Service
public class OperationLockService {
    
    private final ReentrantLock feedOperationLock = new ReentrantLock();
    // Feeds to check and feeds checked by the running operation. Zero total means the progress isn't known.
    private final AtomicInteger progressTotal = new AtomicInteger();
    private final AtomicInteger progressCompleted = new AtomicInteger();
    // Increased every time a scan starts, so the frontend can tell a scan ran even when it ended between two checks
    private final AtomicInteger runCount = new AtomicInteger();
    
    /**
     * Attempts to acquire the feed operation lock.
     * @return true if the lock was acquired, false if another operation is already running
     */
    public boolean tryLockFeedOperation() {
        return feedOperationLock.tryLock();
    }
    
    /**
     * Releases the feed operation lock and resets the progress.
     */
    public void unlockFeedOperation() {
        if (feedOperationLock.isHeldByCurrentThread()) {
            progressTotal.set(0);
            progressCompleted.set(0);
            feedOperationLock.unlock();
        }
    }
    
    /**
     * Checks if a feed operation is currently running.
     * @return true if an operation is running, false otherwise
     */
    public boolean isFeedOperationRunning() {
        return feedOperationLock.isLocked();
    }

    /**
     * Starts counting the progress of the running operation.
     * @param total The number of items (feeds) the operation will process
     */
    public void startProgress(int total) {
        progressCompleted.set(0);
        progressTotal.set(total);
        runCount.incrementAndGet();
    }

    /**
     * Counts one more processed item of the running operation.
     */
    public void incrementProgress() {
        progressCompleted.incrementAndGet();
    }

    public int getProgressTotal() {
        return progressTotal.get();
    }

    public int getProgressCompleted() {
        return progressCompleted.get();
    }

    public int getRunCount() {
        return runCount.get();
    }
}
