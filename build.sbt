name := """akka-streams-graph-stage"""

version := "1.0"

scalaVersion := "2.12.1"
scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"          % "2.6.5",
  "com.typesafe.akka" %% "akka-stream-testkit"  % "2.6.5"  % "test",
  "com.typesafe.akka" %% "akka-testkit"         % "2.6.5"  % "test",
  "org.scalatest"     %% "scalatest"            % "3.0.1"   % "test"
)


