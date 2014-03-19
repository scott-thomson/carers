package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner
import org.cddcore.engine.Engine
import org.joda.time.DateTime

@RunWith(classOf[CddJunitRunner])
object InterestingDates {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Claim.getXml(x._2))
  implicit def stringToDate(x: String) = Claim.asDate(x)
  implicit def stringToOptionDate(x: String) = Some(Claim.asDate(x))

  val isInRange = Engine[DateTime, DateTime, Option[DateTime], Boolean]().title("Date in range").
    useCase("all dates exist").
    scenario("2010-2-15", "2010-2-1", "2010-2-20").expected(true).
    code((dateOfInterest: DateTime, start: DateTime, end: Option[DateTime]) =>
      (start.isBefore(dateOfInterest) || start == dateOfInterest) &&
        end.isDefined &&
        (end.get.isAfter(dateOfInterest) || end.get == dateOfInterest)).
    scenario("2010-2-15", "2010-2-15", "2010-2-20").expected(true).
    scenario("2010-2-20", "2010-2-15", "2010-2-20").expected(true).
    scenario("2010-2-14", "2010-2-15", "2010-2-20").expected(false).
    scenario("2010-2-21", "2010-2-15", "2010-2-20").expected(false).
    scenario("2010-2-16", "2010-2-15", "2010-2-20").expected(true).
    scenario("2010-2-19", "2010-2-15", "2010-2-20").expected(true).

    useCase("if end doesn't exist return false").
    scenario("2010-2-20", "2010-2-15", None).expected(false).
    scenario("2010-2-1", "2010-2-15", None).expected(false).
    build

  def interestingDates = Engine.folding[CarersXmlSituation, Option[DateTime], List[DateTime]]((acc, opt) => acc ::: opt.toList, List()).title("Interesting Dates").

    childEngine("BirthDate", "Your birthdate is interesting IFF you become the age of sixteen during the period of the claim").
    scenario(("2010-3-1", "CL100105a")).expected(None).
    scenario(("2010-3-1", "CL1PA100")).expected("1994-7-10").
    code((c: CarersXmlSituation) => Some(c.claimBirthDate())).
    because((c: CarersXmlSituation) => isInRange(c.claimBirthDate().plusYears(16), c.claimStartDate(), c.claimEndDate())).

    childEngine("Claim start date", "Is always an interesting date").
    scenario(("2010-3-1", "CL100105a")).expected("2010-1-1").
    code((c: CarersXmlSituation) => Some(c.claimStartDate())).

    childEngine("Claim end date", "Is always an interesting date, and we have to fake it if it doesn't exist").
    scenario(("2010-3-1", "CL100105a")).expected("3999-12-31").

    scenario(("2010-3-1", "CL1PA100")).expected("2999-12-31").
    code((c: CarersXmlSituation) => c.claimEndDate()).
    because((c: CarersXmlSituation) => c.claimEndDate().isDefined).

    childEngine("Claim submitted date", "Is always an interesting date").
    scenario(("2010-3-1", "CL100105a")).expected("2010-1-1").
    code((c: CarersXmlSituation) => Some(c.claimSubmittedDate())).

    childEngine("Time Limit For Claiming Three Months", "Is  an interesting date, if it falls inside the claim period").
    scenario(("2010-3-1", "CL100105a")).expected(None).

    scenario(("2010-3-1", "CL1PA100")).expected("2010-3-9").
    code((c: CarersXmlSituation) => Some(c.timeLimitForClaimingThreeMonths)).
    because((c: CarersXmlSituation) => isInRange(c.timeLimitForClaimingThreeMonths, c.claimStartDate(), c.claimEndDate())).

    build;

}