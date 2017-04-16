package org.breakout.connector.backend

case class BackendPayment(amount: BigDecimal,
                          fidorId: Long)

case class BackendInvoice(id: Long,
                          amount: BigDecimal)
