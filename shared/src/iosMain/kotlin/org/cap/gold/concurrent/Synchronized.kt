package org.cap.gold.concurrent

import platform.Foundation.NSRecursiveLock

/**
 * Native (iOS) implementation of the [Synchronized] annotation.
 * Uses NSRecursiveLock for thread-safety on iOS.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
actual annotation class Synchronized()

/**
 * Executes the given block while holding the lock.
 */
internal inline fun <T> NSRecursiveLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
