package org.cddcore.carers

import org.junit.runner.RunWith
import org.cddcore.engine.Engine
import org.cddcore.engine.tests.CddJunitRunner
import org.corecdd.website.WebServer
import org.cddcore.engine.LoggerDisplay
import org.cddcore.engine.LoggerDisplayProcessor
import scala.xml.Elem
import org.cddcore.engine.XmlSituation
import org.joda.time.DateTime
import scala.xml.XML
import org.joda.time.format.DateTimeFormat

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

case class CarersXmlSituation(w: World, validateClaimXml: Elem) extends XmlSituation

@RunWith(classOf[CddJunitRunner])
object Carers {

  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Xmls.validateClaim(x._2))

  val engine = Engine[CarersXmlSituation, KeyAndParams]().
    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println(engine(("2010-7-25", "CL100104A")))
  }
}
