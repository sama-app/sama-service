package com.sama.users.infrastructure.jpa

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.UserSettingsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@DomainRepository
@Repository
interface UserSettingsJpaRepository : JpaRepository<UserSettingsEntity, UserId>