package com.sama.common

import io.sentry.spring.tracing.SentrySpan

@SentrySpan
annotation class ApplicationService
@SentrySpan
annotation class DomainRepository
@SentrySpan
annotation class View
annotation class AggregateRoot
annotation class Factory
annotation class DomainEntity
annotation class DomainService
annotation class ValueObject
