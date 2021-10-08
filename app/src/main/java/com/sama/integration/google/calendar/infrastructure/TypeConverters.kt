package com.sama.integration.google.calendar.infrastructure

import com.sama.integration.google.auth.domain.GoogleAccountId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class GoogleAccountIdWriter : Converter<GoogleAccountId, Long> {
    override fun convert(arg: GoogleAccountId) = arg.id
}

@ReadingConverter
class GoogleAccountIdReader : Converter<Long, GoogleAccountId> {
    override fun convert(arg: Long) = GoogleAccountId(arg)
}