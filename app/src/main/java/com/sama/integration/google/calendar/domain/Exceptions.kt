package com.sama.integration.google.calendar.domain

import java.util.UUID

class ChannelClosedException(channelId: UUID) : RuntimeException("Channel#$channelId is already closed")