package org.cddcore.carers

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.scalatest.selenium.WebBrowser
import org.scalatest.selenium.HtmlUnit
import org.scalatest.BeforeAndAfterAll
import org.eclipse.jetty.server.Server
import net.atos.carers.web.endpoint.ClaimHandler

@RunWith(classOf[JUnitRunner])
class SmokeWebtest extends FlatSpec with ShouldMatchers with HtmlUnit with BeforeAndAfterAll {
  val port = 8090
  val host = s"http://localhost:$port/"
  val claimHandler = new ClaimHandler
  val server = new Server(port);
  server.setHandler(claimHandler);

  override def beforeAll {
    server.start
  }

  override def afterAll {
    server.stop
  }
  
  "Our Rubbishy Website" should "Display a form when it recieves a GET" in {
    go to (host + "index.html")
    pageTitle should be("Validate Claim")

  }
}