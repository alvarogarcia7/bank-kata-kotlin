package com.example.kata.bank.service.domain.transfers

import com.example.kata.bank.service.FinalState
import com.example.kata.bank.service.State
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.accounts.PinCode
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Tx

interface SecureIncomingTransfer {
    fun userConfirmIncoming(transferId: Id, userPinCode: PinCode)
}

interface SecureOutgoingTransfer {
    fun userConfirmOutgoing(transferId: Id, userPinCode: PinCode)
}

interface IncomingTransfer {
    fun confirmIncoming(transferId: Id)
    fun requestIncomingPayload(request: Transaction.Transfer.TransferRequest): Transaction.Transfer
    fun register(transferId: Id, diagram: State<Transaction.Transfer.TransferRequest>, tx: Tx)
}

interface OutgoingTransfer {
    fun confirmOutgoing(transferId: Id)
    fun requestOutgoingPayload(request: Transaction.Transfer.TransferRequest): Transaction.Transfer
    fun register(transferId: Id, diagram: State<Transaction.Transfer.TransferRequest>, tx: Tx)
}

sealed class TransferPayload {
    abstract val transferId: Id
    abstract val request: Transaction.Transfer.TransferRequest
    abstract fun validatedBy(userPinCode: PinCode): Boolean

    data class SecureTransferPayload(override val transferId: Id, private val code: PinCode, override val request: Transaction.Transfer.TransferRequest) : TransferPayload() {
        override fun validatedBy(userPinCode: PinCode): Boolean {
            return this.code.validatedBy(userPinCode)
        }
    }

    data class NotSecureTransferPayload(override val transferId: Id, override val request: Transaction.Transfer.TransferRequest) : TransferPayload() {
        override fun validatedBy(userPinCode: PinCode): Boolean {
            return true
        }
    }
}

sealed class TransferDiagram : State<Transaction.Transfer.TransferRequest> {

    data class Initial(private val transferRequest: Transaction.Transfer.TransferRequest) : State<Transaction.Transfer.TransferRequest> {
        override fun transition(): State<Transaction.Transfer.TransferRequest> {
            val outgoingPayload = transferRequest.from.requestOutgoingPayload(transferRequest)
            val incomingTransferRequest = IncomingTransferRequest(outgoingPayload.payload.transferId, transferRequest)
            return when (outgoingPayload.payload) {
                is TransferPayload.SecureTransferPayload -> {
                    val newState = WaitingForOutgoingConfirmation(incomingTransferRequest)
                    transferRequest.from.register(outgoingPayload.payload.transferId, newState, outgoingPayload.tx)
                    newState
                }
                is TransferPayload.NotSecureTransferPayload -> {
                    val newState = IncomingRequest(incomingTransferRequest)
                    transferRequest.from.register(outgoingPayload.payload.transferId, newState, outgoingPayload.tx)
                    newState.transition()
                    newState
                }
            }
        }
    }


    data class IncomingTransferRequest(val incomingTransferId: Id, val TransferRequest: Transaction.Transfer.TransferRequest)
    data class CompleteTransferRequest(val outgoingTransferId: Id, val incomingTransferRequest: IncomingTransferRequest)


    data class WaitingForOutgoingConfirmation(private val transferRequest: IncomingTransferRequest) : State<Transaction.Transfer.TransferRequest> {
        override fun transition(): State<Transaction.Transfer.TransferRequest> {
            return IncomingRequest(transferRequest).transition()
        }
    }

    data class IncomingRequest(private val transferRequest: IncomingTransferRequest) : State<Transaction.Transfer.TransferRequest> {
        override fun transition(): State<Transaction.Transfer.TransferRequest> {
            val to = transferRequest.TransferRequest.to
            val payload = to.requestIncomingPayload(transferRequest.TransferRequest)
            val transferRequest1 = CompleteTransferRequest(payload.payload.transferId, transferRequest)
            return when (payload.payload) {
                is TransferPayload.SecureTransferPayload -> {
                    val newState = WaitingForIncomingConfirmation(transferRequest1)
                    to.register(payload.payload.transferId, newState, payload.tx)
                    newState
                }
                is TransferPayload.NotSecureTransferPayload -> {
                    val newState = PerformingActions(transferRequest1)
                    to.register(payload.payload.transferId, newState, payload.tx)
                    newState.transition()
                }
            }
        }
    }

    data class WaitingForIncomingConfirmation(private val transferRequest: CompleteTransferRequest) : State<Transaction.Transfer.TransferRequest> {
        override fun transition(): State<Transaction.Transfer.TransferRequest> {
            return PerformingActions(transferRequest).transition()
        }
    }

    data class PerformingActions(private val transferRequest: CompleteTransferRequest) : State<Transaction.Transfer.TransferRequest> {
        override fun transition(): State<Transaction.Transfer.TransferRequest> {
            val trRequest = transferRequest.incomingTransferRequest.TransferRequest
            trRequest.from.confirmOutgoing(transferRequest.incomingTransferRequest.incomingTransferId)
            trRequest.to.confirmIncoming(transferRequest.outgoingTransferId)
            return Confirmed(transferRequest)
        }
    }

    data class Confirmed(val transferRequest: CompleteTransferRequest) : FinalState<Transaction.Transfer.TransferRequest>()
}