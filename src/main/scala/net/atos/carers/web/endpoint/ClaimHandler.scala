package net.atos.carers.web.endpoint

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.AbstractHandler
import scala.xml.Elem
import org.cddcore.carers.Carers
import org.cddcore.carers.Claim
import org.cddcore.carers.World
import org.cddcore.carers.CarersXmlSituation
import scala.xml.XML
import org.cddcore.carers.TimeLineCalcs
import org.cddcore.carers.TimeLineItem

class ClaimHandler extends AbstractHandler {
  private val MethodPost: String = "POST";

  private val MethodGet: String = "GET";

  def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
    response.setContentType("text/html;charset=utf-8")
    response.setStatus(HttpServletResponse.SC_OK);
    try {
      request.getMethod match {
        case MethodPost => response.getWriter().println(handlePost(request.getParameter("custxml"), request.getParameter("claimDate")))
        case MethodGet => response.getWriter().println(handleGet)
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace(response.getWriter);
        e.printStackTrace()
        println()
        e.getCause().printStackTrace()
        val props = System.getProperties();
        props.list(System.err);
        props.list(response.getWriter());
        response.getWriter().println("Class Path = ")
        response.getWriter().println(props.get("java.class.path"))
    }
    baseRequest.setHandled(true)
  }

  def handleGet() = getCarerView("", getDefaultDate);

  def handlePost(custXml: String, claimDate: String) = {
    println("In handle post 1 ")

    val world = World()
    val xml = try { XML.loadString(custXml) } catch { case e: Throwable => e.printStackTrace(); throw e }
    val situation = CarersXmlSituation(world, xml)

    val result = claimDate.isEmpty() match {
      case true => {
        val timeLine: List[TimeLineItem] = TimeLineCalcs.findTimeLine(situation)
        println("empty date")
        <div id='timeLine'>{
          timeLine.map((tli) => <p>{ tli }</p>)
        }</div>
      }
      case _ => {
        println("not empty date")
        val dateTime = Claim.asDate(claimDate)
        val result = Carers.engine(dateTime, situation)
        <div id='oneTime'>{ result }</div>
      }
    }

    println("In handle post 2: " + result)
    //    //CDD Business logic will return a return message - hard coded for now   
    //    val returnMessage = result.toString

    getCarerView(custXml, result, claimDate)
  }

  val operatingPort = System.getenv("PORT")

  def getCarerView(xmlString: String, claimDate: String): Elem =
    <html>
      <head>
        <title>Validate Claim</title>
      </head>
      <body>
        <form action="/" method="POST">
          <h1>Validate Claim { operatingPort }</h1>
          <table>
            <tr>
              <td>
                Claim Xml:
              </td>
              <td>
                <textarea name="custxml">{ xmlString }</textarea>
              </td>
            </tr>
            <tr>
              <td>
                Claim Date:
              </td>
              <td>
                <input type="text" name="claimDate" value={ claimDate }/>
              </td>
              <td>
                <input type="submit" value="Submit"/>
              </td>
            </tr>
          </table>
        </form>
      </body>
    </html>

  def getCarerView(xmlString: String, returnMessage: Elem, claimDate: String): Elem =
    <html>
      <head>
        <title>Validate Claim</title>
      </head>
      <body>
        <form action="/" method="POST">
          <h1>Validate Claim</h1>
          <table>
            <tr>
              <td>
                Claim Xml:
              </td>
              <td>
                <textarea name="custxml">{ xmlString }</textarea>
              </td>
            </tr>
            <tr>
              <td>
                Claim Date:
              </td>
              <td>
                <input type="text" name="claimDate" value={ claimDate }/>
              </td>
              <td>
                <input type="submit" value="Submit"/>
              </td>
            </tr>
          </table>
          <br/>
          <pre>{ returnMessage }</pre>
        </form>
      </body>
    </html>

  def getDefaultDate: String = {
    "2010-07-25"
  }
}