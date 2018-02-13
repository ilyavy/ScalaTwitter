package com.inno.sierra

import org.scalatra._
import scala.collection.mutable.ListBuffer
import org.scalatra.json._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.parse

class MainServlet extends ScalatraServlet with JacksonJsonSupport {
  implicit val jsonFormats = DefaultFormats

  private var messages = Set[Twit]()

  // Pass here a JSON that contains id and message that would be created
  post("/messages") {
    val jsonStr = request.body
    val jValue = parse(jsonStr)
    val m = jValue.extract[Twit]

    if (messages.exists(_.id == m.id)) {
      Conflict("Error 409: The message with the same id already exists.")
    } else {
      messages = messages + m
    }
  }

  // It should return created messages
  get("/messages") {
    contentType = formats("json")
    messages
  }

  // It should return only one message that has same id as :id parameter
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

  // It should update message with id the same as :id parameter
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
      val m = Twit(id, (jValue \ "text").extract[String])
      messages = messages + m
    }
  }

  // It should delete a message with id the same as :id parameter
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
}
