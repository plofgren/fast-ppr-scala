package soal.fastppr.experiments

import com.twitter.cassovary.graph.DirectedGraph
import java.io.{FileWriter, Writer, OutputStream}
import soal.fastppr.{FastPPR, FastPPRConfiguration}

object CSVExportExperiment {
  val topPRTargetNodeIds = Array(21513299, 23933989, 23933986, 21496201, 23934048, 23937213, 23934131, 23934073, 23934123, 23934033, 23934128, 21523985, 23934127, 21515805, 23934069, 21515803, 21515742, 21515862, 21515771, 21511313, 21515801, 21515941, 21515707, 21515684, 21512477, 21515785, 24891384, 21515782, 21512481, 21515750, 21515628, 21515005, 21515691, 21515109, 21515809, 21515811, 21515261, 21515088, 21515804, 21515770, 21515961, 21512480, 21511314, 21515991, 21515753, 21512633, 21515945, 21510341, 21515802, 21510348, 21515267, 21510346, 21514921, 21514860, 23934091, 21515810, 21515712, 21515953, 21515796, 21515793, 21532286, 21515987, 21515969, 21515917, 21511305, 21515294, 21515844, 23933993, 21515396, 21515108, 21515737, 21515438, 21514907, 21515523, 21515918, 21515096, 21515210, 21512479, 21504113, 21515644, 21514878, 21515812, 20914364, 21515741, 21513161, 21515421, 21515692, 21515997, 21507630, 21515806, 21515668, 21514948, 21515672, 20906321, 21515736, 21515094, 21515682, 21515797, 21515615, 21515641)
  def createCSVFiles(graph: DirectedGraph, csvFilePathPrefix: String, nodeCount: Int) {
    val nodeIds = (0 until graph.maxNodeId)
    val targetNodeIds = topPRTargetNodeIds // 0 until nodeCount map { x => ExperimentUtils.randomElement(nodeIds) }
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
