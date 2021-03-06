package com.sama.integration.google

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.sama.common.DomainIntegrityException
import com.sama.common.HasReason
import com.sama.users.domain.UserId

// https://developers.google.com/calendar/api/guides/errors
fun translatedGoogleException(ex: Throwable): Throwable {
    if (ex is GoogleJsonResponseException) {
        return when (ex.statusCode) {
            400 -> {
                if (ex.details?.errors?.find { it?.reason == "pushNotSupportedForRequestedResource" } != null) {
                    GoogleChannelCreationUnsupportedException(ex)
                } else {
                    GoogleBadRequestException(ex)
                }
            }
            401 -> GoogleInvalidCredentialsException(ex)
            403 -> {
                if (ex.details?.errors?.find { it?.reason == "rateLimitExceeded" } != null) {
                    GoogleApiRateLimitException(ex)
                } else {
                    GoogleInsufficientPermissionsException(ex)
                }
            }
            404 -> GoogleNotFoundException(ex)
            429 -> GoogleApiRateLimitException(ex)
            410 -> GoogleSyncTokenInvalidatedException(ex)
            in (500..599) -> GoogleInternalServerException(ex)
            else -> GoogleUnhandledException(ex)
        }
    }
    return ex
}

sealed class GoogleException(override val message: String, originalEx: Exception?) :
    RuntimeException(message, originalEx)

class GoogleBadRequestException(originalEx: Exception) :
    GoogleException("Bad request", originalEx)

class GoogleChannelCreationUnsupportedException(originalEx: Exception) :
    GoogleException("Channel creation unsupported", originalEx)

class GoogleInvalidCredentialsException(originalEx: Exception?) :
    GoogleException("User Google credentials invalid", originalEx), HasReason {
    override val reason: String = "google_invalid_credentials"
}

class GoogleInsufficientPermissionsException(originalEx: Exception) :
    GoogleException("Insufficient permissions to access Google APIs: ${originalEx.message}", originalEx),
    HasReason {
    override val reason: String = "google_insufficient_permissions"
}

class GoogleNotFoundException(originalEx: GoogleJsonResponseException) : GoogleException("Not Found", originalEx)

class GoogleSyncTokenInvalidatedException(originalEx: GoogleJsonResponseException) :
    GoogleException("Sync token invalidated", originalEx)

class GoogleApiRateLimitException(originalEx: GoogleJsonResponseException?) :
    GoogleException("Rate limited: ${originalEx?.message}", originalEx)

class GoogleInternalServerException(originalEx: GoogleJsonResponseException) :
    GoogleException("Internal error: ${originalEx.message}", originalEx)

class GoogleUnhandledException(originalEx: GoogleJsonResponseException) :
    GoogleException("Bad request to Google Calendar: ${originalEx.message}", originalEx)

class NoPrimaryGoogleAccountException(userId: UserId) :
    DomainIntegrityException("no_primary_google_account", "Could not find primary Google Account for User#${userId.id}")