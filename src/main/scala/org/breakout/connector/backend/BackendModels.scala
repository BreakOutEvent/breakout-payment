package org.breakout.connector.backend

case class BackendPayment(amount: BigDecimal,
                          fidorId: Long,
                          date: Option[Long])

case class BackendInvoice(id: Long,
                          amount: BigDecimal)
