package com.example.app.domain.tender.monitor

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MonitorScheduler(
    private val monitorService: MonitorService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${zakupki.monitor.interval-minutes:30}m")
    fun run() {
        log.debug("MonitorScheduler triggered")
        monitorService.runCycle()
    }
}
