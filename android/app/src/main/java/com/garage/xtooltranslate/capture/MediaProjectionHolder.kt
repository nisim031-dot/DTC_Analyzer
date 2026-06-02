package com.garage.xtooltranslate.capture

import android.content.Intent

/**
 * מחזיק את אישור לכידת המסך (resultCode + Intent) שהמשתמש העניק באונבורדינג.
 * לא ניתן להשיג אותו שוב בשקט — אם ה-projection מת, המשתמש חייב לאשר מחדש.
 */
object MediaProjectionHolder {
    var resultCode: Int = 0
        private set
    var resultData: Intent? = null
        private set

    fun store(code: Int, data: Intent) {
        resultCode = code
        resultData = data
    }

    fun hasPermission(): Boolean = resultData != null

    fun clear() {
        resultCode = 0
        resultData = null
    }
}
