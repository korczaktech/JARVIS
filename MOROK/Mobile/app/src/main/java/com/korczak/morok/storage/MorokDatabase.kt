package com.korczak.morok.storage
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(entities=[CommandEntity::class],version=1,exportSchema=false)
abstract class MorokDatabase:RoomDatabase(){
abstract fun commandDao():CommandDao
companion object{
@Volatile private var instance:MorokDatabase?=null
fun get(context:Context):MorokDatabase=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,MorokDatabase::class.java,"morok.db").build().also{instance=it}}
}}