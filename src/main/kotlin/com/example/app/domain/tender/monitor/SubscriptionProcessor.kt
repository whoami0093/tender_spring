package com.example.app.domain.tender.monitor

import com.example.app.common.email.EmailService
import com.example.app.domain.tender.source.Tender
import com.example.app.domain.tender.source.TenderSourceRegistry
import com.example.app.domain.tender.subscription.SubscriptionRepository
import com.example.app.domain.tender.subscription.toFilters
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Suppress("LongMethod")
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
        val subId = sub.id.toString()
        val sourceName = sub.source

        val sample = Timer.start(meterRegistry)
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
            val newTenders: List<Tender> = fetched.filter { it.purchaseNumber !in knownNumbers }

            if (newTenders.isNotEmpty()) {
                seenTenderRepository.saveAll(newTenders.map { SeenTender.from(sub, it) })
                emailService.send(composer.compose(sub, newTenders))
                log.info("subscription={} source={} new={}", sub.id, sub.source, newTenders.size)
                meterRegistry
                    .counter(
                        "monitor.tenders.found",
                        "subscription_id",
                        subId,
                        "source",
                        sourceName,
                    ).increment(newTenders.size.toDouble())
                meterRegistry
                    .counter(
                        "monitor.emails.sent",
                        "subscription_id",
                        subId,
                    ).increment()
                meterRegistry
                    .counter(
                        "monitor.cycles",
                        "subscription_id",
                        subId,
                        "source",
                        sourceName,
                        "result",
                        "found",
                    ).increment()
            } else {
                log.info("subscription={} source={} new=0", sub.id, sub.source)
                meterRegistry
                    .counter(
                        "monitor.cycles",
                        "subscription_id",
                        subId,
                        "source",
                        sourceName,
                        "result",
                        "empty",
                    ).increment()
            }

            val maxPublishedAt = fetched.mapNotNull { it.publishedAt }.maxOrNull()
            if (maxPublishedAt != null) {
                sub.lastCheckedAt = maxPublishedAt
            }
            sub.updatedAt = Instant.now()
        }.onFailure { ex ->
            log.error("Monitor failed for subscription={}", sub.id, ex)
            meterRegistry
                .counter(
                    "monitor.api.errors",
                    "subscription_id",
                    subId,
                    "source",
                    sourceName,
                ).increment()
            meterRegistry
                .counter(
                    "monitor.cycles",
                    "subscription_id",
                    subId,
                    "source",
                    sourceName,
                    "result",
                    "error",
                ).increment()
        }
        val timer =
            meterRegistry.timer(
                "monitor.cycle.duration",
                "subscription_id",
                subId,
                "source",
                sourceName,
            )
        sample.stop(timer)
    }

    private fun <T> withRetry(
        times: Int = 2,
        block: () -> T,
    ): T {
        repeat(times - 1) { runCatching { return block() } }
        return block()
    }
}
