package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine._
import org.cddcore.engine.tests.CddJunitRunner
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.xml._
import org.joda.time.Years

case class KeyAndParams(key: String, comment: String, params: Any*)

case class World(dateProcessingDate: DateTime) extends LoggerDisplay {
  def loggerDisplay(dp: LoggerDisplayProcessor): String =
    "World(" + dateProcessingDate + ")"
}

object World {
  def apply(processingDate: String): World = apply(Xmls.asDate(processingDate))
}

object Xmls {

  def validateClaim(id: String) = {
    try {
      val full = s"ValidateClaim/${id}.xml"
      val url = getClass.getClassLoader.getResource(full)
      val xmlString = scala.io.Source.fromURL(url).mkString
      val xml = XML.loadString(xmlString)
      xml
    } catch {
      case e: Exception => throw new RuntimeException("Cannot load " + id, e)
    }
  }
  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
  def asDate(s: String): DateTime = formatter.parseDateTime(s);
}

case class CarersXmlSituation(w: World, validateClaimXml: Elem) extends XmlSituation {

  import Xml._
  lazy val claimBirthDate = xml(validateClaimXml) \ "ClaimantData" \ "ClaimantBirthDate" \ "PersonBirthDate" \ date("yyyy-MM-dd")
  lazy val claimantUnderSixteen = Carers.checkUnderSixteen(claimBirthDate(), w.dateProcessingDate)
  lazy val claim35Hours = xml(validateClaimXml) \ "ClaimData" \ "Claim35Hours" \ yesNo(default = false)
  lazy val claimCurrentResidentUK = xml(validateClaimXml) \ "ClaimData" \ "ClaimCurrentResidentUK" \ yesNo(default = false)
  lazy val claimEducationFullTime = xml(validateClaimXml) \ "ClaimData" \ "ClaimEducationFullTime" \ yesNo(default = false)
  lazy val claimAlwaysUK = xml(validateClaimXml) \ "ClaimData" \ "ClaimAlwaysUK" \ yesNo(default = false)
  lazy val childCareExpenses = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesChildAmount" \ double
  lazy val hasChildCareExpenses = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesChild" \ yesNo(default = false)
  lazy val occPensionExpenses = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesOccPensionAmount" \ double
  lazy val hasOccPensionExpenses = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesOccPension" \ yesNo(default = false)
  lazy val psnPensionExpenses = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesPsnPensionAmount" \ double
  lazy val hasPsnPensionExpenses = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesPsnPension" \ yesNo(default = false)
}

@RunWith(classOf[CddJunitRunner])
object Carers {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Xmls.validateClaim(x._2))
  implicit def stringToDate(x: String) = Xmls.asDate(x)

  val checkUnderSixteen = Engine[DateTime, DateTime, Boolean]().title("Over sixteen").
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
