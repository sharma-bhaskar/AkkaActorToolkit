package TestInterview

object TestRating extends App {

  val aggregator = new RatingAggregator

  val show = Show("1","Test1")
  aggregator.addRating(show, Rating("v1", 8.0))
  aggregator.addRating(show, Rating("v2", 9.0))
  aggregator.addRating(show, Rating("v1", 7.0)) // same viewer again
  aggregator.addRating(show, Rating("v3", 10.0))

  println(aggregator.computeAggregates(show))



}
