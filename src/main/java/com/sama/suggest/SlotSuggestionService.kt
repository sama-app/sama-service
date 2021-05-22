package com.sama.suggest

interface SlotSuggestionService {

    fun suggest()

    fun markAccepted()

    fun markRejected()
}