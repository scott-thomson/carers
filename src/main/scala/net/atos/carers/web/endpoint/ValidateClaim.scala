package net.atos.carers.web.endpoint

import scala.xml.Elem
import scala.xml.XML

import org.eclipse.jetty.server.Server

object ValidateClaim {

  val claimHandler = new ClaimHandler 

  def getCustId(custXml: String): String = {
    val xmlStr: Elem = XML.loadString(custXml)
    val claimantNode = xmlStr \ "ClaimantData" \ "ClaimantNINO"
    claimantNode.text
  }

  def defaultPort = {
    val portString = System.getenv("PORT")
    println("PortString[" + portString + "]")
    val port = portString match { case null => 8090; case _ => portString.toInt }
    println("Port[" + port + "]")
    port
  }

  def main(args: Array[String]) {
    val s = new Server(defaultPort);
    s.setHandler(claimHandler);
    s.start
    s.join
  }
}