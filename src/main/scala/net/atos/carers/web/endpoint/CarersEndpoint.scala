package net.atos.carers.web.endpoint

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import scala.xml.Elem
import scala.xml.Node
import scala.xml.XML

object CarersEndpoint {
  private val MethodPost: String = "POST";

  private val MethodGet: String = "GET";

  val carersHandler = new AbstractHandler {
    def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
      System.out.println("In Carers handle: " + request.getMethod)

      if (request.getMethod() == MethodPost)
        handlePost(request, response)
      else
        handleGet(request, response)

      baseRequest.setHandled(true)
    }
  }

  def handleGet(request: HttpServletRequest, response: HttpServletResponse) {
    response.setContentType("text/html;charset=utf-8")
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(getCarerView(""));
    response.flushBuffer();
  }

  def handlePost(request: HttpServletRequest, response: HttpServletResponse) {
    val custXml = request.getParameter("custxml")
    callCis(getCustId(custXml))
    response.setContentType("text/html;charset=utf-8")
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(getCarerView(custXml));
    response.flushBuffer();
  }

  def getCustId(custXml: String): String = {
    val xmlStr: Elem = XML.loadString(custXml)

    val claimantNode = xmlStr \ "ClaimantData" \ "ClaimantNINO"
    
    System.out.println("Claimant NINO " + claimantNode.text)

    claimantNode.text
  }

  def callCis(id: String) {
    val cisXml: Elem = <cisResponse>Hello from CIS</cisResponse>
  }

  def getCarerView(xmlString: String): Elem =
    <html>
      <head>
        <title>Carers Allowance</title>
        <h1>Carers Allowance Claim</h1>
      </head>
      <body>
        <form action="http://localhost:8090" method="POST">
          <table>
            <tr>
              <td>
                <textarea name="custxml">{ xmlString }</textarea>
              </td>
              <td>
                <input type="submit" value="Submit"/>
              </td>
            </tr>
          </table>
        </form>
      </body>
    </html>

  def main(args: Array[String]) {
    val s = new Server(8090);
    s.setHandler(carersHandler);
    s.start
    s.join
  }
}