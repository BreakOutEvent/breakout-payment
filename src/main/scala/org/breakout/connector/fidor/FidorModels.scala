package org.breakout.connector.fidor


case class FidorUser(id: String,
                     email: String,
                     last_sign_in_at: Option[String],
                     created_at: String,
                     updated_at: String)


case class FidorTransaction(id: String,
                            account_id: String,
                            transaction_type: String,
                            subject: String,
                            amount: Int,
                            booking_code: Option[String],
                            booking_date: String,
                            value_date: Option[String],
                            return_transaction_id: Option[String],
                            created_at: String,
                            updated_at: String,
                            currency: Option[String],
                            transaction_type_details: FidorTransactionTypeDetails)

case class FidorTransactionTypeDetails(sepa_credit_transfer_id: Option[String],
                                       remote_account_id: Option[String],
                                       internal_transfer_id: Option[String],
                                       remote_bic: Option[String],
                                       remote_iban: Option[String],
                                       remote_name: Option[String],
                                       remote_nick: Option[String],
                                       receiver: Option[String],
                                       remote_subject: Option[String])

case class FidorCollection(total_pages: Int,
                           current_page: Int,
                           current_entries: Int,
                           per_page: Int,
                           total_entries: Int)

case class FidorTransactions(data: Seq[FidorTransaction],
                             collection: FidorCollection)