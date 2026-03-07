package com.example.app.domain.tender.subscription

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController(private val service: SubscriptionService) {

    @GetMapping
    fun findAll(): List<SubscriptionResponse> = service.findAll()

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): SubscriptionResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: SubscriptionRequest): SubscriptionResponse = service.create(req)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: SubscriptionRequest): SubscriptionResponse =
        service.update(id, req)

    @PatchMapping("/{id}/status")
    fun updateStatus(@PathVariable id: Long, @RequestBody req: SubscriptionStatusRequest): SubscriptionResponse =
        service.updateStatus(id, req.status)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
