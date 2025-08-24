package org.cap.gold.concurrent

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import platform.Foundation.NSRecursiveLock

internal actual class SynchronizedProperty<T> actual constructor(
    initialValue: T
) : ReadWriteProperty<Any?, T> {
    private var value = initialValue
    private val lock = NSRecursiveLock()
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        lock.lock()
        try {
            return value
        } finally {
            lock.unlock()
        }
    }
    
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        lock.lock()
        try {
            this.value = value
        } finally {
            lock.unlock()
        }
    }
}
