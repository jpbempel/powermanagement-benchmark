package com.criteo.benchmarks

import io.gatling.http.Predef._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class ServerBenchmark extends Simulation {

  val URL = "http://localhost:8080"
  println("Staring Server Benchmarks to: " + URL)

  val httpConf = http.baseURL(URL)
    .contentTypeHeader("application/json")
  val citypopulationFile = tsv("unsd-citypopulation-year-fm_json-short.json").records.flatMap(record => record.values)
  val scn = scenario("json_citypop")

    .foreach(citypopulationFile, "bodyContent")(
      exec(http("citypopulation")
        .post("/")
        .body(StringBody("${bodyContent}"))
        .check(status.is(200))
      )
    )

  setUp(
    scn.inject(atOnceUsers(10))
  ).protocols(httpConf)
}
