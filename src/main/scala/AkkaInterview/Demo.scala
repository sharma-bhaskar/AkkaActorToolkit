package AkkaInterview

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}


object Demo extends App{
  val system = ActorSystem(RatingAggregator(), "RatingAggregators")

  val show = Show("1","The Nielsen Interview")

  system ! RatingAggregator.AddRating(show,Rating("v1",8.0))
  system ! RatingAggregator.AddRating(show,Rating("v2",9.0))
  system ! RatingAggregator.AddRating(show,Rating("v1",7.0))
  system ! RatingAggregator.AddRating(show,Rating("v3",10.0))

  implicit val timeout: Timeout = 2.seconds

  implicit val scheduler = system.scheduler
  implicit val ec = system.executionContext

  val result = system.ask(ref => RatingAggregator.GetAggregate(show, ref))

  result.foreach{
    case Some(metrics) =>
      println(metrics.averageRating)
      println(metrics.reach)
      println(metrics.engagementScore)
    case None =>
      println("No rating found for show ")
  }

}
