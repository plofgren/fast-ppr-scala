package soal.fastppr.experiments

import com.twitter.cassovary.graph.{Node, DirectedGraph}
import soal.fastppr.{FastPPR, FastPPRConfiguration}
import scala.collection.mutable
import soal.util.HeapMappedPriorityQueue

object ForwardFrontierExperiment {
  def forwardFrontierSizes(graph: DirectedGraph, maxResidual: Float, nodeCount: Int) {
    val maxOutdegree = 1000
    val teleportProbability = FastPPRConfiguration.defaultConfiguration.teleportProbability
    val nodeIds = (0 until graph.maxNodeId) // Assume ids are sequential

    val startNodeIds = 0 until nodeCount  map { x => ExperimentUtils.randomElement(nodeIds) }
    def frontierSizes = for (s <- startNodeIds) yield {
      val size = computeFrontierSize(graph, s, maxResidual, teleportProbability, maxOutdegree)
      println("node " + s + " has forward frontier size " + size)
      size
    }
    println(frontierSizes.mkString("[", ",", "]"))
  }

  def forwardFrontierSizesVaryingResidual(graph: DirectedGraph, nodeCount: Int) {
    val maxOutdegree = 1000
    val teleportProbability = FastPPRConfiguration.defaultConfiguration.teleportProbability
    val nodeIds = (0 until graph.maxNodeId) // Assume ids are sequential

    val startNodeIds = 0 until nodeCount  map { x => ExperimentUtils.randomElement(nodeIds) }

    val maxResiduals = for (maxResidualExponent <- 3 to 11) yield math.pow(2.0, -maxResidualExponent).toFloat

    val meanSizes = for (maxResidual <- maxResiduals) yield {
      println("\n\n----------maxResidual " + maxResidual + " -------------")
      def frontierSizes = for (s <- startNodeIds) yield {
        val size = computeFrontierSize(graph, s, maxResidual, teleportProbability, maxOutdegree)
        println("node " + s + " has forward frontier size " + size)
        size
      }
      frontierSizes.sum / frontierSizes.size
    }

    println(maxResiduals.mkString("[", ",", "]") + ", " + meanSizes.mkString("[", ",", "]"))
  }


  def computeFrontierSize(graph: DirectedGraph, startId: Int, maxResidual: Float, teleportProbability: Float, maxOutdegree: Int): Int = {
    val pprResiduals = new HeapMappedPriorityQueue[Int]()
    val pprEstimates = mutable.HashMap[Int, Float]().withDefaultValue(0.0f) // inversePPREstimates(uId) estimates ppr(u, target)
    println(" start " + startId)
    pprResiduals.insert(startId, 1.0f)
    while (!pprResiduals.isEmpty && pprResiduals.maxPriority > maxResidual) {
      val vResidual = pprResiduals.maxPriority
      val vId = pprResiduals.extractMax()
      println(" pushing " + vId)
      val v = graph.getNodeById(vId).get
      if (v.outboundCount <= maxOutdegree) {
        for (uId <- v.outboundNodes()) {
          val u = graph.getNodeById(uId).get
          val deltaPriority = (1.0f - teleportProbability) / v.outboundCount * vResidual
          if (! pprResiduals.contains(uId)) {
            pprResiduals.insert(uId, 0.0f)
          }
          pprResiduals.increasePriority(uId, pprResiduals.getPriority(uId) + deltaPriority)
        }
      } else {
        println("Ignoring node " + vId + " with outdegree " + v.outboundCount + " and residual " + vResidual)
      }
    }
    //println("\t" + pprResiduals)
    pprResiduals.size
  }
}
