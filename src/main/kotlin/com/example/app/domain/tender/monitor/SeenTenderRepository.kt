package com.example.app.domain.tender.monitor

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SeenTenderRepository : JpaRepository<SeenTender, Long> {
    @Query("SELECT s.purchaseNumber FROM SeenTender s WHERE s.subscription.id = :subscriptionId AND s.purchaseNumber IN :purchaseNumbers")
    fun findSeenNumbers(
        subscriptionId: Long,
        purchaseNumbers: Collection<String>,
    ): List<String>
}
