package com.inno.sierra

import java.security.MessageDigest

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue

import scala.collection.mutable.Set
import org.json4s.MappingException

/** The representation of User entity */
case class User private (id: Int, email: String,
           nickname: String, password: String) {

  override def hashCode() = id

  override def equals(that: scala.Any): Boolean = that match {
    case that: Twit => this.id == that.id
    case _ => false
  }
}


object User {
  implicit val jsonFormats = DefaultFormats

  private var lastId: Int = 0
  private val users = Set[User]()

  /** The helper method */
  private def createUser(userNoId: UserNoId): User = {
    if (users.exists(_.email == userNoId.email)) {
      throw new IllegalArgumentException("email should be unique")
    }

    lastId += 1
    val id = lastId
    val password = MessageDigest.getInstance("MD5")
          .digest(userNoId.password.getBytes())
          .map("%02X".format(_)).mkString

    val user = User(id, userNoId.email,
          userNoId.nickname, password)
    users += user
    user
  }

  /**
    * Creates a new User and adds it to the list of Users.
    * If the User with the same email already exists,
    * the exception will be thrown.
    * @param email  email, should be unique
    * @param nickname nickname, may be not unique
    * @param password password
    * @throws java.lang.IllegalArgumentException
    *               in the case of not unique email
    * @return registered User
    */
  def createUser(email: String, nickname: String, password: String): User = {
    val userNoId = UserNoId(email, nickname, password)
    createUser(userNoId)
  }

  /**
    * Creates a new User and adds it to the list of Users.
    * If the User with the same email already exists,
    * the exception will be thrown.
    * @param jValue json to be parsed
    * @throws java.lang.IllegalArgumentException
    *               in the case of not unique email
    * throws org.json4s.MappingException
    *               in the case of not correct json fields
    * @return registered User
    */
  @throws(classOf[IllegalArgumentException])
  @throws(classOf[MappingException])
  def createUser(jValue: JValue): User = {
    val userNoId = jValue.extract[UserNoId]
    createUser(userNoId)
  }

  def getUsers(): Set[User] = users

  /** Helper case class, used for simplification of json parsing */
  private case class UserNoId(email: String, nickname: String, password: String)
}