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

case class World(dateProcessingData: DateTime, dateOfClaim: DateTime) extends LoggerDisplay {
  def loggerDisplay(dp: LoggerDisplayProcessor): String =
    "World(" + dateOfClaim + ")"
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
}

case class CarersXmlSituation(w: World, validateClaimXml: Elem) extends XmlSituation

@RunWith(classOf[CddJunitRunner])
object Carers {

  val engine = Engine[CarersXmlSituation, KeyAndParams]().
    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println(engine(new CarersXmlSituation(new World(formatter.parseDateTime("2010-3-1"), 
        formatter.parseDateTime("2010-3-1")), Xmls.validateClaim("CL100104A"))))
  }
}
