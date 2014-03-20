package org.cddcore.carers

import org.cddcore.engine.Engine
import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner
import scala.language.implicitConversions
import org.joda.time.DateTime

@RunWith(classOf[CddJunitRunner])
object Income {
  implicit def stringStringToCarers(x: String) = CarersXmlSituation(World(), Claim.getXml(x))
  implicit def stringToDate(x: String) = Claim.asDate(x)

  val income = Engine[DateTime, CarersXmlSituation, Double]().title("Income").
    useCase("No income", "A person without any income should return 0 as their income").
    scenario("2010-3-1", "CL100104A").expected(0).
    because((d: DateTime, c: CarersXmlSituation) => !c.hasEmploymentData()).
    scenario("2010-3-1", "CL100100A").expected(0).
    scenario("2010-3-1", "CL100101A").expected(0).

    useCase("Annually paid", "A person who is annually paid has their annual salary divided by 52 to calculate their income").
    scenario("2010-3-1", "CL100113A").expected(7000.0 / 52).
    because((d: DateTime, c: CarersXmlSituation) => c.employmentPayPeriodicity() == "Annually").
    code((d: DateTime, c: CarersXmlSituation) => c.employmentGrossSalary() / 52).
    scenario("2010-3-1", "CL100114A").expected(10000.0 / 52).

    useCase("Weekly paid").
    scenario("2010-3-1", "CL100110A").expected(110).
    because((d: DateTime, c: CarersXmlSituation) => c.employmentPayPeriodicity() == "Weekly").
    code((d: DateTime, c: CarersXmlSituation) => c.employmentGrossSalary()).
    scenario("2010-3-1", "CL100112A").expected(110).

    build

  def main(args: Array[String]) {
    //    println(income("2010-7-25", "CL100108A"))
    //    println(income("2010-3-1", "CL100112A"))
  }

}