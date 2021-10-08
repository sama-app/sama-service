package com.sama.auth.domain

import com.sama.users.domain.UserId
import java.time.Instant
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.relational.core.mapping.event.BeforeSaveCallback
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.stereotype.Repository

const val STATE_REGISTER = "register"
const val STATE_LINK_ACCOUNT = "link_account"

fun String.isRegisterState() = this == STATE_REGISTER
fun UserId.toLinkAccountOauth2State() = "${STATE_LINK_ACCOUNT}#$id"
fun String.isLinkAccountOauth2State() = startsWith("${STATE_LINK_ACCOUNT}#")
fun String.toLinkAccountOauth2UserId(): UserId {
    require(isLinkAccountOauth2State())
    return UserId(substringAfter("#").toLong())
}

@Table("oauth2_state")
data class Oauth2State(@Id var key: String? = null, val value: String = "", val createdAt: Instant = Instant.now()) {
    companion object {
        private val tokenGenerator = KeyGenerators.string()
    }
}

@Repository
interface Oauth2StateRepository : CrudRepository<Oauth2State, String> {
    fun findByKey(key: String): Oauth2State?

    @Modifying
    @Query("DELETE FROM sama.oauth2_state WHERE created_at < :createdAt")
    fun deleteByCreatedAtLessThan(@Param("createdAt") createdAt: Instant)
}

@Configuration
class Config {
    @Bean
    fun oauth2StateKeyGenerator(): StringKeyGenerator = KeyGenerators.string()

    @Bean
    fun keySetter(oauth2StateKeyGenerator: StringKeyGenerator) = BeforeSaveCallback<Oauth2State>
    { aggregate, _ ->
        if (aggregate.key == null) {
            aggregate.key = oauth2StateKeyGenerator.generateKey()
        }
        aggregate
    }
}