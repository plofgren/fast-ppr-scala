package soal.fastppr

import javax.servlet.http._
import com.twitter.cassovary.util.io.AdjacencyListGraphReader
import com.twitter.cassovary.graph.StoredGraphDir.StoredGraphDir
import com.twitter.cassovary.graph.{DirectedGraph, StoredGraphDir}
import com.twitter.cassovary.graph.StoredGraphDir.StoredGraphDir
import com.twitter.logging.Logger

class FastPPRServlet extends HttpServlet  {
  var graph: DirectedGraph = null
  var fastPPRConfig: FastPPRConfiguration = null
  private lazy val log = Logger.get

  override def init {
    log.info("Reading graph...")
    val reader = new AdjacencyListGraphReader("src/test/resources/", "test_graph.txt") {
      override def storedGraphDir: StoredGraphDir = StoredGraphDir.BothInOut
    }
    graph = reader.toArrayBasedDirectedGraph()
    log.info("Finished Reading graph...")


    // TODO: Read from config file
    fastPPRConfig = FastPPRConfiguration.defaultConfiguration.copy(pprSignificanceThreshold = 1.0f / graph.nodeCount)
  }

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    response.setContentType("application/json")
    response.setCharacterEncoding("UTF-8")
    val startIdString = request.getParameter("startId")
    val targetIdString = request.getParameter("targetId")
    val pprEstimate =
      if (startIdString != null && targetIdString != null &&
        (startIdString matches "\\d+") && (targetIdString matches "\\d+")) {
        val startId = startIdString.toInt
        val targetId = targetIdString.toInt
        val startOption = graph.getNodeById(startId)
        val targetOption = graph.getNodeById(targetId)
        if (startOption.isDefined && targetOption.isDefined) {
           FastPPR.estimatePPR(graph, startId, targetId, fastPPRConfig)
        } else {
          log.warning("startId " + startIdString + " or targetId " + targetId + " not defined in graph.")
          -1.0
        }
      } else {
        log.warning("Query string Missing startId or targetId: " + request.getQueryString)
        -2.0
      }
    val responseJSON = "{ \"pprEstimate\": " + pprEstimate + " }\n"
    response.getWriter.write(responseJSON)
  }
}
