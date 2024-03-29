val ScalatraVersion = "2.6.2"

organization := "cd"

name := "ScalaTwitter"

version := "0.1"

scalaVersion := "2.12.4"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.8.v20171121" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.scalatra" %% "scalatra-json" % ScalatraVersion,
  "org.json4s" %% "json4s-jackson" % "3.5.2",
  "com.pauldijou" %% "jwt-core" % "0.14.1",
  "com.pauldijou" %% "jwt-json4s-jackson" % "0.14.1"
)

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
