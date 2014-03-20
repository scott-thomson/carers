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
import java.util.concurrent.atomic.AtomicInteger
import net.atos.carers.web.endpoint.ValidateClaimServer

object SmokeWebtest {
  val port = new AtomicInteger(8090)

}
@RunWith(classOf[JUnitRunner])
class SmokeWebtest extends FlatSpec with ShouldMatchers with HtmlUnit with BeforeAndAfterAll {
  import SmokeWebtest._
  val localPort = port.getAndIncrement()
  val host = s"http://localhost:$localPort/"
  val server = ValidateClaimServer(localPort)

  override def beforeAll {
    server.start
  }

  override def afterAll {
    server.stop
  }

  "The default port method" should "return 8090 if PORT isn't set" in {
    System.getenv("PORT") should equal(null)
    ValidateClaimServer.defaultPort should equal(8090)
  }

  "Our Rubbishy Website" should "Display a form when it recieves a GET" in {
    go to (host + "index.html")
    pageTitle should be("Validate Claim")

  }
}