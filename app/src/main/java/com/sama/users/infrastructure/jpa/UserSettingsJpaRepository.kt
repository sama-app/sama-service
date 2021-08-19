package com.sama.users.infrastructure.jpa

import com.sama.common.DomainRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@DomainRepository
@Repository
interface UserSettingsJpaRepository : JpaRepository<UserSettingsEntity, Long>