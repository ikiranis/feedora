package eu.apps4net.feedora.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Service to manage locks for preventing concurrent operations that could conflict.
 * This prevents OPML import operations from running at the same time as feed parsing operations.
 */
@Service
public class OperationLockService {
    
    private final ReentrantLock feedOperationLock = new ReentrantLock();
    
    /**
     * Attempts to acquire the feed operation lock.
     * @return true if the lock was acquired, false if another operation is already running
     */
    public boolean tryLockFeedOperation() {
        return feedOperationLock.tryLock();
    }
    
    /**
     * Releases the feed operation lock.
     */
    public void unlockFeedOperation() {
        if (feedOperationLock.isHeldByCurrentThread()) {
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
}
