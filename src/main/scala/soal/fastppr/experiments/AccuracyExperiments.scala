package soal.fastppr.experiments

import com.twitter.cassovary.util.io.AdjacencyListGraphReader
import com.twitter.cassovary.graph.StoredGraphDir._
import com.twitter.cassovary.graph.{Node, DirectedGraph, StoredGraphDir}
import com.twitter.cassovary.graph.StoredGraphDir.StoredGraphDir
import scala.collection.mutable
import scala.util.Random
import soal.fastppr.{FastPPRConfiguration, FastPPR}

import grizzled.slf4j.Logging


object AccuracyExperiments extends Logging {

  def measureRelativeError(graph: DirectedGraph, targetCount: Int, startCountPerTarget: Int): Seq[Float] = {
    val nodes: Seq[Node] = graph.toSeq
    val threshold = 4.0f / graph.nodeCount
    var config = FastPPRConfiguration.defaultConfiguration
    config = config.copy(pprSignificanceThreshold = threshold)

    val truePPRs = mutable.ArrayBuffer[Float]()
    val estimatedPPRs = mutable.ArrayBuffer[Float]()
    for (targetIndex <- 0 until targetCount) {
      val target = nodes(Random.nextInt(nodes.size))
      val trueInversePPRs = FastPPR.estimateInversePPR(graph, target.id, config, threshold / 100.0f)
      // We only claim accuracy when truePPR >~ threshold
      val candidateStarts = (trueInversePPRs.keys filter {
        nodeId => threshold / 4.0f <= trueInversePPRs(nodeId) && trueInversePPRs(nodeId) <= threshold * 4
      }).toSeq

      if (candidateStarts.size >= startCountPerTarget) {
        for (startIndex <- 0 until startCountPerTarget) {
          val startId: Int = candidateStarts(Random.nextInt(candidateStarts.size))
          val truePPR = trueInversePPRs(startId)
          val estimatedPPR = FastPPR.estimatePPR(graph, startId, target.id, config, balanced = false)
          truePPRs.append(truePPR)
          estimatedPPRs.append(estimatedPPR)
          printf("%d\t%d\t%g\t%g\n", startId, target.id, estimatedPPR, truePPR)
        }
      } else {
        println("Not enough start nodes for target " + target.id)
      }
    }

    println("estimatedPPRs = " + estimatedPPRs.mkString("[", ",", "]"))
    println("truePPRs = " + truePPRs.mkString("[", ",", "]"))

    val relErrors = (truePPRs zip estimatedPPRs) map {
      case (truePPR, estimatedPPR) => math.abs(truePPR - estimatedPPR) / truePPR
    }

    println("relErrors = " + relErrors)
    println("averageRelError = " + relErrors.sum / relErrors.size)
    println("maxRelError = " + relErrors.max)

    relErrors
  }
}
