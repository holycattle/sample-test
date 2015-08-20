name := """ticket-serv"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  cache,
  ws,
  specs2 % Test
)

libraryDependencies += "com.typesafe.play" %% "play-slick" % "1.0.1"
libraryDependencies += "com.h2database" % "h2" % "1.3.176" // replace `${H2_VERSION}` with an actual version number
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.25"

libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.0.1",
    "joda-time" % "joda-time" % "2.4",
    "org.joda" % "joda-convert" % "1.7",
    "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
