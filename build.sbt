name := """akka-streams-graph-stage"""

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"          % "2.4.17",
  "com.typesafe.akka" %% "akka-stream-testkit"  % "2.4.17"  % "test",
  "com.typesafe.akka" %% "akka-testkit"         % "2.4.17"  % "test",
  "org.scalatest"     %% "scalatest"            % "3.0.1"   % "test"
)


