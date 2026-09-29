package com.korczak.morok.storage
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
class SecureStorage(context:Context){
private val preferences=EncryptedSharedPreferences.create("morok_secure",MasterKey.DEFAULT_MASTER_KEY_ALIAS,context,EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
fun put(key:String,value:String){preferences.edit().putString(key,value).apply()}
fun get(key:String):String?=preferences.getString(key,null)
fun remove(key:String){preferences.edit().remove(key).apply()}
}