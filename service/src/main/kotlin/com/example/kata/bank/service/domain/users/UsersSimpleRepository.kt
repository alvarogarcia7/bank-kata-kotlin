package com.example.kata.bank.service.domain.users

import com.example.kata.bank.service.infrastructure.storage.InMemorySimpleRepository

class UsersSimpleRepository : InMemorySimpleRepository<User>(mutableListOf())

