package com.inno.sierra

import org.scalatra.test.scalatest._

class MainServletTests extends ScalatraFunSuite {

  addServlet(classOf[MainServlet], "/*")

  test("GET / on MainServlet should return status 200"){
    get("/"){
      status should equal (200)
    }
  }

}
