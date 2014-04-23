package soal.fastppr

import com.twitter.cassovary.graph.{DirectedGraph, StoredGraphDir}
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
    val pprErrorTolerance = 2.0e-6f
    for (line <- Source.fromFile("src/test/resources/test_graph_true_pprs.txt").getLines()) {
      val pieces = line.split("\t")
      val (startId, targetId, truePPR) = (pieces(0).toInt, pieces(1).toInt, pieces(2).toFloat)
      val inversePPRs = FastPPR.estimateInversePPR(graph, targetId, config, pprErrorTolerance)
      withClue ("(%d, %d)".format(startId, targetId)) {inversePPRs(startId) should equal (truePPR +- pprErrorTolerance)}
    }
  }

  "FastPPR.frontierBalanced" should "be correct on the test graph" in {
    var config = FastPPRConfiguration.defaultConfiguration
    config = config.copy(pprSignificanceThreshold = 1.0e-16f) // Choose tiny significance value to test correctness
    val pprErrorTolerance = 2.0e-6f
    for (line <- Source.fromFile("src/test/resources/test_graph_true_pprs.txt").getLines()) {
      val pieces = line.split("\t")
      val (startId, targetId, truePPR) = (pieces(0).toInt, pieces(1).toInt, pieces(2).toFloat)
      val (inversePPRs, epsilonR) = FastPPR.estimateInversePPRBalanced(graph, targetId, config)
      withClue ("(%d, %d)".format(startId, targetId)) {inversePPRs(startId) should equal (truePPR +- pprErrorTolerance)}
    }
  }

  "FastPPR.estimatePPR" should "be approximately correct on the test graph" in {
    var config = FastPPRConfiguration.defaultConfiguration
    config = config.copy(pprSignificanceThreshold = 0.03f) // smallest true PPR on test graph is 0.03
    val approximationRatio = 1.4f
    for (line <- Source.fromFile("src/test/resources/test_graph_true_pprs.txt").getLines()) {
      val pieces = line.split("\t")
      val (startId, targetId, truePPR) = (pieces(0).toInt, pieces(1).toInt, pieces(2).toFloat)
      for (balanced <- List(false, true)) {
        val estimate = FastPPR.estimatePPR(graph, startId, targetId, config, balanced)
        withClue("(%d, %d)".format(startId, targetId)) {
          assert(estimate > truePPR / approximationRatio)
        }
        withClue("(%d, %d)".format(startId, targetId)) {
          assert(estimate < truePPR * approximationRatio)
        }
      }
    }
  }

}
