package com.sama.comms.application

import com.sama.common.ApplicationService
import com.sama.comms.domain.CommsUserRepository
import org.springframework.stereotype.Service

@ApplicationService
@Service
class ConnectionCommsApplicationService(private val commsUserRepository: CommsUserRepository) {


}