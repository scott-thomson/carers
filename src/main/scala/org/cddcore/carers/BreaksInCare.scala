package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner
import org.cddcore.engine.Engine
import org.joda.time.DateTime
import scala.language.implicitConversions

@RunWith(classOf[CddJunitRunner])
object BreaksInCare {

  implicit def stringToDate(x: String) = (Claim.asDate(x))

  implicit def toValidateClaim(x: List[(String, String, Boolean)]): CarersXmlSituation = Claim.validateClaimWithBreaks(x: _*)

  def breaksInCare = Engine[DateTime, CarersXmlSituation, Boolean]().title("Breaks in care").
    description("This works out if any given break in care (specified by the DateRange) still allows payment. For reference the validateClaimWithBreaks method " +
      "creates a validate claims application form with a claims start date of 2010-01-01. The 22 week enabler for breaks in care occurs on 2010-06-04").

    useCase("The datetime is outside any break in care, means that payment is OK").
    scenario(("2010-05-12"), List(("2010-5-13", "2010-6-13", true)), "Just before break").expected(true).
    code((d: DateTime, c: CarersXmlSituation) => {
      val startDate = c.claimStartDate()
      c.breaksInCare().foldLeft(true)((acc, dr) => acc && BreakInCare.singleBreakInCare(d, startDate, dr))
    }).
    scenario(("2010-06-14"), List(("2010-5-13", "2010-6-13", true)), "Just after break").expected(true).
    scenario(("2010-05-12"), List(("2010-5-13", "2010-6-13", true), ("2010-6-1", "2010-6-2", true)), "Just before break, multiple breaks").expected(true).
    scenario(("2010-06-14"), List(("2010-5-13", "2010-6-13", true), ("2010-6-1", "2010-6-2", true)), "Just after break, multiple breaks").expected(true).

    useCase("The datetime is in a break in care (dependant in hospital), and the care payments were made for 22 weeks pre care, and the break is less than 12 weeks").
    scenario(("2010-7-1"), List(("2010-7-1", "2010-9-22", true)), "First day of break that is one day short of 12 weeks").expected(true).
    scenario(("2010-9-22"), List(("2010-7-1", "2010-9-22", true)), "Last day of break that is one day short of 12 weeks").expected(true).

    useCase("The datetime is in a break in care (dependant not in hospital), and the care payments were made for 22 weeks pre care, and the break is less than 4 weeks").
    scenario(("2010-7-1"), List(("2010-7-1", "2010-7-10", false)), "First day of break that is one day short of 4 weeks").expected(true).
    scenario(("2010-7-10"), List(("2010-7-1", "2010-7-10", false)), "Last day of break that is one day short of 4 weeks").expected(true).

    useCase("The datetime is in a break in care (dependant not in hospital), and the care payments were made for 22 weeks pre care, and the break is more than 4 weeks").
    scenario(("2010-7-1"), List(("2010-7-1", "2010-08-02", false)), "First day of break that is over 4 weeks").expected(true).
    scenario(("2010-7-28"), List(("2010-7-1", "2010-08-02", false)), "Last valid day of break that is over 4 weeks").expected(true).
    scenario(("2010-7-29"), List(("2010-7-1", "2010-08-02", false)), "First invalid day of break that is over 4 weeks").expected(false).
    scenario(("2010-8-2"), List(("2010-7-1", "2010-08-02", false)), "Last day of break that is over 4 weeks").expected(false).
    scenario(("2010-8-3"), List(("2010-7-1", "2010-08-02", false)), "After break that is over 4 weeks").expected(true).

    useCase("The datetime is in a break in care (dependant in hospital), and the care payments were made for 22 weeks pre care, and the break is more than 12 weeks").
    scenario(("2010-6-04"), List(("2010-6-4", "2010-9-1", true)), "First day of break that is over 12 weeks").expected(true).
    scenario(("2010-8-25"), List(("2010-6-4", "2010-9-1", true)), "Last valid day of break that is over 12 weeks1").expected(true).
    scenario(("2010-8-26"), List(("2010-6-4", "2010-9-1", true)), "Last valid day of break that is over 12 weeks").expected(true).
    scenario(("2010-8-27"), List(("2010-6-4", "2010-9-1", true)), "First invalid day of break that is over 12 weeks").expected(false).
    scenario(("2010-9-1"), List(("2010-6-4", "2010-9-1", true)), "Last day of break that is over 12 weeks").expected(false).
    scenario(("2010-9-2"), List(("2010-6-4", "2010-9-1", true)), "After break that is over 12 weeks").expected(true).

    useCase("The datetime is in a break in care and the care payments were not made for 22 weeks pre care").
    scenario(("2010-2-2"), List(("2010-3-1", "2010-3-3", true)), "before a break that is pre 22 weeks").expected(true).
    scenario(("2010-3-1"), List(("2010-3-1", "2010-3-3", true)), "first day of a break that is pre 22 weeks").expected(false).
    scenario(("2010-3-3"), List(("2010-3-1", "2010-3-3", true)), "last day a break that is pre 22 weeks").expected(false).
    scenario(("2010-3-4"), List(("2010-3-1", "2010-3-3", true)), "after a break that is pre 22 weeks").expected(true).
    build
}