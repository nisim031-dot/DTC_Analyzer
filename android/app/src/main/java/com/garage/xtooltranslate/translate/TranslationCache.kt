package com.garage.xtooltranslate.translate

import android.util.LruCache

/**
 * מטמון תרגומים. ממשקי אבחון חוזרים על אותן מחרוזות שוב ושוב,
 * כך ש-hit rate גבוה וחוסך קריאות ML Kit (ולטנטיות).
 */
class TranslationCache(maxEntries: Int = 500) {
    private val cache = LruCache<String, String>(maxEntries)

    fun get(source: String): String? = cache.get(source)

    fun put(source: String, hebrew: String) {
        cache.put(source, hebrew)
    }
}
