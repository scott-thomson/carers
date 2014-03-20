package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine._
import org.cddcore.engine.tests.CddJunitRunner
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.xml._
import org.joda.time.Years
import scala.language.implicitConversions

case class KeyAndParams(key: String, comment: String, params: Any*)

case class World(ninoToCis: NinoToCis = new TestNinoToCis) extends LoggerDisplay {
  def loggerDisplay(dp: LoggerDisplayProcessor): String =
    "World()"
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

  /** The boolean is 'hospitalisation' */
  def validateClaimWithBreaks(breaks: (String, String, Boolean)*): CarersXmlSituation =
    validateClaimWithBreaksFull(breaks.map((x) => (x._1, x._2, if (x._3) "Hospitalisation" else "other", if (x._3) "Hospital" else "other")): _*)

  def validateClaimWithBreaksFull(breaks: (String, String, String, String)*): CarersXmlSituation = {
    val url = getClass.getClassLoader.getResource("ClaimXml/CL801119A.xml")
    val xmlString = scala.io.Source.fromURL(url).mkString
    val breaksInCareXml = <ClaimBreaks>
                            {
                              breaks.map((t) =>
                                <BreakInCare>
                                  <BICFromDate>{ t._1 }</BICFromDate>
                                  <BICToDate>{ t._2 }</BICToDate>
                                  <BICReason>{ t._3 }</BICReason>
                                  <BICType>{ t._4 }</BICType>
                                </BreakInCare>)
                            }
                          </ClaimBreaks>
    val withBreaks = xmlString.replace("<ClaimBreaks />", breaksInCareXml.toString)
    new CarersXmlSituation(World(new TestNinoToCis), XML.loadString(withBreaks))
  }

  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
  def asDate(s: String): DateTime = formatter.parseDateTime(s);
}

case class CarersXmlSituation(world: World, claimXml: Elem) extends XmlSituation {

  import Xml._
  lazy val claimBirthDate = xml(claimXml) \ "ClaimantData" \ "ClaimantBirthDate" \ "PersonBirthDate" \ date("yyyy-MM-dd")
  def claimantUnderSixteen(d: DateTime) = Carers.checkUnderSixteen(claimBirthDate(), d)
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
    case Some(s) => world.ninoToCis(s);
    case None => <NoDependantXml/>
  }

  lazy val dependantLevelOfQualifyingCare = xml(dependantCisXml) \\ "AwardComponent" \ string
  lazy val dependantHasSufficientLevelOfQualifyingCare = dependantLevelOfQualifyingCare() == "DLA Middle Rate Care"

  lazy val claimStartDate = xml(claimXml) \ "ClaimData" \ "ClaimStartDate" \ date
  lazy val timeLimitForClaimingThreeMonths = claimSubmittedDate().minusMonths(3)
  lazy val claimEndDate = xml(claimXml) \ "ClaimData" \ "ClaimEndDate" \ optionDate
  lazy val claimSubmittedDate = xml(claimXml) \ "StatementData" \ "StatementDate" \ date
  lazy val dependantAwardStartDate = xml(dependantCisXml) \ "Award" \ "AssessmentDetails" \ "ClaimStartDate" \ optionDate

  def income(d: DateTime) = Income.income(d, this)
  def expenses(d: DateTime) = Expenses.expenses(d, this)
  def netIncome(d: DateTime) = income(d) - expenses(d)

  lazy val awardList = xml(dependantCisXml) \ "Award" \
    obj((group) => group.map((n) => {
      val benefitType = (n \ "AssessmentDetails" \ "BenefitType").text
      val awardComponent = (n \ "AwardComponents" \ "AwardComponent").text
      val claimStatus = (n \ "AssessmentDetails" \ "ClaimStatus").text
      val awardStartDate = Claim.asDate((n \ "AwardDetails" \ "AwardStartDate").text)
      Award(benefitType, awardComponent, claimStatus, awardStartDate)
    }).toList)

  lazy val breaksInCare = xml(claimXml) \ "ClaimData" \ "ClaimBreaks" \ "BreakInCare" \
    obj((ns) => ns.map((n) => {
      val from = Claim.asDate((n \ "BICFromDate").text)
      val to = Claim.asDate((n \ "BICToDate").text)
      val reason = (n \ "BICType").text
      new DateRange(from, to, reason)
    }))

  def isThereAnyQualifyingBenefit(d: DateTime) = awardList().foldLeft[Boolean](false)((acc, a) => acc || Carers.checkQualifyingBenefit(d, a))
}

case class Award(benefitType: String, awardComponent: String, claimStatus: String, awardStartDate: DateTime)

@RunWith(classOf[CddJunitRunner])
object Carers {
  implicit def stringStringToCarers(x: String) = CarersXmlSituation(World(), Claim.getXml(x))
  implicit def stringToDate(x: String) = Claim.asDate(x)
  implicit def stringToOptionDate(x: String) = Some(Claim.asDate(x))

  val checkUnderSixteen = Engine[DateTime, DateTime, Boolean]().title("Check for being under-age (less than age sixteen)").
    useCase("Oversixteen").
    scenario("1996-12-10", "2012-12-9").expected(true).
    code((from: DateTime, to: DateTime) => ((Years.yearsBetween(from, to).getYears) < 16)).
    scenario("1996-12-10", "2012-12-10").expected(false).
    scenario("1996-12-10", "2012-12-11").expected(false).
    build

  implicit def toAward(x: (String, String, String, String)) = Award(x._1, x._2, x._3, Claim.asDate(x._4))

  val checkQualifyingBenefit = Engine[DateTime, Award, Boolean]().title("Check for qualifying Benefit").

    useCase("QB not in payment or future dated").
    scenario("2010-05-28", ("AA", "AA lower rate", "Active", "2010-05-27"), "QB start date in past").expected(true).
    scenario("2010-05-28", ("AA", "AA lower rate", "Active", "2010-05-28"), "QB start date exact").expected(true).
    because((d: DateTime, a: Award) => !d.isBefore(a.awardStartDate) && a.claimStatus == "Active").

    scenario("2010-05-28", ("AA", "AA lower rate", "Active", "2010-05-29"), "QB start date in future").expected(false).
    because((d: DateTime, a: Award) => d.isBefore(a.awardStartDate)).
    scenario("2010-05-28", ("AA", "AA lower rate", "Inactive", "2010-05-01"), "QB not in payment").expected(false).
    because((d: DateTime, a: Award) => a.claimStatus != "Active").

    scenario("2010-05-28", ("ZZ", "AA lower rate", "Active", "2010-05-01"), "QB is one of AA/DLA/CAA").expected(false).
    because((d: DateTime, a: Award) => a.benefitType match {
      case "AA" | "DLA" | "CAA" => false
      case _ => true
    }).

    build

  val engine = Engine[DateTime, CarersXmlSituation, KeyAndParams]().title("Validate Claim Rules").
    code((d: DateTime, c: CarersXmlSituation) => KeyAndParams("000", "Default Response")).
    useCase("Claimants under the age of 16 are not entitled to claim Carer's Allowance", "Carer's Allowance is intended for people over the age of 16 who are unable to undertake or continue regular full time employment because they are needed at home to look after a disabled person. Carer's Allowance is not available to customers under the age of 16.").
    scenario("2010-7-25", "CL100104A", "Claimant CL100104 is a child under 16").
    expected(KeyAndParams("510", "You must be over 16")).
    because((d: DateTime, c: CarersXmlSituation) => c.claimantUnderSixteen(d)).

    useCase("Caring Hours", "Claimants must be caring for over 35 hours per week").
    scenario("2010-7-25", "CL100105A", "Claimant CL100105 is not caring for 35 hours").
    expected(KeyAndParams("501", "Not caring for 35 hours")).
    because((d: DateTime, c: CarersXmlSituation) => !c.claim35Hours()).

    useCase("Claimant Residence", "Claimants must resident and present in the UK").
    scenario("2010-7-25", "CL100107A", "Claimant CL100107 is not UK resident").
    expected(KeyAndParams("530", "Not resident in UK")).
    because((d: DateTime, c: CarersXmlSituation) => !c.claimAlwaysUK()).

    useCase("Claimant Current Residence", "Claimants must be currently resident in the UK with no restrictions").
    scenario("2010-7-25", "CL100108A", "Claimant CL100108 is not normally UK resident").
    expected(KeyAndParams("534", "Not resident in UK")).
    because((d: DateTime, c: CarersXmlSituation) => !c.claimCurrentResidentUK()).

    useCase("Claimant in Full Time Education", "Claimants must not be in full time education (over 21 hours/week)").
    scenario("2010-7-25", "CL100109A", "Claimant CL100109 is in full time education").
    expected(KeyAndParams("513", "Claimant in full time education")).
    because((d: DateTime, c: CarersXmlSituation) => c.claimEducationFullTime()).

    useCase("Qualifying Benefit", "The person for whom the customer is caring (the disabled person) must be " +
      " receiving payment of either Attendance Allowance (AA), Disability Living Allowance (DLA) " +
      "care component (middle or highest rate), or Constant Attendance Allowance (CAA) " +
      "(at least full day rate).").
    scenario("2010-7-25", "CL100106A", "Dependent party without qualifying benefit").
    expected(KeyAndParams("503", "Dependent doesn't have a Qualifying Benefit")).
    because((d: DateTime, c: CarersXmlSituation) => !c.isThereAnyQualifyingBenefit(d)).

    scenario("2010-7-25", "CL100101A", "Dependent party with suitable qualifying benefit").
    expected(KeyAndParams("ENT", "Dependent award is valid on date")).
    because((d: DateTime, c: CarersXmlSituation) => c.isThereAnyQualifyingBenefit(d)).

    useCase("Claimant Income and Expenses", "Claimants must have a limit to income offsetting expenses").
    scenario("2010-7-25", "CL100113A", "Customers with income exceeding the threshold are not entitled to CA").
    expected(KeyAndParams("520", "Too much income")).
    because((d: DateTime, c: CarersXmlSituation) => c.netIncome(d) > 95).
    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println("CL100111A": CarersXmlSituation)
  }

}
