package com.fancia.backend.user.core.job

import com.fancia.backend.user.core.exception.SmartMatchBatchFailedException
import com.fancia.backend.user.core.exception.SmartMatchBatchFailure
import com.fancia.backend.user.core.exception.SmartMatchBatchJobFailedException
import com.fancia.backend.user.core.repository.UserRepository
import com.fancia.backend.user.core.service.SmartMatchBatchService
import com.fancia.backend.shared.common.core.exception.DomainException
import com.fancia.backend.shared.user.core.enums.AccountStatus
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GenerateSmartMatchBatchJob(
    private val userRepository: UserRepository,
    private val smartMatchBatchService: SmartMatchBatchService,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val users = userRepository.findByStatus(AccountStatus.ACTIVE)
        log.info(
            "Starting Smart Match batch generation for {} active users (batchSize={})",
            users.size,
            SmartMatchBatchService.BATCH_SIZE,
        )

        var usersTouched = 0
        var rowsUpserted = 0
        val failures = mutableListOf<SmartMatchBatchFailure>()

        for (user in users) {
            val userId = user.id
            if (userId == null) {
                log.warn("Skipping active user with null id")
                continue
            }
            try {
                val written = smartMatchBatchService.generateForUser(userId)
                if (written > 0) {
                    usersTouched++
                    rowsUpserted += written
                }
            } catch (ex: Exception) {
                failures += recordFailure(userId, ex)
            }
        }

        log.info(
            "Finished Smart Match batch generation: upserted={} across usersTouched={}, scanned={}, failedUserIds={}",
            rowsUpserted,
            usersTouched,
            users.size,
            failures.map { it.userId },
        )

        if (failures.isNotEmpty()) {
            log.error(
                "Smart Match batch failures by user: {}",
                failures.joinToString(separator = " | ") {
                    "userId=${it.userId} exceptionType=${it.exceptionType} errorCode=${it.errorCode} message=${it.message}"
                },
            )
            throw SmartMatchBatchJobFailedException(failures)
        }
    }

    private fun recordFailure(userId: UUID, ex: Exception): SmartMatchBatchFailure {
        val domainEx = when (ex) {
            is DomainException -> ex
            else -> SmartMatchBatchFailedException(userId = userId, cause = ex)
        }
        val rootType = when (domainEx) {
            is SmartMatchBatchFailedException -> domainEx.rootExceptionType
            else -> domainEx::class.java.name
        }

        log.error(
            "Failed to generate Smart Match batch for userId={} exceptionType={} errorCode={} message={}",
            userId,
            rootType,
            domainEx.errorCode,
            domainEx.message,
            domainEx,
        )

        return SmartMatchBatchFailure(
            userId = userId,
            exceptionType = rootType,
            errorCode = domainEx.errorCode,
            message = domainEx.message,
        )
    }
}
