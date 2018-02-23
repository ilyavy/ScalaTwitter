package com.inno.sierra

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, NoSuchElementException}
import javax.servlet.http.HttpServletRequest

import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra._
import org.scalatra.json._
import pdi.jwt.{JwtAlgorithm, JwtJson4s}

class MainServlet extends ScalatraServlet with JacksonJsonSupport {
  implicit val jsonFormats = DefaultFormats

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm")
  private var messages = Set[Twit]()
  private val users = User.getUsers()
  private val key = "powugpsoavbpiepag" // TODO: generation of a new key each time?
  private var blackListTokens = Map[String, Date]()
  private var subscriptions = Map[Int, Set[Int]]()
  private var retweets = Map[Int, Set[Int]]() //to add retweets
  // userId mentioned, the set of twit ids
  private var mentions = Map[Int, Set[Int]]()

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
        "Error 409: The user with the same email already exists.")
      case e: Exception => e.printStackTrace()
    }
  }

  /**
    * Pass here a JSON that contains
    * email and password of a User to be signed in
    */
  post("/signin") {
    try {
      val jValue = parse(request.body)
      val values = jValue.extract[Map[String, String]]
      val user = User.getUser(values("email"), values("password"))
      val token = signIn(user)
      response.setHeader("Access-Token", token)

    } catch {
      case me: MappingException => Conflict(
        "Error 409: Specified parameters are incorrect.")
      case nsee: java.util.NoSuchElementException => Conflict(
        "Error 409: The login or the password is invalid.")
      case e: Exception => e.printStackTrace()
    }
  }

  /**
    * Pass here the correct token in order to be signed out
    */
  post("/signout") {
    // Clear the black list of the expired tokens
    blackListTokens = blackListTokens.filter(
          p => p._2.after(Calendar.getInstance().getTime()))

    // Sign out if token is correct
    if (isTokenCorrect(request)) {
      val token = request.getHeader("Authorization").substring(7)
      val result = JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
      val map = result.get.extract[Map[String, Any]]

      val date = dateFormat.parse(map("timestamp").toString)
      val calendar = Calendar.getInstance()
      calendar.setTime(date)
      calendar.add(Calendar.HOUR,
            map("expTime").asInstanceOf[BigInt].toInt)
      blackListTokens = blackListTokens + (token -> calendar.getTime())

    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /**
    * Pass here a JSON that contains id and message that would be created
    */
  post("/messages") { // TODO: Do not forget to use isTokenCorrect(), the example in get("/messages")
    val jValue = parse(request.body)
    val userId = getIdFromToken(request)
    val m = Twit(
      (parsedBody \ "id").extract[Int],
      (parsedBody \ "text").extract[String],
      users.find(u=>u.id == userId).get,
      Calendar.getInstance().getTime()
    )

    if (isTokenCorrect(request)) {
      if (messages.exists(_.id == m.id)) {
        Conflict("Error 409: The message with the same id already exists.")
      } else {
        messages = messages + m
      }

    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /**
    * Pass here a JSON that contains id of tweet you want to retweet
    */

  post("/retweet") {
    val jValue = parse(request.body)
    val twitId = (parsedBody \ "id").extract[Int]
	  val userId = getIdFromToken(request)

	if (isTokenCorrect(request)) {
      	if (messages.exists(_.id == twitId)) {
			if(retweets contains userId){
		        retweets += userId -> (retweets(userId) + twitId) 
			}else {retweets += userId -> Set(twitId) }
      	} else {     
		Conflict("Error 404: The message does not exist.")
      	}
 
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /** Remove retweet of a message*/
  delete("/retweet/:id") {
    val id = params("id").toInt
    val userId = getIdFromToken(request)

    if (isTokenCorrect(request)) {
      if (messages.isEmpty || !messages.exists(_.id == id)) {
        NotFound("Error 404. The message with the specified id ("
          + id + ") does not exist.")
      } else {
          if (retweets.contains(userId)){
            if (retweets(userId).contains(id)){
                val x=retweets(userId)-id
                retweets =retweets + (userId->x)
              }else {
                Conflict("Error 404: Message has not been retweeted.")
                }
            } else {
                Conflict("Error 404: There are no retweets.")
                }
        }
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /**
    * Pass here a JSON that contains id of the user you want ot subscribe to
    */
  post("/subscribe") {
    val jValue = parse(request.body)
    val s = (parsedBody \ "id").extract[Int]
    if (isTokenCorrect(request)) {
      val userId = getIdFromToken(request)
      if(subscriptions contains userId) {
        subscriptions += userId -> (subscriptions(userId) + s)
      } else
        subscriptions += userId -> Set(s)

    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /** It should return created messages */
  get("/messages") { // TODO: Do not forget to use isTokenCorrect(), the example in get("/messages")
    contentType = formats("json")

    if (isTokenCorrect(request)) {
      messages
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /** It should return feed for user */
  get("/feed") {
    contentType = formats("json")

    if (isTokenCorrect(request)) {
      val userId = getIdFromToken(request)
      val userSubscribtions =
        if(subscriptions contains userId) subscriptions(userId)
        else Set[Int]()
      messages.filter(userSubscribtions contains _.author.id) union messages.filter(retweets contains _.author.id)
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /** It should return messages of specified user */
  get("/feed/:id") {
    contentType = formats("json")
    val id = params("id").toInt

    if (isTokenCorrect(request)) {
      messages.filter(_.author.id == id)
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /** It should return only one message that has same id as :id parameter */
  get("/messages/:id") { // TODO: Do not forget to use isTokenCorrect(), the example in get("/messages")
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
    if (isTokenCorrect(request)) {
      if (messages.isEmpty || !messages.exists(_.id == id)) {
        NotFound("Error 404. The message with the specified id ("
          + id + ") does not exist.")

      } else {
        val mes = messages.filter(_.id == id).head
        messages = messages - mes
        val m = Twit(id, (parsedBody \ "text").extract[String],
          users.find(u=>u.id == getIdFromToken(request)).get,
          Calendar.getInstance().getTime())
        messages = messages + m
      }
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /** It should delete a message with id the same as :id parameter */
  delete("/messages/:id") { // TODO: Do not forget to use isTokenCorrect(), the example in get("/messages")
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
    * Mentions the user by his/her nickname if the option
    * "Notify me when somebody mentions me" is turned on.
    * Takes the json with two parameters:
    * twit_id - Int, the id of the twin to be mentioned in
    * nickname - String, the nickname of the user mentioned
    */
  post("/mention") {
    if (isTokenCorrect(request)) {
      try {
        val jValue = parse(request.body)
        val values = jValue.extract[Map[String, Any]]
        val nick = values("nickname").toString
        val userId = User.getUsers().filter(_.nickname == nick).head.id

        val twitIds =
          if (mentions.contains(userId)) mentions(userId)
          else Set[Int]()
        mentions = mentions +
          (userId -> (twitIds + values("twit_id").toString.toInt))
      } catch {
        case me: MappingException => Conflict(
          "Error 409: Specified parameters are incorrect.")
        case nsee: NoSuchElementException => Conflict(
          "Error 409: There is no such user."
        )
        case e: Exception => e.printStackTrace()
      }
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /**
    * Returns the feed with the twits, where user
    * was mentioned.
    */
  get("/mentions_feed") {
    contentType = formats("json")

    if (isTokenCorrect(request)) {
      val userId = getIdFromToken(request)
      if (mentions.contains(userId)) {
        val twitIds = mentions(userId)
        messages.filter(t => twitIds.contains(t.id))
      }
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /**
    * Turns the setting "Notify me when somebody mentions me"
    * on or off. Takes one parameter:
    * on - String, turn the setting on
    * off - String, turn the setting off
    */
  post("/mentions_setting:value") {
    if (isTokenCorrect(request)) {
      val userId = getIdFromToken(request)
      val user = User.getUsers().filter(_.id == userId).head
      params("value").toString match {
        case "on" => user.notifyWhenMentioned(true)
        case "off" => user.notifyWhenMentioned(false)
      }
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }
  }

  /**
    * Creates token for the specified User
    * @param user for whom to create a token
    * @return token
    */
  private def signIn(user: User): String = {
    val algorithm = JwtAlgorithm.HS256
    val timestamp = dateFormat.format(Calendar.getInstance().getTime())

    val payload = JObject(("userId", user.id),
      ("email", user.email), ("nickname", user.nickname),
      ("timestamp", timestamp), ("expTime", 24) // expTime in hours
    )

    val token = JwtJson4s.encode(payload, key, algorithm)
    println(token)
    token
  }

  /**
    * Returns id from the token by the request.
    * Token is supposed to be in the header "Authorization"
    * @param request  the request with token
    * @return id of the user, who owns the token
    */
  private def getIdFromToken(request: HttpServletRequest) : Int = {
    val token = request.getHeader("Authorization").substring(7)

    val result = JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
    val map = result.get.extract[Map[String, Any]]
    map("userId").toString.toInt
  }

  /**
    * Verifies either the specified token is correct or not
    * @param request  the request, with the token in the header
    * @return Boolean (true - if correct, false - otherwise)
    */
  private def isTokenCorrect(request: HttpServletRequest): Boolean = {
    val token = request.getHeader("Authorization").substring(7)

    val result = JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
    val map = result.get.extract[Map[String, Any]]

    val date = dateFormat.parse(map("timestamp").toString)
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    calendar.add(Calendar.HOUR,
          map("expTime").asInstanceOf[BigInt].toInt)

    if (blackListTokens.contains(token) ||
          Calendar.getInstance().getTime().after(calendar.getTime())) {
      false
    } else {
      result.isSuccess
    }
  }
}
