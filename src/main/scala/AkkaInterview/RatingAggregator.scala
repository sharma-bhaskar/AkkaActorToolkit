package AkkaInterview

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.concurrent.TrieMap

/**
  * Scenario
  * You are developing a media ratings aggregator for a media company that oversees multiple shows across different genres.
  *
  * The application collects viewer ratings for each show and calculates aggregate metrics such as:
  *
  * average rating - the mean score given to a particular show by its viewers
  * reach - indicates the total number of unique viewers who have submitted ratings for the show
  * an audience engagement score - an overview of viewer involvement
  * Requirements:
  *
  * Design classes to represent a show, a rating, and the aggregate metrics
  *
  * Implement a thread safe version of a RatingAggregator, by implementing the following methods:
  *
  * addRating: adds a rating for a specific show from a particular viewer
  * computeAggregates: computes and returns the aggregate metrics for a specific show
  *
  * Use object-oriented design (OOD) principles so that the code is maintainable and properly testable
  *
  *
  * Example of Computing Metrics
  *
  * Let's consider a show called "The Nielsen Interview" with the following ratings submitted:
  *
  * Viewer 1: 8.0
  * Viewer 2: 9.0
  * Viewer 1: 7.0 (same viewer)
  * Viewer 3: 10.0
  *
  * Calculation Steps:
  * Average Rating:
  * Total Scores = 8.0 + 9.0 + 7.0 + 10.0 = 34.0
  * Number of Ratings = 4
  * Average Rating = 34.0/4 = 8.5
  * Reach:
  * Unique Viewers = {Viewer 1, Viewer 2, Viewer 3}
  * Reach = 3 (total unique viewers)
  * Engagement Score:
  * EngagementScore = Average Rating * Reach / Number of Ratings = 6.375 */
case class Show(id: String, name: String)

case class Rating(viewerId: String, score: Double)

case class AggregateMetrics(averageRating: Double, reach: Int, engagementScore: Double)

object RatingAggregator {

  sealed trait SystemCommand

  case class AddRating(show: Show, rating: Rating) extends SystemCommand

  case class GetAggregate(show: Show, replyTo: ActorRef[Option[AggregateMetrics]]) extends SystemCommand

  def apply(): Behavior[SystemCommand] = Behaviors.setup { _ =>

    val ratingsMap: TrieMap[Show, Vector[Rating]] = TrieMap.empty

    Behaviors.receiveMessage {
      case AddRating(show, rating) =>
        ratingsMap.updateWith(show) {
          case Some(existing) => Some(existing :+ rating)
          case None => Some(Vector(rating))
        }
        Behaviors.same

      case GetAggregate(show, replyTo) =>
        val result = ratingsMap.get(show).map { ratings =>
          val totalScore = ratings.map(_.score).sum
          val numberOfRating = ratings.size
          val reach = ratings.map(_.viewerId).distinct.size

          val averageRating = totalScore / numberOfRating
          val enagementScore = averageRating * reach / numberOfRating

          AggregateMetrics(averageRating,reach,enagementScore)
        }

        replyTo ! result
        Behaviors.same
    }
  }


}
