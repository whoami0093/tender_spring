package com.example.app.domain.tender.subscription

import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findAllByStatus(status: SubscriptionStatus): List<Subscription>
}
