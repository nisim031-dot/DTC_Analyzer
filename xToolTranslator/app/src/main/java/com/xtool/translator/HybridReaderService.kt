package com.xtool.translator

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** קורא טקסט נגיש (תפריטים/כפתורים) ישירות מה-Views ומתרגם לבועה — בלי OCR. */
class HybridReaderService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collectText(root, texts)
        for (t in texts.distinct().take(20)) {
            TranslationEngine.translate(t) { he -> FloatingBubble.append(t, he) }
        }
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.toString()?.trim()?.let { if (it.isNotEmpty()) out.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, out) }
        }
    }

    override fun onInterrupt() {}
}
