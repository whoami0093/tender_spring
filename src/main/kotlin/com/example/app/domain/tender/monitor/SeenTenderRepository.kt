package com.example.app.domain.tender.monitor

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SeenTenderRepository : JpaRepository<SeenTender, Long> {

    @Query("SELECT s.purchaseNumber FROM SeenTender s WHERE s.subscription.id = :subscriptionId")
    fun findPurchaseNumbersBySubscriptionId(subscriptionId: Long): List<String>
}
