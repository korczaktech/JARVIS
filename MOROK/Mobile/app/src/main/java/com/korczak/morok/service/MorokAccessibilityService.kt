package com.korczak.morok.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MorokAccessibilityService: AccessibilityService() {
 companion object {
  @Volatile private var instance: MorokAccessibilityService? = null
  fun isEnabled() = instance != null
  fun back() = instance?.performGlobalAction(GLOBAL_ACTION_BACK) == true
  fun home() = instance?.performGlobalAction(GLOBAL_ACTION_HOME) == true
  fun recents() = instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) == true
 }
 override fun onServiceConnected() { super.onServiceConnected(); instance = this }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
 override fun onInterrupt() {}
 override fun onDestroy() { instance = null; super.onDestroy() }
}