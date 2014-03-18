package org.cddcore.income

import org.cddcore.carers.CarersXmlSituation
import org.cddcore.carers.World
import org.cddcore.engine.Engine
import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner
import org.cddcore.carers.Claim

@RunWith(classOf[CddJunitRunner])
object Income {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Claim.getXml(x._2))
  
  val income = Engine[CarersXmlSituation, Double]().title("Income").
    useCase("No income", "A person without any income should return 0 as their income").
    scenario(("2010-3-1", "CL100104A")).expected(0).
    because((c: CarersXmlSituation) => !c.hasEmploymentData()).
    scenario(("2010-3-1", "CL100100A")).expected(0).
    scenario(("2010-3-1", "CL100101A")).expected(0).
    
    useCase("Annually paid", "A person who is annually paid has their annual salary divided by 52 to calculate their income").    
    scenario(("2010-3-1", "CL100113A")).expected(7000.0 / 52).
    because((c: CarersXmlSituation) => c.employmentPayPeriodicity() == "Annually").
    code((c: CarersXmlSituation) => c.employmentGrossSalary() / 52).
    scenario(("2010-3-1", "CL100114A")).expected(10000.0 / 52).

    useCase("Weekly paid").
    scenario(("2010-3-1", "CL100110A")).expected(110).
    because((c: CarersXmlSituation) => c.employmentPayPeriodicity() == "Weekly").
    code((c: CarersXmlSituation) => c.employmentGrossSalary()).    
    scenario(("2010-3-1", "CL100112A")).expected(110).

    build
}