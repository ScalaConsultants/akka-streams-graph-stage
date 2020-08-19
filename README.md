# Custom GraphStage in Akka Streams

This is accompanying repository for [article](http://blog.scalac.io/2017/04/25/akka-streams-graph-stage.html) about `GraphStage`.

Code for the first part of article (basics of `GraphStage`) is located in `io.scalac.streams.stage.basic` package. 

Code for `ProxyGraphStage` may be found in `io.scalac.streams.stage.proxy`. First and naive version of code for 
`ProxyGraphStage` is in `HttpsProxyGraphStage0.scala` and then it's consequently improved 
up to `HttpsProxyGraphStage2.scala`. The final version is in`CorrectHttpsProxyGraphStage.scala`

## How to run

You can run simple samples with `sbt run`.

You can run tests for `ProxyGraphStage` with `sbt test`.

Developed by [Scalac](https://scalac.io/?utm_source=scalac_github&utm_campaign=scalac1&utm_medium=web)
