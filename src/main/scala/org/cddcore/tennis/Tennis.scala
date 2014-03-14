package org.cddcore.tennis

import org.junit.runner.RunWith
import org.cddcore.engine.Engine
import org.cddcore.engine.tests.CddJunitRunner
import org.corecdd.website.WebServer

case class Score1(name: String)

object Score1 {
  val love = Score1("love")
  val s15 = Score1("Fifteen")
  val s30 = Score1("Thirty")
  val s40 = Score1("Forty")
  val deuce = Score1("deuce")
  val advantage = Score1("advantage")
  val won = Score1("won")
  val lost = Score1("lost")
  val noScore = Score1("no score")
}

@RunWith(classOf[CddJunitRunner])
object Tennis {
  import Score1._
  val lookup = Map(0 -> love, 1 -> s15, 2 -> s30, 3 -> s40)
  val scorer = Engine[Int, Int, String]().
    title("Tennis Kata").
    description("Tennis Kata specified by http://codingdojo.org/cgi-bin/wiki.pl?KataTennis").
    useCase("A game is won by the first player to have won at least four points in total and at least two points more than the opponent.").
    scenario(4, 0).expected("Left won").because((l: Int, r: Int) => (l - r) >= 2 && l >= 4).
    scenario(4, 1).expected("Left won").
    scenario(4, 2).expected("Left won").
    scenario(5, 3).expected("Left won").
    scenario(0, 4).expected("Right won").because((l: Int, r: Int) => (r - l) >= 2 && r >= 4).
    scenario(1, 4).expected("Right won").
    scenario(2, 4).expected("Right won").
    scenario(3, 5).expected("Right won").
    useCase("The running score of each game is described in a manner peculiar to tennis: scores from zero to three points are described as 'love', 'fifteen', 'thirty', and 'forty' respectively.").
    scenario(0, 0).expected("love - love").because((l: Int, r: Int) => l < 4 && r < 4).code((l: Int, r: Int) => lookup(l).name + " - " + lookup(r).name).
    scenario(2, 3).expected("Thirty - Forty").
    scenario(2, 1).expected("Thirty - Fifteeny").

    useCase("If at least three points have been scored by each player, and the scores are equal, the score is 'deuce'.").priority(1).
    scenario(4, 4).expected("Deuce").because((l: Int, r: Int) => l >= 3 && r >= 3 && l == r).
    scenario(6, 6).expected("Deuce").
    scenario(3, 3).expected("Deuce").

    useCase("If at least three points have been scored by each side and a player has one more point than his opponent, the score of the game is 'advantage' for the player in the lead.").
    scenario(5, 4).expected("Advantage Left").because((l: Int, r: Int) => l > 3 && r > 3 && l == r + 1).
    scenario(6, 5).expected("Advantage Left").
    scenario(4, 5).expected("Advantage Right").because((l: Int, r: Int) => l > 3 && r > 3 && r == l + 1).
    scenario(5, 6).expected("Advantage Right").
    build

  def main(args: Array[String]) {
    WebServer(scorer).launch
  }
}