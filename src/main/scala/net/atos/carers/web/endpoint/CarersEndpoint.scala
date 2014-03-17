package net.atos.carers.web.endpoint

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object CarersEndpoint {
  private val MethodPost: String = "POST";

  private val MethodGet: String = "GET";

  val carersHandler = new AbstractHandler {
    def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
      System.out.println("In CIS handle: " + request.getMethod)
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
    response.getWriter().println("<h1>Carers Get Hello</h1>");
    response.flushBuffer();
  }

  def handlePost(request: HttpServletRequest, response: HttpServletResponse) {
    response.setContentType("text/html;charset=utf-8")
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println("<h1>Carers Post Hello</h1>");
    response.flushBuffer();
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
    s.setHandler(carersHandler);
    s.start
    s.join
  }
}