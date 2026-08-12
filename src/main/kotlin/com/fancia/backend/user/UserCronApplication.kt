package com.fancia.backend.user

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import kotlin.system.exitProcess

@EntityScan(
    basePackages = [
        "com.fancia.backend.shared.user.core.entity",
        "com.fancia.backend.shared.common.core.entity",
        "com.fancia.backend.shared.common.tag.core.entity",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.fancia.backend.user.core.repository",
    ],
)
@SpringBootApplication(scanBasePackages = ["com.fancia.backend.user"])
class UserCronApplication

fun main(args: Array<String>) {
    val context = runApplication<UserCronApplication>(*args)
    exitProcess(SpringApplication.exit(context))
}
