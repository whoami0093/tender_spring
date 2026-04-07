package com.example.app.domain.tender

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface TenderRepository :
    JpaRepository<Tender, Long>,
    JpaSpecificationExecutor<Tender>
