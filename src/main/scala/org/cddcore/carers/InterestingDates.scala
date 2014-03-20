package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner
import org.cddcore.engine.Engine
import org.joda.time.DateTime
import scala.language.implicitConversions

@RunWith(classOf[CddJunitRunner])
object InterestingDates {
  implicit def stringStringToCarers(x: String) = CarersXmlSituation(World(), Claim.getXml(x))
  implicit def stringToDate(x: String) = Claim.asDate(x)
  implicit def stringToOptionDate(x: String) = Some(Claim.asDate(x))
  implicit def stringStringToDateTimeString(t: (String, String)) = List((Claim.asDate(t._1), t._2))
  implicit def toValidateClaim(x: List[(String, String, Boolean)]): CarersXmlSituation = Claim.validateClaimWithBreaks(x: _*)

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

  private val addStartDateOfDateInInCareAndFirstDateOutOfBreak = (dr: DateRange) => List(
    (dr.from, "Break in care (" + dr.reason + ") started"),
    (dr.to.plusDays(1), "Break in care (" + dr.reason + ") ended"))

  def conditionallyAddDate(dr: DateRange, weeks: Int): List[(DateTime, String)] = {
    val lastDay = dr.from.plusWeeks(weeks)
    if (dr.to.isAfter(lastDay) || dr.to == lastDay) List((lastDay, "Care break too long")) else List()
  }

  val interestingDates = Engine.folding[CarersXmlSituation, Iterable[(DateTime, String)], List[(DateTime, String)]]((acc, opt) => acc ++ opt, List()).title("Interesting Dates").
    childEngine("Sixteenth Birthday", "Your birthdate is interesting IFF you become the age of sixteen during the period of the claim").
    scenario("CL100105A").expected(List()).
    scenario("CL1PA100").expected(("2010-7-10", "Sixteenth Birthday")).
    code((c: CarersXmlSituation) => List((c.claimBirthDate().plusYears(16), "Sixteenth Birthday"))).
    because((c: CarersXmlSituation) => isInRange(c.claimBirthDate().plusYears(16), c.claimStartDate(), c.claimEndDate())).

    childEngine("Claim start date", "Is always an interesting date").
    scenario("CL100105A").expected(("2010-1-1", "Claim Start Date")).
    code((c: CarersXmlSituation) => List((c.claimStartDate(), "Claim Start Date"))).

    childEngine("Claim end date", "Is always an interesting date, and we have to fake it if it doesn't exist").
    scenario("CL100105A").expected(("3999-12-31", "Claim End Date")).

    scenario("CL1PA100").expected(("2999-12-31", "Claim End Date")).
    code((c: CarersXmlSituation) => List((c.claimEndDate().get, "Claim End Date"))).
    because((c: CarersXmlSituation) => c.claimEndDate().isDefined).

    childEngine("Claim submitted date", "Is always an interesting date").
    scenario("CL100105A").expected(("2010-1-1", "Claim Submitted Date")).
    code((c: CarersXmlSituation) => List((c.claimSubmittedDate(), "Claim Submitted Date"))).

    childEngine("Time Limit For Claiming Three Months", "Is an interesting date, if it falls inside the claim period").
    scenario("CL100105A").expected(List()).

    scenario("CL1PA100").expected(("2010-3-9", "Three month claim time limit")).
    code((c: CarersXmlSituation) => List((c.timeLimitForClaimingThreeMonths, "Three month claim time limit"))).
    because((c: CarersXmlSituation) => isInRange(c.timeLimitForClaimingThreeMonths, c.claimStartDate(), c.claimEndDate())).

    childEngine("Breaks in Care add the from date, and the first date after the to date").
    scenario(List(("2010-3-1", "2010-3-4", true)), "Single break").expected(List(("2010-3-1", "Break in care (Hospital) started"), ("2010-3-5", "Break in care (Hospital) ended"))).
    code((c: CarersXmlSituation) => c.breaksInCare().flatMap(addStartDateOfDateInInCareAndFirstDateOutOfBreak)).
    scenario(List(("2010-3-1", "2010-3-4", true), ("2010-4-1", "2010-4-4", true)), "Two breaks").
    expected(List(("2010-3-1", "Break in care (Hospital) started"), ("2010-3-5", "Break in care (Hospital) ended"),
      ("2010-4-1", "Break in care (Hospital) started"), ("2010-4-5", "Break in care (Hospital) ended"))).

    childEngine("Four weeks after the start of a non hospital break in care, and twelve weeks after a hospital break of care are interesting if the break is active then").
    scenario(List(("2010-7-1", "2010-7-4", false)), "Non hospital break, Too short").expected(List()).
    code((c: CarersXmlSituation) => c.breaksInCare().flatMap(dr => {
      dr.reason.equalsIgnoreCase("Hospital") match {
        case false => conditionallyAddDate(dr, 4)
        case true => conditionallyAddDate(dr, 12)
      }
    }).toList).
    scenario(List(("2010-7-1", "2010-8-1", false)), "Non hospital break, more than four weeks").expected(List(("2010-7-29", "Care break too long"))).
    scenario(List(("2010-7-1", "2010-8-1", true)), "Hospital break. Too short").expected(List()).
    scenario(List(("2010-7-1", "2010-10-1", true)), "Hospital break, more than twelve weeks").expected(List(("2010-9-23", "Care break too long"))).

    build;

}