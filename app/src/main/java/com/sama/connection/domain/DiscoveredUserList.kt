package com.sama.connection.domain

import com.sama.users.domain.UserId

data class DiscoveredUserList(val userId: UserId, val discoveredUsers: Set<UserId>) {
    fun addDiscoveredUsers(newUsers: Collection<UserId>): DiscoveredUserList {
        val newDiscoveredUsers = discoveredUsers.plus(newUsers).minus(userId) // exclude self
        return copy(discoveredUsers = newDiscoveredUsers)
    }
}