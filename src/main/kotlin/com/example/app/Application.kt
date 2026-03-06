package com.example.app

import com.example.app.common.email.EmailConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(EmailConfig::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
