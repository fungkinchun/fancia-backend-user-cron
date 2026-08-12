package com.fancia.backend.user.core.exception

import com.fancia.backend.shared.common.core.exception.DomainException
import java.util.UUID

class SmartMatchBatchFailedException(
    val userId: UUID,
    cause: Throwable? = null,
    title: String = "Smart Match Batch Failed",
    message: String = buildMessage(userId, cause),
    errorCode: String = "SMART_MATCH_BATCH_FAILED",
) : DomainException(title, message, errorCode) {
    init {
        if (cause != null) {
            initCause(cause)
        }
    }

    val rootExceptionType: String
        get() = (cause ?: this)::class.java.name

    companion object {
        private fun buildMessage(userId: UUID, cause: Throwable?): String {
            val causeType = cause?.let { it::class.java.name }
            val causeMessage = cause?.message
            return buildString {
                append("Failed to generate Smart Match batch for user $userId")
                if (causeType != null) {
                    append(": $causeType")
                }
                if (!causeMessage.isNullOrBlank()) {
                    append(" — $causeMessage")
                }
            }
        }
    }
}

data class SmartMatchBatchFailure(
    val userId: UUID,
    val exceptionType: String,
    val errorCode: String?,
    val message: String?,
)

class SmartMatchBatchJobFailedException(
    val failures: List<SmartMatchBatchFailure>,
    title: String = "Smart Match Batch Job Failed",
    message: String = buildMessage(failures),
    errorCode: String = "SMART_MATCH_BATCH_JOB_FAILED",
) : DomainException(title, message, errorCode) {
    val failedUserIds: List<UUID>
        get() = failures.map { it.userId }

    companion object {
        private fun buildMessage(failures: List<SmartMatchBatchFailure>): String {
            val userIds = failures.map { it.userId }.joinToString(prefix = "[", postfix = "]")
            val details = failures.joinToString(separator = "; ") { failure ->
                buildString {
                    append(failure.userId)
                    append("→")
                    append(failure.exceptionType)
                    if (!failure.errorCode.isNullOrBlank()) {
                        append(" (")
                        append(failure.errorCode)
                        append(")")
                    }
                    if (!failure.message.isNullOrBlank()) {
                        append(": ")
                        append(failure.message)
                    }
                }
            }
            return "Smart Match batch generation failed for ${failures.size} user(s) $userIds: $details"
        }
    }
}
