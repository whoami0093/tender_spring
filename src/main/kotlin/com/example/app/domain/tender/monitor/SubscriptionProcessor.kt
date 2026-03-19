package com.example.app.domain.tender.monitor

import com.example.app.common.email.EmailService
import com.example.app.domain.tender.source.TenderSourceRegistry
import com.example.app.domain.tender.subscription.SubscriptionRepository
import com.example.app.domain.tender.subscription.toFilters
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SubscriptionProcessor(
    private val subscriptionRepository: SubscriptionRepository,
    private val seenTenderRepository: SeenTenderRepository,
    private val registry: TenderSourceRegistry,
    private val emailService: EmailService,
    private val composer: TenderEmailComposer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun process(subscriptionId: Long) {
        val sub = subscriptionRepository.findByIdOrNull(subscriptionId) ?: return

        if (sub.lastCheckedAt == null) {
            sub.lastCheckedAt = Instant.now()
            sub.updatedAt = Instant.now()
            log.info("subscription={} baseline set, skipping first run", sub.id)
            return
        }

        val publishedAfter = sub.lastCheckedAt!!
        runCatching {
            val source = registry.get(sub.source)
            val fetched = withRetry { source.fetch(sub.toFilters(), publishedAfter) }

            val fetchedNumbers = fetched.map { it.purchaseNumber }
            val knownNumbers =
                if (fetchedNumbers.isEmpty()) {
                    emptySet()
                } else {
                    seenTenderRepository.findSeenNumbers(sub.id, fetchedNumbers).toSet()
                }
            val newTenders = fetched.filter { it.purchaseNumber !in knownNumbers }

            if (newTenders.isNotEmpty()) {
                seenTenderRepository.saveAll(newTenders.map { SeenTender.from(sub, it) })
                emailService.send(composer.compose(sub, newTenders))
                log.info("subscription={} source={} new={}", sub.id, sub.source, newTenders.size)
            } else {
                log.info("subscription={} source={} new=0", sub.id, sub.source)
            }

            val maxPublishedAt = fetched.mapNotNull { it.publishedAt }.maxOrNull()
            if (maxPublishedAt != null) {
                sub.lastCheckedAt = maxPublishedAt
            }
            sub.updatedAt = Instant.now()
        }.onFailure { ex ->
            log.error("Monitor failed for subscription={}", sub.id, ex)
            // subscription stays active, lastCheckedAt not updated
        }
    }

    private fun <T> withRetry(
        times: Int = 2,
        block: () -> T,
    ): T {
        repeat(times - 1) { runCatching { return block() } }
        return block()
    }
}
