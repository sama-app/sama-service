package com.sama

import org.dmfs.rfc5545.recur.RecurrenceRule
import org.junit.jupiter.api.Test


class KitchenSink {

    @Test
    fun test() {
        val rule = "FREQ=DAILY;INTERVAL=2"
        val recur = RecurrenceRule(rule)
        println(recur)
    }
}