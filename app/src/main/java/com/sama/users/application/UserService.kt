package com.sama.users.application

import com.sama.users.domain.UserId

interface UserService {
    fun me(): UserPublicDTO
    fun find(userId: UserId): UserPublicDTO
    fun findAll(userIds: Collection<UserId>): Map<UserId, UserPublicDTO>
    fun register(command: RegisterUserCommand): UserId
    fun updatePublicDetails(userId: UserId, command: UpdateUserPublicDetailsCommand): Boolean
}