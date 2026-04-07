package com.example.app.domain.tender

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TenderRepository :
    JpaRepository<Tender, Long>,
    JpaSpecificationExecutor<Tender> {
    @Query("SELECT t.purchaseNumber FROM Tender t WHERE t.purchaseNumber IN :numbers")
    fun findExistingPurchaseNumbers(numbers: Collection<String>): List<String>
}
