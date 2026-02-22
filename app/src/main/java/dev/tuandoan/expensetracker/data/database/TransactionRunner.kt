package dev.tuandoan.expensetracker.data.database

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRunner {
    suspend fun <R> runInTransaction(block: suspend () -> R): R
}

@Singleton
class RoomTransactionRunner
    @Inject
    constructor(
        private val database: AppDatabase,
    ) : TransactionRunner {
        override suspend fun <R> runInTransaction(block: suspend () -> R): R = database.withTransaction { block() }
    }
