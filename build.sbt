

val ScalatraVersion = "2.3.0"

lazy val active_slick = RootProject(file("lib/active-slick"))

lazy val root = Project(
  "slick-auth",
  file("."),
  settings = Defaults.defaultSettings ++ Seq(
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.10.3",
    libraryDependencies ++= Seq(
      // slick
      "com.typesafe.slick" %% "slick" % "2.0.2",
      "c3p0" % "c3p0" % "0.9.1.2",
      "com.github.tototoshi" %% "slick-joda-mapper" % "1.1.0",
      // loggers
      "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
      // scalatra
      "org.scalatest" %% "scalatest" % "2.2.0" % "test",
      "org.scalatra" %% "scalatra" % ScalatraVersion,
      "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
      "org.scalatra" %% "scalatra-auth" % "2.2.2",
      // servlets
      "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "provided;test" artifacts (Artifact("javax.servlet", "jar", "jar")),
      "com.typesafe.slick" %% "slick" % "2.0.2",
      // utils
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.5",
      "org.json4s" %% "json4s-native" % "3.2.10",
      "org.json4s" %% "json4s-jackson" % "3.2.7",
      "org.json4s"   %% "json4s-ext" % "3.2.10",
      "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
    )
  )
).dependsOn(active_slick).aggregate(active_slick)
