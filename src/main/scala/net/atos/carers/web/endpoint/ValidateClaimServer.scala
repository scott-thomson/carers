package net.atos.carers.web.endpoint

import scala.xml.Elem
import scala.xml.XML

import org.eclipse.jetty.server.Server

object ValidateClaimServer {

  val claimHandler = new ClaimHandler

  def apply(port: Int = defaultPort) = {
    val s = new Server(port);
    s.setHandler(claimHandler);
    s
  }

  def defaultPort = {
    val portString = System.getenv("PORT")
    println("PortString[" + portString + "]")
    val port = portString match { case null => 8090; case _ => portString.toInt }
    println("Port[" + port + "]")
    port
  }

  def main(args: Array[String]) {
    val s = apply()
    s.start
    s.join
  }
}