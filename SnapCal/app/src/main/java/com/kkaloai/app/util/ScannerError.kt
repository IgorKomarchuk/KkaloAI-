package com.kkaloai.app.util

import androidx.annotation.StringRes
import com.kkaloai.app.R
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException

sealed class ScannerError(@StringRes val messageRes: Int) {
    data object RateLimited : ScannerError(R.string.err_rate_limited)
    data object Network : ScannerError(R.string.err_network)
    data object Quota : ScannerError(R.string.err_quota)
    data object ParseFailure : ScannerError(R.string.err_parse_failure)
    data object Timeout : ScannerError(R.string.err_timeout)
    data object NotFound : ScannerError(R.string.err_not_found)
    data object Unknown : ScannerError(R.string.err_unknown)

    companion object {
        fun fromException(e: Throwable): ScannerError {
            if (e is CancellationException) throw e

            val msg = (e.message ?: "").lowercase()
            return when {
                e is UnknownHostException -> Network
                e is IOException && msg.contains("timeout") -> Timeout
                e is IOException -> Network
                msg.contains("quota") && msg.contains("free_tier") -> Quota
                msg.contains("quota") -> Quota
                msg.contains("rate") && msg.contains("limit") -> RateLimited
                msg.contains("429") -> RateLimited
                msg.contains("resource_exhausted") -> RateLimited
                msg.contains("503") || msg.contains("unavailable") -> RateLimited
                msg.contains("json") || msg.contains("parse") -> ParseFailure
                else -> Unknown
            }
        }
    }
}
