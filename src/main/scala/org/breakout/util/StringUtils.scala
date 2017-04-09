package org.breakout.util

import java.security.MessageDigest

import com.typesafe.scalalogging.Logger

object StringUtils {

  private val log = Logger[StringUtils.type]

  implicit class StringImprovements(val string: String) {
    def hasValidSubject: Boolean = {
      val pattern = """^([A-Z0-9]{4})([A-Z0-9]{2})-""".r

      val matchedString = pattern.findAllIn(string)
      matchedString.isEmpty match {
        case true => false
        case false =>
          val matchedGroups = (0 to matchedString.groupCount).map(matchedString.group).toList

          matchedGroups match {
            case _ :: code :: checksum :: _ =>
              log.debug(s"code: $code; checksum: $checksum; subject: $string")

              val expectedChecksum = MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes("UTF-8"))
                .map("%02X".format(_)).mkString
                .substring(0, 2)

              checksum == expectedChecksum
            case _ => false
          }
      }
    }
  }

}