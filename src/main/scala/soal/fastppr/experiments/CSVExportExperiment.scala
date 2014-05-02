package soal.fastppr.experiments

import com.twitter.cassovary.graph.DirectedGraph
import java.io.{FileWriter, Writer, OutputStream}
import soal.fastppr.{FastPPR, FastPPRConfiguration}

object CSVExportExperiment {

  def createCSVFiles(graph: DirectedGraph, csvFilePathPrefix: String, nodeCount: Int) {
    val nodeIds = (0 until graph.maxNodeId)
    val targetNodeIds = 0 until nodeCount map { x => ExperimentUtils.randomElement(nodeIds) }
    val startNodeIds = 0 until nodeCount  map { x => ExperimentUtils.randomElement(nodeIds) }

    val config = FastPPRConfiguration.defaultConfiguration.copy(pprSignificanceThreshold = 4.0f / graph.nodeCount)
    val frontierWriter = new FileWriter(csvFilePathPrefix + "_frontier.tsv")
    writeCSVFrontiers(graph, targetNodeIds, frontierWriter, config)
    frontierWriter.close()

    val walkProbabilitiesWriter = new FileWriter(csvFilePathPrefix + "_walk_probabilities.tsv")
    writeCSVWalkProbabilities(graph, startNodeIds, walkProbabilitiesWriter, config)
    walkProbabilitiesWriter.close()
  }

  def writeCSVFrontiers(graph: DirectedGraph, targetNodeIds: Iterable[Int], out: Writer, config: FastPPRConfiguration) {
     for (targetId <- targetNodeIds
          if graph.getNodeById(targetId).nonEmpty) {
       println("Computing frontier for target " + targetId)
       val reversePPRSignificanceThreshold = math.sqrt(config.pprSignificanceThreshold).toFloat
       val inversePPREstimates = FastPPR.estimateInversePPR(graph, targetId, config, config.reversePPRApproximationFactor * reversePPRSignificanceThreshold)
       val frontier = FastPPR.computeFrontier(graph, inversePPREstimates, reversePPRSignificanceThreshold)
       for (frontierId <- frontier) {
         out.write(targetId + "\t" + frontierId + "\t" + inversePPREstimates(frontierId) + "\n")
       }
     }
  }

  def writeCSVWalkProbabilities(graph: DirectedGraph, startNodeIds: Iterable[Int], out: Writer, config: FastPPRConfiguration) {
    for (startId <- startNodeIds
         if graph.getNodeById(startId).nonEmpty) {
      println("Computing walk probilities for start " + startId)
      val forwardPPRSignificanceThreshold = math.sqrt(config.pprSignificanceThreshold).toFloat
      val forwardPPREstimates = FastPPR.monteCarloPPR(
        graph,
        startId,
        config.walkCount(forwardPPRSignificanceThreshold),
        config.teleportProbability)
      for ((frontierId, pprEstimate) <- forwardPPREstimates) {
        out.write(startId + "\t" + frontierId + "\t" + pprEstimate + "\n")
      }
    }
  }
}
