package com.korczak.morok.core
data class Command(val id: String, val rawText: String, val source: CommandSource, val createdAtEpochMs: Long)
enum class CommandSource { VOICE, TEXT, BUTTON }
