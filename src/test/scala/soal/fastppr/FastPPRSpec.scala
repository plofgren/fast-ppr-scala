package soal.fastppr

import com.twitter.cassovary.graph.StoredGraphDir
import com.twitter.cassovary.graph.StoredGraphDir.StoredGraphDir
import org.scalatest.{Matchers, FlatSpec}
import com.twitter.cassovary.util.io.AdjacencyListGraphReader
import scala.io.Source

class FastPPRSpec extends FlatSpec with Matchers {
  val reader = new AdjacencyListGraphReader("src/test/resources/", "test_graph.txt") {
    override def storedGraphDir: StoredGraphDir = StoredGraphDir.BothInOut
  }
  val graph = reader.toArrayBasedDirectedGraph()

  "FastPPR.frontier" should "be correct on the test graph" in {
    val config = FastPPRConfiguration.defaultConfiguration
    for (line <- Source.fromFile("src/test/resources/test_graph_true_pprs.txt").getLines()) {
      val pieces = line.split("\t")
      val (uId, vId, ppr) = (pieces(0).toInt, pieces(1).toInt, pieces(2).toFloat)
      val inversePPRs = FastPPR.estimateInversePPR(graph, vId, config, 1.0e-6f)
      withClue ("(%d, %d)".format(uId, vId)) {inversePPRs(uId) should equal (ppr +- 1.0e-6f)}
    }
  }

  "FastPPR.frontierBalanced" should "be correct on the test graph" in {
    var config = FastPPRConfiguration.defaultConfiguration
    config = config.copy(pprSignificanceThreshold = 1.0e-16f) // Choose tiny significance value to test correctness
    for (line <- Source.fromFile("src/test/resources/test_graph_true_pprs.txt").getLines()) {
      val pieces = line.split("\t")
      val (uId, vId, ppr) = (pieces(0).toInt, pieces(1).toInt, pieces(2).toFloat)
      val (inversePPRs, epsilonR) = FastPPR.estimateInversePPRBalanced(graph, vId, config)
      withClue ("(%d, %d)".format(uId, vId)) {inversePPRs(uId) should equal (ppr +- 1.0e-6f)}
    }
  }
}
