package org.cddcore.carers

import scala.xml.Elem
import java.net.URL
import scala.io.Source
import scala.xml.XML

class WebserverNinoToCis(host: String) extends NinoToCis {
  def apply(nino: String): Elem = {
    val url = new URL(host + nino)
    val stream = url.openStream()
    try {
      val s = Source.fromInputStream(stream).mkString
      XML.loadString(s)
    } finally {
      stream.close
    }
  }
}

object WebserverNinoToCis {
  val cisHost = "http://localhost:8091/"
  def main(args: Array[String]) {
    val ninoToCis = new WebserverNinoToCis(cisHost)
    println(ninoToCis("CL100104A"))
  }
}