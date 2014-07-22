import org.scalatra.sbt._

val ScalatraVersion = "2.3.0"

lazy val active_slick = project in file("lib/active-slick")

lazy val root = Project(
  "root",
  file("."),
  settings = Defaults.defaultSettings ++ ScalatraPlugin.scalatraSettings ++ Seq(
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.10.3",
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "2.0.2",
      "org.slf4j" % "slf4j-nop" % "1.6.4",
      "org.scalatest" %% "scalatest" % "2.2.0" % "test",
      "org.scalatra" %% "scalatra" % ScalatraVersion,
      "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
      "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container",
      "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar")),
      "com.typesafe.slick" %% "slick" % "2.0.2",
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.5",
      "com.github.tototoshi" %% "slick-joda-mapper" % "1.1.0",
      "org.json4s" %% "json4s-native" % "3.2.10",
      "org.json4s" %% "json4s-jackson" % "3.2.7",
      "c3p0" % "c3p0" % "0.9.1.2"
    )
  )
).dependsOn(active_slick).aggregate(active_slick)
