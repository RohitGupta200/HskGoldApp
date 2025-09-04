package org.cap.gold.concurrent

import platform.Foundation.NSRecursiveLock

/**
 * Executes the given block while holding the lock using NSRecursiveLock.
 */
internal inline fun <T> NSRecursiveLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
