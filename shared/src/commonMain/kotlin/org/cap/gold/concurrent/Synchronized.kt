package org.cap.gold.concurrent

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Creates a synchronized property delegate.
 * On JVM, this uses Java's built-in synchronization.
 * On iOS, this uses NSRecursiveLock.
// */
//internal expect class SynchronizedProperty<T>(
//    initialValue: T
//) : ReadWriteProperty<Any?, T>
//
///**
// * Creates a synchronized property delegate.
// */
//@Suppress("ACTUAL_WITHOUT_EXPECT") // This is a common implementation
//expect fun <T> synchronized(initialValue: T): ReadWriteProperty<Any?, T>
//
//// Common implementation for the synchronized function
//internal actual fun <T> synchronized(initialValue: T): ReadWriteProperty<Any?, T> =
//    SynchronizedProperty(initialValue)
