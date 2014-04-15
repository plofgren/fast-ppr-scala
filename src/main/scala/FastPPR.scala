import com.twitter.cassovary.graph.DirectedGraph

object FastPPR {
  def fastPPR(graph: DirectedGraph,
              startId: Int,
              targetId: Int,
              config: FastPPRConfiguration): Double = {
    val (frontier, epsilonF) = balancedFrontier(graph, targetId, config)
    -1.0
  }

  def balancedFrontier(graph: DirectedGraph, targetId: Int, config: FastPPRConfiguration): (Map[Int, Float], Double) = {
    null
  }
}