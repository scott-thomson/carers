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

case class CarersXmlSituation(world: World, claimXml: Elem) extends XmlSituation {

  import Xml._
  lazy val claimBirthDate = xml(claimXml) \ "ClaimantData" \ "ClaimantBirthDate" \ "PersonBirthDate" \ date("yyyy-MM-dd")
  lazy val claimantUnderSixteen = Carers.checkUnderSixteen(claimBirthDate(), world.dateProcessingDate)
  lazy val claim35Hours = xml(claimXml) \ "ClaimData" \ "Claim35Hours" \ yesNo(default = false)
  lazy val claimCurrentResidentUK = xml(claimXml) \ "ClaimData" \ "ClaimCurrentResidentUK" \ yesNo(default = false)
  lazy val claimEducationFullTime = xml(claimXml) \ "ClaimData" \ "ClaimEducationFullTime" \ yesNo(default = false)
  lazy val claimAlwaysUK = xml(claimXml) \ "ClaimData" \ "ClaimAlwaysUK" \ yesNo(default = false)

  lazy val DependantNino = xml(claimXml) \ "DependantData" \ "DependantNINO" \ string
  lazy val dependantCisXml: Elem = DependantNino.get() match {
    case Some(s) => world.ninoToCis(s);
    case None => <NoDependantXml/>
  }

  lazy val awardList = xml(dependantCisXml) \ "Award" \
    obj((group) => group.map((n) => {
      val benefitType = (n \ "AssessmentDetails" \ "BenefitType").text
      val awardComponent = (n \ "AwardComponents" \ "AwardComponent").text
      val claimStatus = (n \ "AssessmentDetails" \ "ClaimStatus").text
      val awardStartDate = Claim.asDate((n \ "AwardDetails" \ "AwardStartDate").text)
      Award(benefitType, awardComponent, claimStatus, awardStartDate)
    }).toList)

  def isThereAnyQualifyingBenefit(d:DateTime) = awardList().foldLeft[Boolean](false) ((acc,a) => acc || Carers.checkQualifyingBenefit(d,a))
}

case class Award(benefitType: String, awardComponent: String, claimStatus: String, awardStartDate: DateTime)

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

  implicit def toAward(x: (String, String, String, String)) = Award(x._1, x._2, x._3, Claim.asDate(x._4))

  val checkQualifyingBenefit = Engine[DateTime, Award, Boolean]().title("Check for qualifying Benefit").
  
    useCase("QB not in payment or future dated").
    scenario("2010-05-28", ("AA", "AA lower rate", "Active", "2010-05-27"), "QB start date in past").expected(true).
    scenario("2010-05-28", ("AA", "AA lower rate", "Active", "2010-05-28"), "QB start date exact").expected(true).
    because((d: DateTime, a: Award) => !d.isBefore(a.awardStartDate) && a.claimStatus == "Active").

    scenario("2010-05-28", ("AA", "AA lower rate", "Active", "2010-05-29"), "QB start date in future").expected(false).
    because((d: DateTime, a: Award) => d.isBefore(a.awardStartDate) ).
    scenario("2010-05-28", ("AA", "AA lower rate", "Inactive", "2010-05-01"), "QB not in payment").expected(false).
    because((d: DateTime, a: Award) => a.claimStatus != "Active").

    scenario("2010-05-28", ("ZZ", "AA lower rate", "Active", "2010-05-01"), "QB is one of AA/DLA/CAA").expected(false).
    because((d: DateTime, a: Award) => a.benefitType match {
      case "AA" | "DLA" | "CAA" => false
      case _ => true} ).
    
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

    useCase("Claimant Current Residence", "Claimants must be currently resident in the UK with no restrictions").
    scenario(("2010-7-25", "CL100108A"), "Claimant CL100108 is not normally UK resident").
    expected(KeyAndParams("534", "Not resident in UK")).
    because((c: CarersXmlSituation) => !c.claimCurrentResidentUK()).

    useCase("Claimant in Full Time Education", "Claimants must not be in full time education (over 21 hours/week)").
    scenario(("2010-7-25", "CL100109A"), "Claimant CL100109 is in full time education").
    expected(KeyAndParams("513", "Claimant in full time education")).
    because((c: CarersXmlSituation) => c.claimEducationFullTime()).

    useCase("Qualifying Benefit", "The person for whom the customer is caring (the disabled person) must be " +
      " receiving payment of either Attendance Allowance (AA), Disability Living Allowance (DLA) " +
      "care component (middle or highest rate), or Constant Attendance Allowance (CAA) " +
      "(at least full day rate).").
    scenario(("2010-7-25", "CL100106A"), "Dependent party without qualifying benefit").
    expected(KeyAndParams("503", "Dependent doesn't have a Qualifying Benefit")).
    because((c: CarersXmlSituation) => !c.isThereAnyQualifyingBenefit(c.world.dateProcessingDate)).
    
    scenario(("2010-7-25", "CL100101A"), "Dependent party with suitable qualifying benefit").
    expected(KeyAndParams("ENT", "Dependent award is valid on date")).
    because((c: CarersXmlSituation) => c.isThereAnyQualifyingBenefit(c.world.dateProcessingDate)).

    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println(engine(("2010-7-25", "CL100108A")))
  }
}
