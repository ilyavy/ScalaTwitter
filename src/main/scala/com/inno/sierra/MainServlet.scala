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
  private val key = "powugpsoavbpiepag"
  private var blackListTokens = Map[String, Date]()
  private var subscriptions = Map[Int, Set[Int]]()
  private var retweets = Map[Int, Set[Int]]() //to add retweets
  // userId mentioned, the set of twit ids
  private var mentions = Map[Int, Set[Int]]()
  private var likes =  Map[Int, Set[Int]]()
  private var dislikes =  Map[Int, Set[Int]]()	

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
  post("/messages") {
    val jValue = parse(request.body)
    val userId = getIdFromToken(request)
    val m = Twit.createTwit(
      (parsedBody \ "text").extract[String],
      users.find(u=>u.id == userId).get
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
    * it should receive an json with the id of message to like
    * it also will remove dislike if user previously disliked that tweet
    */
  post("/like") {
    val jValue = parse(request.body)
    val twitId = (parsedBody \ "id").extract[Int]
    val userId = getIdFromToken(request)
    println("I am " + userId)

    if (isTokenCorrect(request)) {
        like(userId, twitId)
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }

    /** Function with a user dislikes a tweet (from other people).
      *@param uID
      * @param tID
      */
    def like (uID: Int, tID: Int): Unit= {

      if (!messages.isEmpty && messages.exists(_.id == tID)) {
        //get author from message
        val m: Twit = messages.filter(_.id == tID).head
        val author: Int = m.author.id
        println("I want to like twit of " + author)

        if (!author.equals(uID)) {
          val userLikes =
            if (likes.contains(uID)) likes(uID)
            else Set[Int]()
          likes += uID -> (userLikes + tID)
          println("Likes: " + likes)
          //if message has been previously disliked it,
          // delete it from dislikes
          likeEliminatesDislike(uID, tID)
        } else {
          println("you cannot like your own message")
        }
      } else {
        Conflict("Error 404: Message cannot be liked" +
          " because it does not exist!")
      }

      /**
        * a user dislikes a previously liked tweet (from other people).
        * @param userID
        * @param id
        */
      def likeEliminatesDislike(userID: Int, id: Int): Unit = {
        if (dislikes.contains(userID)) {
          println("user has dislikes messages before")
          if (dislikes(userID).contains(id)) {
            println("twit has been disliked")
            val x = dislikes(userID) - id
            dislikes = dislikes + (userID -> x)

          } else {
            println("twit has not been disliked. Do nothing!")
          }
        } else {
          println("user does not disliked any message")
        }
      }
    }
  }

  /**
    * it should receive an json with the id of message to dislike
    * it also will remove like if user previously liked that tweet
    */
  post("/dislike") {
    val jValue = parse(request.body)
    val twitId = (parsedBody \ "id").extract[Int]
    val userId = getIdFromToken(request)

    if (isTokenCorrect(request)) {
      dislike(userId, twitId)
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
    }

    /** Function with a user dislikes a tweet (from other people).
      *@param uID
      * @param tID
      */
    def dislike (uID: Int, tID: Int): Unit= {

      if (!messages.isEmpty && messages.exists(_.id == tID)) {
        println("messages exist, continue")
        //get author from message
        val m: Twit = messages.filter(_.id == tID).head
        val author: Int = m.author.id

        if (!author.equals(uID)) {
          val userLikes =
            if (dislikes.contains(uID)) dislikes(uID)
            else Set[Int]()
          dislikes += uID -> (userLikes + tID)
          println("Dislikes: " + dislikes)
          //if message has been previously liked it,
          // delete it from likes
          dislikeEliminatesLike(uID, tID)
        } else {
          println("you cannot dislike your own message")
        }
      } else {
        Conflict("Error 404: Message cannot be disliked" +
          " because it does not exist!")
      }

      /**
        * When a user dislikes a tweet (from other people) previously liked, removes like.
        * @param userID
        * @param id
        */
      def dislikeEliminatesLike(userID: Int, id: Int): Unit = {
        if (likes.contains(userID)) {
          println("user has liked messages before")
          if (likes(userID).contains(id)) {
            println("twit has been liked")
            val x = likes(userID) - id
            likes = likes + (userID -> x)
            println("like has been deleted")
          } else {
            println("twit has not been liked. Do nothing!")
          }
        } else {
          println("user does not liked any message")
        }
      }
    }
  }

  /**
    * Removes likes and dislikes.
    * Takes one parameter - the id of the twit,
    * like/dislike for which should be removed.
    */
  post("/removelike/:id") {
    println("Likes before: " + likes)
    println("Dislikes before: " + dislikes)
    val id = params("id").toInt
    if (isTokenCorrect(request)) {
      val userId = getIdFromToken(request)
      if (likes.contains(userId)) {
        val twitIds = likes(userId) - id
        likes = likes + (userId -> (twitIds))
      }
      if (dislikes.contains(userId)) {
        val twitIds = dislikes(userId) - id
        dislikes = dislikes + (userId -> (twitIds))
      }
    }
    println("Likes after: " + likes)
    println("Dislikes after: " + dislikes)
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
        val m = Twit.createTwit(
          (parsedBody \ "text").extract[String],
          users.find(u=>u.id == getIdFromToken(request)).get
        )
        messages = messages + m
      }
    } else {
      Conflict("Error 401: The token is incorrect or expired.")
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
  post("/mentions_setting/:value") {
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
