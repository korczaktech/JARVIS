package com.korczak.morok.core
sealed interface CommandAction {
 data object None: CommandAction
 data object OpenSettings: CommandAction
 data object OpenWifiSettings: CommandAction
 data object OpenBluetoothSettings: CommandAction
 data object OpenLocationSettings: CommandAction
 data object OpenAccessibilitySettings: CommandAction
 data object OpenAppSettings: CommandAction
 data object FlashlightOn: CommandAction
 data object FlashlightOff: CommandAction
 data class SetVolume(val percent:Int): CommandAction
 data class SetBrightness(val percent:Int): CommandAction
 data class Dial(val number:String): CommandAction
 data class SendSms(val number:String?,val body:String): CommandAction
 data class OpenUrl(val url:String): CommandAction
 data object OpenCamera: CommandAction
 data object OpenCalendar: CommandAction
 data object OpenContacts: CommandAction
 data object OpenFiles: CommandAction
 data object OpenNotifications: CommandAction
 data object OpenDateSettings: CommandAction
 data object OpenTimeSettings: CommandAction
}