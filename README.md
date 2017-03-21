# Custom GraphStage in Akka Streams

This is accompanying repository for article [ADD LINK WHEN AVAILABLE].

Code for the first part of article (basics of `GraphStage`) is located in `io.scalac.streams.stage.basic` package. 

Code for `ProxyGraphStage` may be found in `io.scalac.streams.stage.proxy`. First and naive version of code for 
`ProxyGraphStage` is in `HttpsProxyGraphStage0.scala` and then it's consequently improved 
up to `HttpsProxyGraphStage0.scala`. The final version is in`CorrectHttpsProxyGraphStage.scala`

## How to run

You can run simple samples with `sbt run`.

You can run tests for `ProxyGraphStage` with `sbt test`.