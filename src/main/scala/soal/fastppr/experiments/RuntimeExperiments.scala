package soal.fastppr.experiments

import com.twitter.cassovary.graph.{Node, DirectedGraph}
import soal.fastppr.{FastPPR, FastPPRConfiguration}
import scala.collection.mutable
import scala.util.Random

object RuntimeExperiments {

  def measureRuntime(graph: DirectedGraph, trialCount: Int, balanced: Boolean): Seq[Float] = {
    val nodes: Seq[Node] = graph.toSeq
    val threshold = 4.0f / graph.nodeCount
    var config = FastPPRConfiguration.defaultConfiguration
    config = config.copy(pprSignificanceThreshold = threshold)

    val forwardTimes = mutable.ArrayBuffer[Float]()
    val reverseTimes = mutable.ArrayBuffer[Float]()
    for (trialIndex <- 0 until trialCount) {
      val start = ExperimentUtils.randomElement(nodes)
      val target = ExperimentUtils.sampleFromPageRank(graph, nodes, config)
      val (forwardTime, reverseTime) = measureForwardReverseTime(start.id, target.id, graph, config, balanced)
      forwardTimes.append(forwardTime)
      reverseTimes.append(reverseTime)

    }

    val totalTimes = (forwardTimes zip reverseTimes) map { case (x, y) => x + y }
    println("reverseTimes = " + reverseTimes.mkString("[", ", ", "]"))
    println("forwardTimes = " + forwardTimes.mkString("[", ", ", "]"))
    println("totalTimes = " + totalTimes.mkString("[", ", ", "]"))
    println("meanReverseTime = " + ExperimentUtils.mean(reverseTimes))
    println("meanForwardTime = " + ExperimentUtils.mean(forwardTimes))


    totalTimes
  }

  // From http://rosettacode.org/wiki/Time_a_function#Scala
  def time(f: => Unit): Long = {
    val s = System.currentTimeMillis
    f
    System.currentTimeMillis - s
  }

  def measureForwardReverseTime(startId: Int,
                                targetId: Int,
                                graph: DirectedGraph,
                                config: FastPPRConfiguration,
                                balanced: Boolean): (Float, Float) = {
    // This is copied and pasted from FastPPR. TODO: find a way of measure forward+backward time without copying+pasting

    var startTime = System.currentTimeMillis

    val (inversePPREstimates, reversePPRSignificanceThreshold) =
      if (balanced)
        FastPPR.estimateInversePPRBalanced(graph, targetId, config)
      else {
        val reverseThreshold = math.sqrt(config.pprSignificanceThreshold).toFloat
        (FastPPR.estimateInversePPR(graph, targetId, config, config.reversePPRApproximationFactor * reverseThreshold),
          reverseThreshold)
      }
    val frontier = FastPPR.computeFrontier(graph, inversePPREstimates, reversePPRSignificanceThreshold)
    println("frontierSize = " + frontier.size)

    val forwardPPRSignificanceThreshold = config.pprSignificanceThreshold / reversePPRSignificanceThreshold

    val reverseTime = (System.currentTimeMillis - startTime) / 1000.0f
    println("reverseTime = " + reverseTime)

    val startNodeInTargetSet = (inversePPREstimates.getOrElse(startId, 0.0f) >= reversePPRSignificanceThreshold)
    if (startNodeInTargetSet || frontier.contains(startId))
      return (0.0f, reverseTime)


    startTime = System.currentTimeMillis
    val walkCount = config.walkCount(forwardPPRSignificanceThreshold)
    println("walkCount = " + walkCount)
    val pprEstimate = FastPPR.pprToFrontier(graph, startId, forwardPPRSignificanceThreshold, config, frontier, inversePPREstimates)
    println("startNodeOutdegree = " + graph.getNodeById(startId).get.outboundCount)
    val forwardTime = (System.currentTimeMillis - startTime) / 1000.0f
    println("forwardTime = " + forwardTime)

    if (pprEstimate == 1.2345f)
      println("Make sure the compile doesn't optimize pprEstimate away.")
    (forwardTime, reverseTime)
  }
}
