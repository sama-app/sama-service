package com.sama.users.domain

import com.sama.common.DomainRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@DomainRepository
@Repository
interface UserSettingsRepository : JpaRepository<UserSettingsEntity, UserId>