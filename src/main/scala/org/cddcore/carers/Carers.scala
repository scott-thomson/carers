package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine._
import org.cddcore.engine.tests.CddJunitRunner
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.xml._
import org.joda.time.Years

case class KeyAndParams(key: String, comment: String, params: Any*)

case class World(dateProcessingDate: DateTime, ninoToCis: NinoToCis = new TestNinoToCis) extends LoggerDisplay {
  def loggerDisplay(dp: LoggerDisplayProcessor): String =
    "World(" + dateProcessingDate + ")"
}

object World {
  def apply(processingDate: String): World = apply(Claim.asDate(processingDate))
}

trait NinoToCis {
  def apply(nino: String): Elem
}

class TestNinoToCis extends NinoToCis {
  def apply(nino: String) =
    try {
      val full = s"Cis/${nino}.txt"
      val url = getClass.getClassLoader.getResource(full)
      if (url == null)
        <NoCis/>
      else {
        val xmlString = scala.io.Source.fromURL(url).mkString
        val xml = XML.loadString(xmlString)
        xml
      }
    } catch {
      case e: Exception => throw new RuntimeException("Cannot load " + nino, e)
    }
}

object Claim {
  def getXml(id: String) = {
    val full = s"ClaimXml/${id}.xml"
    try {
      val url = getClass.getClassLoader.getResource(full)
      val xmlString = scala.io.Source.fromURL(url).mkString
      val xml = XML.loadString(xmlString)
      xml
    } catch {
      case e: Exception => throw new RuntimeException("Cannot load " + id + " fullid is " + full, e)
    }
  }
  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
  def asDate(s: String): DateTime = formatter.parseDateTime(s);
}

case class CarersXmlSituation(w: World, claimXml: Elem) extends XmlSituation {

  import Xml._
  lazy val claimBirthDate = xml(claimXml) \ "ClaimantData" \ "ClaimantBirthDate" \ "PersonBirthDate" \ date("yyyy-MM-dd")
  lazy val claimantUnderSixteen = Carers.checkUnderSixteen(claimBirthDate(), w.dateProcessingDate)
  lazy val claim35Hours = xml(claimXml) \ "ClaimData" \ "Claim35Hours" \ yesNo(default = false)
  lazy val claimCurrentResidentUK = xml(claimXml) \ "ClaimData" \ "ClaimCurrentResidentUK" \ yesNo(default = false)
  lazy val claimEducationFullTime = xml(claimXml) \ "ClaimData" \ "ClaimEducationFullTime" \ yesNo(default = false)
  lazy val claimAlwaysUK = xml(claimXml) \ "ClaimData" \ "ClaimAlwaysUK" \ yesNo(default = false)
  lazy val childCareExpenses = xml(claimXml) \ "ExpensesData" \ "ExpensesChildAmount" \ double
  lazy val hasChildCareExpenses = xml(claimXml) \ "ExpensesData" \ "ExpensesChild" \ yesNo(default = false)
  lazy val occPensionExpenses = xml(claimXml) \ "ExpensesData" \ "ExpensesOccPensionAmount" \ double
  lazy val hasOccPensionExpenses = xml(claimXml) \ "ExpensesData" \ "ExpensesOccPension" \ yesNo(default = false)
  lazy val psnPensionExpenses = xml(claimXml) \ "ExpensesData" \ "ExpensesPsnPensionAmount" \ double
  lazy val hasPsnPensionExpenses = xml(claimXml) \ "ExpensesData" \ "ExpensesPsnPension" \ yesNo(default = false)
  lazy val hasEmploymentData = xml(claimXml) \ "newEmploymentData" \ boolean
  lazy val employmentGrossSalary = xml(claimXml) \ "EmploymentData" \ "EmploymentGrossSalary" \ double
  lazy val employmentPayPeriodicity = xml(claimXml) \ "EmploymentData" \ "EmploymentPayPeriodicity" \ string

  lazy val DependantNino = xml(claimXml) \ "DependantData" \ "DependantNINO" \ string
  lazy val dependantCisXml: Elem = DependantNino.get() match {
    case Some(s) => w.ninoToCis(s);
    case None => <NoDependantXml/>
  }
  lazy val dependantLevelOfQualifyingCare = xml(dependantCisXml) \\ "AwardComponent" \ string
  lazy val dependantHasSufficientLevelOfQualifyingCare = dependantLevelOfQualifyingCare() == "DLA Middle Rate Care"
}

@RunWith(classOf[CddJunitRunner])
object Carers {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Claim.getXml(x._2))
  implicit def stringToDate(x: String) = Claim.asDate(x)

  val checkUnderSixteen = Engine[DateTime, DateTime, Boolean]().title("Check for being under-age (less than age sixteen)").
    useCase("Oversixteen").
    scenario("1996-12-10", "2012-12-9").expected(true).
    code((from: DateTime, to: DateTime) => ((Years.yearsBetween(from, to).getYears) < 16)).
    scenario("1996-12-10", "2012-12-10").expected(false).
    scenario("1996-12-10", "2012-12-11").expected(false).
    build

  val engine = Engine[CarersXmlSituation, KeyAndParams]().title("Validate Claim Rules").
    code((c: CarersXmlSituation) => KeyAndParams("000", "Default Response")).
    useCase("Claimants under the age of 16 are not entitled to claim Carer's Allowance", "Carer's Allowance is intended for people over the age of 16 who are unable to undertake or continue regular full time employment because they are needed at home to look after a disabled person. Carer's Allowance is not available to customers under the age of 16.").
    scenario(("2010-7-25", "CL100104A"), "Claimant CL100104 is a child under 16").
    expected(KeyAndParams("510", "You must be over 16")).
    because((c: CarersXmlSituation) => c.claimantUnderSixteen).

    useCase("Caring Hours", "Claimants must be caring for over 35 hours per week").
    scenario(("2010-7-25", "CL100105A"), "Claimant CL100105 is not caring for 35 hours").
    expected(KeyAndParams("501", "Not caring for 35 hours")).
    because((c: CarersXmlSituation) => !c.claim35Hours()).

    useCase("Claimant Residence", "Claimants must resident and present in the UK").
    scenario(("2010-7-25", "CL100107A"), "Claimant CL100107 is not UK resident").
    expected(KeyAndParams("530", "Not resident in UK")).
    because((c: CarersXmlSituation) => !c.claimAlwaysUK()).

    useCase("Claimant Residence", "Claimants must resident and present in the UK").
    scenario(("2010-7-25", "CL100107A"), "Claimant CL100107 is not UK resident").
    expected(KeyAndParams("530", "Not resident in UK")).
    because((c: CarersXmlSituation) => !c.claimAlwaysUK()).

    useCase("Claimant Current Residence", "Claimants must be currently resident in the UK with no restrictions").
    scenario(("2010-7-25", "CL100108A"), "Claimant CL100108 is not normally UK resident").
    expected(KeyAndParams("534", "Not resident in UK")).
    because((c: CarersXmlSituation) => !c.claimCurrentResidentUK()).

    useCase("Claimant in Full Time Education", "Claimants must not be in full time education (over 21 hours/week)").
    scenario(("2010-7-25", "CL100109A"), "Claimant CL100109 is in full time education").
    expected(KeyAndParams("513", "Claimant in full time education")).
    because((c: CarersXmlSituation) => c.claimEducationFullTime()).

    //    useCase("Claimant ...", "Claimants must ...").
    //    scenario(("2010-7-25", "CL100XXXA"), "Claimant CL100xxx is ").
    //    expected(KeyAndParams("534", "Not ...")).
    //    because((c: CarersXmlSituation) => !c.claimXXX()).

    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println(engine(("2010-7-25", "CL100108A")))
  }
}
