package com.inno.sierra

class User(id: Int, email: String,
           nickname: String, password: String) {

  override def hashCode() = id

  override def equals(that: scala.Any): Boolean = that match {
    case that: Twit => this.id == that.id
    case _ => false
  }
}
