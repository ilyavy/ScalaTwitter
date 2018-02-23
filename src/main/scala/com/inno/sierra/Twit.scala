package com.inno.sierra

import java.time._
import java.util.{Calendar, Date}

import scala.collection.mutable.Set

/** The representation of Twit entity */
case class Twit private (id: Int, text: String,
                author: User, submitted: Date) {

  override def hashCode() = id

  override def equals(that: scala.Any): Boolean = that match {
    case that: Twit => this.id == that.id
    case _ => false
  }
}

object Twit {
  private var lastId: Int = 0
  private val twits = Set[Twit]()

  /**
    * Creates and returns a new Twit,
    * id is unique and generated automatically,
    * submitted date is set with the current date.
    * @param text
    * @param author
    * @return
    */
  def createTwit(text: String, author: User): Twit = {
    lastId += 1
    val twit = Twit(lastId, text, author,
            Calendar.getInstance().getTime())
    twits += twit
    twit
  }

  /**
    * Returns the list of the twits
    * @return the list of the twits
    */
  def getUsers(): Set[Twit] = twits
}
