package com.example.app.domain.tender.monitor

import com.example.app.domain.tender.subscription.SubscriptionRepository
import com.example.app.domain.tender.subscription.SubscriptionStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MonitorService(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionProcessor: SubscriptionProcessor,
    @Value("\${zakupki.monitor.between-subscriptions-ms:1000}")
    private val betweenSubscriptionsMs: Long = 1_000,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun runCycle() {
        val subIds = subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE).map { it.id }
        log.info("Monitor cycle started: {} active subscriptions", subIds.size)
        subIds.forEachIndexed { index, id ->
            if (index > 0 && betweenSubscriptionsMs > 0) Thread.sleep(betweenSubscriptionsMs)
            subscriptionProcessor.process(id)
        }
    }
}
