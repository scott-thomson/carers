package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner
import org.cddcore.engine.Engine
import org.joda.time.format.DateTimeFormat
import org.cddcore.engine.UseCase
import org.cddcore.engine.Xml
import scala.language.implicitConversions
import org.joda.time.DateTime

@RunWith(classOf[CddJunitRunner])
object Expenses {
  implicit def stringStringToCarers(x: String) = CarersXmlSituation(World(), Claim.getXml(x))
  implicit def stringToDate(x: String) = Claim.asDate(x)

  val expenses = Engine.folding[DateTime, CarersXmlSituation, Double, Double]((acc, v) => acc + v, 0).
    title("Expenses").
    code((d: DateTime, c: CarersXmlSituation) => 0.0).
    childEngine("Child care expenses", """Customer's claiming CA may claim an allowable expense of up to 50% of their childcare expenses
        where the child care is not being undertaken by a direct relative. This amount may then be deducted from their gross pay.""").
    useCase("Customer has child care expenses").
    scenario("2010-3-1", "CL100110A").expected(15).
    code((d: DateTime, c: CarersXmlSituation) => {
      if (c.hasChildCareExpenses())
        c.childCareExpenses() / 2
      else
        0
    }).
    useCase("Customer has no child care expenses").
    scenario("2010-3-1", "CL100112A").expected(0).
    useCase("Customer has no child care expenses data").
    scenario("2010-3-1", "CL100101A").expected(0).

    childEngine("PSN  Pensions", """Customers claiming CA may claim an allowable expense of up to 50% of their Private Pension contributions. 
        This amount may then be deducted from their gross pay figure.""").
    useCase("Customer has PSN Pension").
    scenario("2010-3-1", "CL100111A").expected(15).
    code((d: DateTime, c: CarersXmlSituation) => if (c.hasPsnPensionExpenses())
      c.psnPensionExpenses() / 2
    else
      0).
    useCase("Customer has no PSN Pension").
    scenario("2010-3-1", "CL100112A").expected(0).
    useCase("Customer has no PSN Pension data").
    scenario("2010-3-1", "CL100101A").expected(0).

    childEngine("Occupational Pension",
      """Customers claiming CA may claim an allowable expense of up to 50% of their Occupational Pension contributions. 
        This amount may then be deducted from their gross pay figure.""").
      useCase("Customer has Occupational Pension").
      scenario("2010-3-1", "CL100112A").expected(15).
      code((d: DateTime, c: CarersXmlSituation) => if (c.hasOccPensionExpenses())
        c.occPensionExpenses() / 2
      else
        0).
      useCase("Customer has no Occupational pension").
      scenario("2010-3-1", "CL100111A").expected(0).
      useCase("Customer has no Occupational pension data").
      scenario("2010-3-1", "CL100101A").expected(0).
      build
}