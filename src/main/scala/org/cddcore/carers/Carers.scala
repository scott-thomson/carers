package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine._
import org.cddcore.engine.tests.CddJunitRunner
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.xml._
import org.joda.time.Years

case class KeyAndParams(key: String, params: Any*)

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
  lazy val overSixteen = Carers.overSixteen(claimBirthDate(), w.dateProcessingDate)
  lazy val claim35Hours = xml(validateClaimXml) \ "ClaimData" \ "Claim35Hours" \ yesNo(default = false)
  lazy val ClaimCurrentResidentUK = xml(validateClaimXml) \ "ClaimData" \ "ClaimCurrentResidentUK" \ yesNo(default = false)
  lazy val ClaimEducationFullTime = xml(validateClaimXml) \ "ClaimData" \ "ClaimEducationFullTime" \ yesNo(default = false)
  lazy val ClaimAlwaysUK = xml(validateClaimXml) \ "ClaimData" \ "ClaimAlwaysUK" \ yesNo(default = false)

}

@RunWith(classOf[CddJunitRunner])
object Carers {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Xmls.validateClaim(x._2))
  implicit def stringToDate(x: String) = Xmls.asDate(x)

  val overSixteen = Engine[DateTime, DateTime, Boolean]().title("Over sixteen").
    useCase("Oversixteen").
    scenario("1996-12-10", "2012-12-9").expected(false).
    code((from: DateTime, to: DateTime) => {
      val years = Years.yearsBetween(from, to).getYears;
      years >= 16
    }).
    scenario("1996-12-10", "2012-12-10").expected(true).
    scenario("1996-12-10", "2012-12-11").expected(true).
    build

  val engine = Engine[CarersXmlSituation, KeyAndParams]().title("Validate Claim Rules").
    code((c: CarersXmlSituation) => KeyAndParams("Default Response")).
    useCase("Claimants under the age of 16 are not entitled to claim Carer's Allowance", "Carer’s Allowance is intended for people over the age of 16 who are unable to undertake or continue regular full time employment because they are needed at home to look after a disabled person. Carer’s Allowance is not available to customers under the age of 16.").
    scenario(("2010-7-25", "CL100104A"), "Claimant CL100104 is a child under 16").expected(KeyAndParams("510", "16")).
    because((c: CarersXmlSituation) => !c.overSixteen).
    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    //    println(engine(("2010-7-25", "CL100104A")))
  }
}
