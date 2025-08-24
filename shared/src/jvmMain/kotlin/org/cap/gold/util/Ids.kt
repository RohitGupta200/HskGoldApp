package org.cap.gold.util

import java.util.UUID

actual fun newUUID(): String = UUID.randomUUID().toString()
