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
    fun requestIncomingPayload(request: Transaction.Transfer.Request): Transaction.Transfer
    fun register(transferId: Id, diagram: State<Transaction.Transfer.Request>, tx: Tx)
}

interface OutgoingTransfer {
    fun confirmOutgoing(transferId: Id)
    fun requestOutgoingPayload(request: Transaction.Transfer.Request): Transaction.Transfer
    fun register(transferId: Id, diagram: State<Transaction.Transfer.Request>, tx: Tx)
}

sealed class TransferPayload {
    abstract val transferId: Id
    abstract val request: Transaction.Transfer.Request
    abstract fun validatedBy(userPinCode: PinCode): Boolean

    data class Secure(override val transferId: Id, private val code: PinCode, override val request: Transaction.Transfer.Request) : TransferPayload() {
        override fun validatedBy(userPinCode: PinCode): Boolean {
            return this.code.validatedBy(userPinCode)
        }
    }

    data class NotSecure(override val transferId: Id, override val request: Transaction.Transfer.Request) : TransferPayload() {
        override fun validatedBy(userPinCode: PinCode): Boolean {
            return true
        }
    }
}

sealed class TransferDiagram : State<Transaction.Transfer.Request> {

    data class Initial(private val request: Transaction.Transfer.Request) : State<Transaction.Transfer.Request> {
        override fun transition(): State<Transaction.Transfer.Request> {
            val outgoingPayload = request.from.requestOutgoingPayload(request)
            val incomingTransferRequest = IncomingTransferRequest(outgoingPayload.payload.transferId, request)
            return when (outgoingPayload.payload) {
                is TransferPayload.Secure -> {
                    val newState = WaitingForOutgoingConfirmation(incomingTransferRequest)
                    request.from.register(outgoingPayload.payload.transferId, newState, outgoingPayload.tx)
                    newState
                }
                is TransferPayload.NotSecure -> {
                    val newState = IncomingRequest(incomingTransferRequest)
                    request.from.register(outgoingPayload.payload.transferId, newState, outgoingPayload.tx)
                    newState.transition()
                    newState
                }
            }
        }
    }


    data class IncomingTransferRequest(val incomingTransferId: Id, val request: Transaction.Transfer.Request)
    data class CompleteTransferRequest(val outgoingTransferId: Id, val incomingTransferRequest: IncomingTransferRequest)


    data class WaitingForOutgoingConfirmation(private val transferRequest: IncomingTransferRequest) : State<Transaction.Transfer.Request> {
        override fun transition(): State<Transaction.Transfer.Request> {
            return IncomingRequest(transferRequest).transition()
        }
    }

    data class IncomingRequest(private val transferRequest: IncomingTransferRequest) : State<Transaction.Transfer.Request> {
        override fun transition(): State<Transaction.Transfer.Request> {
            val to = transferRequest.request.to
            val payload = to.requestIncomingPayload(transferRequest.request)
            val transferRequest1 = CompleteTransferRequest(payload.payload.transferId, transferRequest)
            return when (payload.payload) {
                is TransferPayload.Secure -> {
                    val newState = WaitingForIncomingConfirmation(transferRequest1)
                    to.register(payload.payload.transferId, newState, payload.tx)
                    newState
                }
                is TransferPayload.NotSecure -> {
                    val newState = PerformingActions(transferRequest1)
                    to.register(payload.payload.transferId, newState, payload.tx)
                    newState.transition()
                }
            }
        }
    }

    data class WaitingForIncomingConfirmation(private val transferRequest: CompleteTransferRequest) : State<Transaction.Transfer.Request> {
        override fun transition(): State<Transaction.Transfer.Request> {
            return PerformingActions(transferRequest).transition()
        }
    }

    data class PerformingActions(private val transferRequest: CompleteTransferRequest) : State<Transaction.Transfer.Request> {
        override fun transition(): State<Transaction.Transfer.Request> {
            val trRequest = transferRequest.incomingTransferRequest.request
            trRequest.from.confirmOutgoing(transferRequest.incomingTransferRequest.incomingTransferId)
            trRequest.to.confirmIncoming(transferRequest.outgoingTransferId)
            return Confirmed(transferRequest)
        }
    }

    data class Confirmed(val transferRequest: CompleteTransferRequest) : FinalState<Transaction.Transfer.Request>()
}