package dev.brandonvargas.conceptgames.ui.components

import androidx.annotation.StringRes
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.data.gemini.GeminiException
import dev.brandonvargas.conceptgames.domain.NoSourcesException
import dev.brandonvargas.conceptgames.engine.validate.InvalidPackException
import java.io.IOException

/** A user-facing explanation of a failure, and whether fixing the API key is the way out. */
data class ErrorInfo(@StringRes val title: Int, @StringRes val message: Int, val fixKey: Boolean = false)

fun Throwable.toErrorInfo(): ErrorInfo = when (this) {
    is GeminiException.MissingKey -> ErrorInfo(R.string.error_missing_key_title, R.string.error_missing_key, fixKey = true)
    is GeminiException.InvalidKey -> ErrorInfo(R.string.error_invalid_key_title, R.string.error_invalid_key, fixKey = true)
    is GeminiException.ApiDisabled -> ErrorInfo(R.string.error_api_disabled_title, R.string.error_api_disabled, fixKey = true)
    is GeminiException.UnsupportedRegion -> ErrorInfo(R.string.error_region_title, R.string.error_region)
    is GeminiException.Quota -> ErrorInfo(R.string.error_quota_title, R.string.error_quota)
    is GeminiException.NotOnFreeTier -> ErrorInfo(R.string.error_not_free_title, R.string.error_not_free)
    is GeminiException.ModelNotFound -> ErrorInfo(R.string.error_model_title, R.string.error_model)
    is GeminiException.Overloaded -> ErrorInfo(R.string.error_overloaded_title, R.string.error_overloaded)
    is GeminiException.Blocked -> ErrorInfo(R.string.error_blocked_title, R.string.error_blocked)
    is GeminiException.Network, is IOException -> ErrorInfo(R.string.error_network_title, R.string.error_network)
    is InvalidPackException -> ErrorInfo(R.string.error_invalid_pack_title, R.string.error_invalid_pack)
    is NoSourcesException -> ErrorInfo(R.string.error_no_sources_title, R.string.error_no_sources)
    else -> ErrorInfo(R.string.error_generic_title, R.string.error_generic)
}
