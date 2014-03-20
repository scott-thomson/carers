package org.cddcore.carers

import org.cddcore.engine.Engine
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner
import scala.language.implicitConversions

@RunWith(classOf[CddJunitRunner])
object BreakInCare {
  implicit def stringToDate(x: String) = (Claim.asDate(x))

  implicit def stringStringToDateRange(x: Tuple3[String, String, String]) = DateRange(Claim.asDate(x._1), Claim.asDate(x._2), x._3)

  val singleBreakInCare = Engine[DateTime, DateTime, DateRange, Boolean]().title("Single Break In Care").
    description("The first date is the date being processed. The second date is the claim start date. The result is false if this DateRange invalidates the claim for the current date").
    useCase("Outside date range").
    scenario(("2010-3-1"), ("2010-1-1"), ("2010-5-1", "2010-5-5", "Reason"), "Before date").expected(true).
    because((processDate: DateTime, claimStartDate: DateTime, dr: DateRange) => !dr.contains(processDate)).

    useCase("Not yet 22 weeks after claim start date").
    scenario(("2010-3-1"), ("2010-1-1"), ("2010-3-1", "2010-3-5", "Reason"), "After date").expected(false).
    because((processDate: DateTime, claimStartDate: DateTime, dr: DateRange) => processDate.isBefore(claimStartDate.plusWeeks(22))).
    scenario(("2010-6-3"), ("2010-1-1"), ("2010-3-1", "2010-11-4", "Reason"), "Process Date 21 weeks and 6 days after claim date").expected(false).

    useCase(" 22 weeks after claim start date, non hospital").
    scenario(("2010-7-1"), ("2010-1-1"), ("2010-7-1", "2010-11-4", "Reason"), "Non hospital break, first day").expected(true).
    because((processDate: DateTime, claimStartDate: DateTime, dr: DateRange) => processDate.isBefore(dr.from.plusWeeks(4))).
    scenario(("2010-7-20"), ("2010-1-1"), ("2010-7-1", "2010-11-4", "Reason"), "Non hospital break, second day").expected(true).
    scenario(("2010-7-28"), ("2010-1-1"), ("2010-7-1", "2010-11-4", "Reason"), "Non hospital break, last day of four weeks").expected(true).
    scenario(("2010-7-29"), ("2010-1-1"), ("2010-7-1", "2010-11-4", "Reason"), "Non hospital break, first day after four weeks").expected(false).
    because((processDate: DateTime, claimStartDate: DateTime, dr: DateRange) => { val lastDay = dr.from.plusWeeks(4).minusDays(1); processDate.isAfter(lastDay) }).
    scenario(("2010-11-4"), ("2010-1-1"), ("2010-7-1", "2010-11-4", "Reason"), "Non hospital break, last day of break").expected(false).
    scenario(("2010-11-5"), ("2010-1-1"), ("2010-7-1", "2010-11-4", "Reason"), "Non hospital break, first day after break").expected(true).

    useCase(" 22 weeks after claim start date, hospital").
    scenario(("2010-7-1"), ("2010-1-1"), ("2010-7-1", "2010-12-4", "Hospital"), "Hospital break, first day").expected(true).
    scenario(("2010-7-29"), ("2010-1-1"), ("2010-7-1", "2010-11-4", "Hospital"), "Hospital break, first day after four weeks").expected(true).
    because((processDate: DateTime, claimStartDate: DateTime, dr: DateRange) => { val firstInvalidDay = dr.from.plusWeeks(12); dr.reason.equalsIgnoreCase("Hospital") && processDate.isBefore(firstInvalidDay) }).
    scenario(("2010-09-21"), ("2010-1-1"), ("2010-7-1", "2010-12-4", "Hospital"), "Hospital break, last day of twelve weeks 1").expected(true).
    scenario(("2010-09-22"), ("2010-1-1"), ("2010-7-1", "2010-12-4", "Hospital"), "Hospital break, last day of twelve weeks").expected(true).
    scenario(("2010-09-23"), ("2010-1-1"), ("2010-7-1", "2010-12-4", "Hospital"), "Hospital break, first day after twelve weeks").expected(false).
    scenario(("2010-12-4"), ("2010-1-1"), ("2010-7-1", "2010-12-4", "Hospital"), "Hospital break, last day of break").expected(false).
    scenario(("2010-12-5"), ("2010-1-1"), ("2010-7-1", "2010-12-4", "Hospital"), "Hospital break, first day after break").expected(true).
    build

}