package org.cap.gold.concurrent

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class SynchronizedProperty<T> constructor(
    initialValue: T
) : ReadWriteProperty<Any?, T> {
    private var value = initialValue
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = 
        synchronized(this) { value }
    
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(this) { this.value = value }
    }
}
