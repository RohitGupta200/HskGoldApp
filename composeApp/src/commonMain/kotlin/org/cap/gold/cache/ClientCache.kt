package org.cap.gold.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Very small, centralized in-memory client cache with TTL.
 * Keyed by a string (e.g., HTTP method + URL + query + user).
 */
object ClientCache {
    private data class Entry(
        val payload: String,
        val expiresAtMillis: Long
    )

    private val mutex = Mutex()
    private val store = mutableMapOf<String, Entry>()

    suspend fun getFresh(key: String): String? = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        val e = store[key]
        if (e != null && e.expiresAtMillis > now) e.payload else null
    }

    suspend fun put(key: String, payload: String, ttlSeconds: Long) = mutex.withLock {
        val expires = Clock.System.now().toEpochMilliseconds() + ttlSeconds * 1000
        store[key] = Entry(payload, expires)
    }

    suspend fun invalidate(prefix: String) = mutex.withLock {
        val toRemove = store.keys.filter { it.startsWith(prefix) }
        toRemove.forEach { store.remove(it) }
    }

    suspend fun clear() = mutex.withLock { store.clear() }
}
