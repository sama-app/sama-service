package com.sama.connection.application

data class CreateConnectionRequestCommand(val recipientEmail: String)
data class RemoveUserConnectionCommand(val recipientEmail: String)