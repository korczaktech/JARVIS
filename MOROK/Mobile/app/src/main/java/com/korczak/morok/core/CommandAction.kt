package com.korczak.morok.core
sealed interface CommandAction {
 data object None:CommandAction
 data object OpenSettings:CommandAction
 data class OpenApp(val query:String):CommandAction
 data object OpenAppList:CommandAction
 data object AccessibilityBack:CommandAction
 data object AccessibilityHome:CommandAction
 data object AccessibilityRecents:CommandAction
 data object OpenWifiSettings:CommandAction
 data object OpenBluetoothSettings:CommandAction
 data object OpenLocationSettings:CommandAction
 data object OpenAccessibilitySettings:CommandAction
 data object OpenAppSettings:CommandAction
 data object FlashlightOn:CommandAction
 data object FlashlightOff:CommandAction
 data class SetVolume(val percent:Int):CommandAction
 data class VolumeDelta(val direction:Int):CommandAction
 data class SetBrightness(val percent:Int):CommandAction
 enum class RingerMode{NORMAL,VIBRATE,SILENT}
 data class SetRingerMode(val mode:RingerMode):CommandAction
 data object BatteryStatus:CommandAction
 data object StorageSettings:CommandAction
 data object OpenNetworkSettings:CommandAction
 data object MediaPlayPause:CommandAction
 data object MediaNext:CommandAction
 data object MediaPrevious:CommandAction
 data object OpenDisplaySettings:CommandAction
 data class Dial(val number:String):CommandAction
 data class SendSms(val number:String?,val body:String):CommandAction
 data class OpenUrl(val url:String):CommandAction
 data object OpenCamera:CommandAction
 data object OpenCalendar:CommandAction
 data object OpenContacts:CommandAction
 data object OpenFiles:CommandAction
 data object OpenNotifications:CommandAction
 data object OpenDateSettings:CommandAction
 data object OpenTimeSettings:CommandAction
}