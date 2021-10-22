package com.sama.integration.google.auth.application

import com.sama.integration.google.auth.domain.GoogleAccountPublicId

data class GoogleUserInfo(val email: String, val fullName: String?)

data class GoogleIntegrationsDTO(val linkedAccounts: List<GoogleAccountDTO>)

data class GoogleAccountDTO(val id: GoogleAccountPublicId, val email: String)