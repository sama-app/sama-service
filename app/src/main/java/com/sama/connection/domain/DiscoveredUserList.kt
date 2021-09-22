package com.sama.connection.domain

import com.sama.users.domain.UserId

data class DiscoveredUserList(val userId: UserId, val discoveredUsers: Set<UserId>) {

    fun addDiscoveredUsers(toAdd: Set<UserId>): Pair<DiscoveredUserList, Set<UserId>> {
        val newUsers = toAdd.minus(discoveredUsers).minus(userId) // exclude self
        return copy(discoveredUsers = discoveredUsers.plus(newUsers)) to newUsers
    }
}