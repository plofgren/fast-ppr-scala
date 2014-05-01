package soal.fastppr.experiments

import com.twitter.cassovary.graph.{Node, DirectedGraph}
import scala.io.Source
import scala.collection.mutable
import soal.fastppr.{FastPPRConfiguration, FastPPR}

object RankingPreliminaryExperiment {
  val ashishTwitterId = 23217821
  def topTargetsForUsers(graph: DirectedGraph, nodeIdToTwitterIdPath: String, startTwitterId: Int =  ashishTwitterId) {
    val idMapping = new NodeTwitterIdMapping(nodeIdToTwitterIdPath)
    val startId = idMapping.toNodeId(startTwitterId)
    val start = graph.getNodeById(startId).get
    val teleportProbability = FastPPRConfiguration.defaultConfiguration.teleportProbability
    val walkCount = 1000 * 1000
    val pprEstimates = FastPPR.monteCarloPPR(graph, startId, walkCount, teleportProbability)
    val pprEstimatePairs = pprEstimates.toSeq.sortBy(-_._2) take 500

    val intermediateFriendCounts = computeIntermediateFriendCounts(graph, start)
    val neighborSet = Set(start.outboundNodes() : _*)

    println("TwitterId\t pprEstimate\tisNeighbor\tintermediateFriendCount")
    for ((nodeId, pprEstimate) <- pprEstimatePairs) {
      val isNeighbor = neighborSet.contains(nodeId)
      println(idMapping.toTwitterId(nodeId) + "\t" + pprEstimate + "\t" + isNeighbor + "\t" + intermediateFriendCounts(nodeId))
    }
  }

  /** Returns a map from nodeId to #{v: (node, v) in E and (v, target) in E}, i.e. the number of two-hop paths from startId to nodeId */
  def computeIntermediateFriendCounts(graph: DirectedGraph, start: Node): mutable.Map[Int, Int] = {
    val intermediateFriendCounts = new mutable.HashMap[Int, Int]().withDefaultValue(0)
    for (outNeighborId <- start.outboundNodes()) {
      val outNeighbor = graph.getNodeById(outNeighborId).get
      for (secondDegreeNeighborId <- outNeighbor.outboundNodes()) {
        intermediateFriendCounts(secondDegreeNeighborId) += 1
      }
    }
    intermediateFriendCounts
  }
}



class NodeTwitterIdMapping(nodeIdToTwitterIdPath: String) {
  val toTwitterId = new mutable.ArrayBuffer[Int]()
  val toNodeId = new mutable.HashMap[Int, Int]()
  for ((twitterIdString, nodeId) <- Source.fromFile(nodeIdToTwitterIdPath).getLines().zipWithIndex) {
    val twitterId = twitterIdString.toInt
    toTwitterId.append(twitterId)
    toNodeId(twitterId) = nodeId
  }
}