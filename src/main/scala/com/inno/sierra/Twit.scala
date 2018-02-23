package com.inno.sierra

import java.time._
import java.util.Date

case class Twit(id: Int, text: String,
                author: User, submitted: Date) {

  override def hashCode() = id

  override def equals(that: scala.Any): Boolean = that match {
    case that: Twit => this.id == that.id
    case _ => false
  }
}
