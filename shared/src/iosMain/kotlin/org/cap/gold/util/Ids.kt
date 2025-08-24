package org.cap.gold.util

import platform.Foundation.NSUUID

actual fun newUUID(): String = NSUUID().UUIDString()
