package com.example.app.domain.tender.monitor

import com.example.app.common.email.EmailService
import com.example.app.domain.tender.source.TenderSourceRegistry
import com.example.app.domain.tender.subscription.Subscription
import com.example.app.domain.tender.subscription.SubscriptionRepository
import com.example.app.domain.tender.subscription.SubscriptionStatus
import com.example.app.domain.tender.subscription.toFilters
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class MonitorService(
    private val subscriptionRepository: SubscriptionRepository,
    private val seenTenderRepository: SeenTenderRepository,
    private val registry: TenderSourceRegistry,
    private val emailService: EmailService,
    private val composer: TenderEmailComposer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun runCycle() {
        val subscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE)
        log.info("Monitor cycle started: {} active subscriptions", subscriptions.size)
        subscriptions.forEach { processSubscription(it) }
    }

    @Transactional
    fun processSubscription(sub: Subscription) {
        if (sub.lastCheckedAt == null) {
            // first run — set baseline, do not notify
            sub.lastCheckedAt = Instant.now()
            sub.updatedAt = Instant.now()
            log.info("subscription={} baseline set, skipping first run", sub.id)
            return
        }

        val publishedAfter = sub.lastCheckedAt!!
        runCatching {
            val source = registry.get(sub.source)
            val fetched = withRetry { source.fetch(sub.toFilters(), publishedAfter) }

            val knownNumbers = seenTenderRepository
                .findPurchaseNumbersBySubscriptionId(sub.id)
                .toSet()
            val newTenders = fetched.filter { it.purchaseNumber !in knownNumbers }

            if (newTenders.isNotEmpty()) {
                seenTenderRepository.saveAll(newTenders.map { SeenTender.from(sub, it) })
                emailService.send(composer.compose(sub, newTenders))
                log.info("subscription={} source={} new={}", sub.id, sub.source, newTenders.size)
            } else {
                log.debug("subscription={} no new tenders", sub.id)
            }

            sub.lastCheckedAt = Instant.now()
            sub.updatedAt = Instant.now()
        }.onFailure { ex ->
            log.error("Monitor failed for subscription={}", sub.id, ex)
            // subscription stays active, lastCheckedAt not updated
        }
    }

    private fun <T> withRetry(times: Int = 2, delayMs: Long = 5000, block: () -> T): T {
        repeat(times - 1) {
            runCatching { return block() }.onFailure { Thread.sleep(delayMs) }
        }
        return block()
    }
}
