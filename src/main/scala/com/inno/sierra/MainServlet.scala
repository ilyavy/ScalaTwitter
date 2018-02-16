package com.inno.sierra

import java.text.SimpleDateFormat
import java.util.Calendar

import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra._
import org.scalatra.json._
import pdi.jwt.{JwtAlgorithm, JwtJson4s}

class MainServlet extends ScalatraServlet with JacksonJsonSupport {
  implicit val jsonFormats = DefaultFormats

  private var messages = Set[Twit]()
  private val users = User.getUsers()
  private val key = "powugpsoavbpiepag" // TODO: generation of a new key each time?

  /**
    * Pass here a JSON that contains
    * email, nickname, password of a User to be registered
    */
  post("/register") {
    try {
      val jValue = parse(request.body)
      val user = User.createUser(jValue)
      val token = signIn(user)
      response.setHeader("Access-Token", token)

    } catch {
      case me: MappingException => Conflict(
        "Error 409: Specified parameters are incorrect.")
      case iae: IllegalArgumentException => Conflict(
        "Error 409: The message with the same id already exists.")
      case e: Exception => e.printStackTrace()
    }
  }

  post("/login") {
    signIn(users.head)
  }

  /**
    * Pass here a JSON that contains id and message that would be created
    */
  post("/messages") {
    val jValue = parse(request.body)
    val m = jValue.extract[Twit]

    if (messages.exists(_.id == m.id)) {
      Conflict("Error 409: The message with the same id already exists.")
    } else {
      messages = messages + m
    }
  }

  /** It should return created messages */
  get("/messages") {
    contentType = formats("json")

    val token = request.getHeader("Authorization").substring(7)
    val result = JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
    println(result) // TODO: Delete later

    if (result.isFailure) {
      Conflict("Error 401: Authentication failed.")
    } else {
      val jObj = result.get
      messages
    }
  }

  /** It should return only one message that has same id as :id parameter */
  get("/messages/:id") {
    contentType = formats("json")
    val id = params("id").toInt
    if (messages.isEmpty || !messages.exists(_.id == id)) {
      NotFound("Error 404. The message with the specified id ("
        + id + ") does not exist.")
    } else {
      messages.filter(_.id == id).head
    }
  }

  /** It should update message with id the same as :id parameter */
  put("/messages/:id") {
    val id = params("id").toInt
    if (messages.isEmpty || !messages.exists(_.id == id)) {
      NotFound("Error 404. The message with the specified id ("
        + id + ") does not exist.")

    } else {
      val mes = messages.filter(_.id == id).head
      messages = messages - mes

      val jsonStr = request.body
      val jValue = parse(jsonStr).asInstanceOf[JObject]
      // TODO: have to be fixed. [Edit twit] user story
      //val m = Twit(id, (jValue \ "text").extract[String])
      //messages = messages + m
    }
  }

  /** It should delete a message with id the same as :id parameter */
  delete("/messages/:id") {
    val id = params("id").toInt
    if (messages.isEmpty || !messages.exists(_.id == id)) {
      NotFound("Error 404. The message with the specified id ("
        + id + ") does not exist.")

    } else {
      val mes = messages.filter(_.id == id).head
      messages = messages - mes
    }
  }

  /**
    * Creates token for the specified User
    * @param user for whom to create a token
    * @return token
    */
  private def signIn(user: User): String = {
    val algorithm = JwtAlgorithm.HS256

    val sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm")
    val timestamp = sdf.format(Calendar.getInstance().getTime())

    val payload = JObject(("userId", user.id),
      ("email", user.email), ("nickname", user.nickname),
      ("timestamp", timestamp), ("expTime", "24h")
    )

    val token = JwtJson4s.encode(payload, key, algorithm)
    println(token)
    token
  }
}
