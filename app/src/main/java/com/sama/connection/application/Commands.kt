package com.sama.connection.application


data class CreateConnectionRequestCommand(val recipientEmail: String)

sealed class ApproveConnectionCommand
sealed class RejectConnectionCommand