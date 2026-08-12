package com.fancia.backend.user.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app")
class ApplicationProperties {
    var applicationName: String? = null
}
