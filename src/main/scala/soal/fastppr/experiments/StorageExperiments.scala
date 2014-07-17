package soal.fastppr.experiments

import com.twitter.cassovary.graph.DirectedGraph
import soal.fastppr.{FastPPR, FastPPRConfiguration}

object StorageExperiments {
  def frontierSizesVaryingResidual(graph: DirectedGraph, nodeCount: Int) {
    val config = FastPPRConfiguration.defaultConfiguration
    val teleportProbability = config.teleportProbability
    val nodeIds = (0 until graph.maxNodeId) // Assume ids are sequential

    val targetNodeIds = 0 until nodeCount  map { x => ExperimentUtils.randomElement(nodeIds) } filter { graph.getNodeById(_).nonEmpty }

    val reverseThresholds = for (i <- 3 to 15) yield math.pow(2.0, -i).toFloat

    val meanSizes = for (reverseThreshold <- reverseThresholds) yield {
      println("\n\n----------reverseThreshold " + reverseThreshold + " -------------")
      def frontierSizes = for (targetId <- targetNodeIds) yield {
        val inversePPREstimates = FastPPR.estimateInversePPR(graph, targetId, config, config.reversePPRApproximationFactor * reverseThreshold)

        val frontier = FastPPR.computeFrontier(graph, inversePPREstimates, reverseThreshold)
        val size = frontier.size
        println("node " + targetId + " has (reverse) frontier size " + size)
        size
      }
      frontierSizes.sum.toFloat / frontierSizes.size
    }

    println(reverseThresholds.mkString("[", ",", "]") + ", " + meanSizes.mkString("[", ",", "]"))
  }


}
