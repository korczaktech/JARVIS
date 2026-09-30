package com.korczak.morok.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MorokAccessibilityService: AccessibilityService() {
 companion object {
  @Volatile private var instance: MorokAccessibilityService? = null
  fun isEnabled() = instance != null
  fun back() = instance?.performGlobalAction(GLOBAL_ACTION_BACK) == true
  fun home() = instance?.performGlobalAction(GLOBAL_ACTION_HOME) == true
  fun recents() = instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) == true
  fun clickText(text:String) = instance?.findAndClick(text) == true
  fun typeText(text:String) = instance?.typeIntoFocusedField(text) == true
 }
 override fun onServiceConnected() { super.onServiceConnected(); instance = this }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
 override fun onInterrupt() {}
 override fun onDestroy() { instance = null; super.onDestroy() }

 private fun findAndClick(text:String):Boolean {
  val root=rootInActiveWindow ?: return false
  val target=text.trim().lowercase()
  val nodes=root.findAccessibilityNodeInfosByText(text)
  val node=nodes.firstOrNull { it.isVisibleToUser && (it.text?.toString()?.lowercase()==target || it.contentDescription?.toString()?.lowercase()==target) }
   ?: nodes.firstOrNull { it.isVisibleToUser } ?: return false
  return clickNode(node)
 }
 private fun clickNode(node:AccessibilityNodeInfo):Boolean {
  if(node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
  var parent=node.parent
  while(parent!=null) {
   if(parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
   parent=parent.parent
  }
  return false
 }
 private fun typeIntoFocusedField(text:String):Boolean {
  val root=rootInActiveWindow ?: return false
  val focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
  val args=Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text) }
  return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args)
 }
}
