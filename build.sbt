name := "AkkaActorToolkit"

version := "0.1"

scalaVersion := "2.13.12"

val akkaVersion = "2.6.21"
val scalaTestVersion = "3.2.18"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % "2.8.8",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  )
}
