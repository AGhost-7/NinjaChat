name := """Ninja Chat"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
	jdbc,
	anorm,
	cache,
	ws,
	"org.mindrot" % "jbcrypt" % "0.3m",
	"org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
	"com.typesafe.akka" %% "akka-slf4j" % "2.3.11"
)

//libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

doc in Compile <<= target.map(_ / "none")