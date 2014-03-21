package org.cddcore.carers

import scala.xml.Elem
import java.net.URL
import scala.io.Source
import scala.xml.XML

class WebserverNinoToCis(host: String) extends NinoToCis {
  def apply(nino: String): Elem = {
    val s = Source.fromURL(host + nino).mkString
    XML.loadString(s)
  }
}

object WebserverNinoToCis {
  val cisHost = "http://localhost:8091/"
  def apply(host: String = cisHost): NinoToCis = new WebserverNinoToCis(host)

  //  def main(args: Array[String]) {
  //    val ninoToCis = WebserverNinoToCis("http://atos-cis.pcfapps.vsel-canopy.com/")
  //    println(ninoToCis("CL100104A"))
  //  }
}