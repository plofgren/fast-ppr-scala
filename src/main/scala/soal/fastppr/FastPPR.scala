package soal.fastppr

import com.twitter.cassovary.graph.DirectedGraph
import scala.collection.mutable
import soal.util.HeapMappedPriorityQueue

object FastPPR {
  def fastPPR(graph: DirectedGraph,
              startId: Int,
              targetId: Int,
              config: FastPPRConfiguration,
              balanced: Boolean = true): Float = {

    -1.0f // not implemented yet
  }

  /** Compute the frontier to a fixed additive accuracy pprErrorTolerance.
    *
    * @return inversePPREstimates
    */
  def estimateInversePPR(
                graph: DirectedGraph,
                targetId: Int,
                config: FastPPRConfiguration,
                pprErrorTolerance: Float): mutable.Map[Int, Float] = {
    val largeResidualNodes = mutable.Queue[Int]()
    largeResidualNodes.enqueue(targetId)

    // inversePPREstimates(uId) estimates ppr(u, target)
    val inversePPREstimates = mutable.HashMap[Int, Float]().withDefaultValue(0.0f)
    val inversePPRResiduals = mutable.HashMap[Int, Float]().withDefaultValue(0.0f)
    inversePPREstimates(targetId) = config.teleportProbability
    inversePPRResiduals(targetId) =  config.teleportProbability

    val largeResidualThreshold = pprErrorTolerance * config.teleportProbability // inversePPRResiduals about this must be enqueued and pushed

    while (!largeResidualNodes.isEmpty) {
      val vId = largeResidualNodes.dequeue()
      val vResidual = inversePPRResiduals(vId)
      inversePPRResiduals(vId) = 0.0f
      val v = graph.getNodeById(vId).get
      for (uId <- v.inboundNodes()) {
        val u = graph.getNodeById(uId).get
        val deltaPriority = (1.0f - config.teleportProbability) / u.outboundCount * vResidual
        inversePPRResiduals(uId) += deltaPriority
        inversePPREstimates(uId) += deltaPriority
        if (inversePPRResiduals(uId) >= largeResidualThreshold && inversePPRResiduals(uId) - deltaPriority < largeResidualThreshold)
          largeResidualNodes.enqueue(uId)

      }
    }

    debias(graph, config, inversePPREstimates, pprErrorTolerance / config.reversePPRApproximationFactor, pprErrorTolerance)

    inversePPREstimates
  }


  /** Compute the frontier to a dynamic accuracy with the goal of balancing forward and reverse work.
    * @return (inversePPREstimates, reversePPRSignificanceThreshold)
    */
  def estimateInversePPRBalanced(graph: DirectedGraph, targetId: Int, config: FastPPRConfiguration): (mutable.Map[Int, Float], Float) = {
    val inversePPRResiduals = new HeapMappedPriorityQueue[Int]()
    val inversePPREstimates = mutable.HashMap[Int, Float]().withDefaultValue(0.0f) // inversePPREstimates(uId) estimates ppr(u, target)
    inversePPRResiduals.insert(targetId, config.teleportProbability)
    inversePPREstimates(targetId) = config.teleportProbability

    var reverseSteps = 0L

    def predictedForwardSteps(largestResidual: Float): Long = {
      val reverseThreshold = largestResidual / config.teleportProbability / config.reversePPRApproximationFactor
      if (reverseThreshold < config.pprSignificanceThreshold)
        return 0 // avoid division by 0 if reversePPRThreshold==0.0f
      else {
        val forwardThreshold = config.pprSignificanceThreshold / reverseThreshold
        (config.walkCount(forwardThreshold) / config.teleportProbability).toLong
      }
    }

    while( !inversePPRResiduals.isEmpty && predictedForwardSteps(inversePPRResiduals.maxPriority) >= reverseSteps) {
      val vPriority = inversePPRResiduals.maxPriority
      val vId = inversePPRResiduals.extractMax()
      val v = graph.getNodeById(vId).get
      for (uId <- v.inboundNodes()) {
        val u = graph.getNodeById(uId).get
        val deltaPriority = (1.0f - config.teleportProbability) / u.outboundCount * vPriority
        if (! inversePPRResiduals.contains(uId)) {
          inversePPRResiduals.insert(uId, 0.0f)
        }
        inversePPRResiduals.increasePriority(uId, inversePPRResiduals.getPriority(uId) + deltaPriority)
        inversePPREstimates(uId) = inversePPREstimates.getOrElse(uId, 0.0f) + deltaPriority
        reverseSteps += 1
      }
    }
    val pprErrorTolerance = if(inversePPRResiduals.isEmpty) 0.0f else inversePPRResiduals.maxPriority / config.teleportProbability
    val reversePPRSignificanceThreshold = pprErrorTolerance / config.reversePPRApproximationFactor

    debias(graph, config, inversePPREstimates, reversePPRSignificanceThreshold, pprErrorTolerance)

    (inversePPREstimates, reversePPRSignificanceThreshold)
  }

  /*
    Estimates are within an interval
      estimate <= trueValue <= estimate + pprErrorTolerance
    This function heuristically centers the estimates in the Target Set, and propagates those new estimates to the frontier
   */
  def debias(
                graph: DirectedGraph,
                config: FastPPRConfiguration,
                inversePPREstimates: mutable.Map[Int, Float],
                reversePPRSignificanceThreshold: Float,
                pprErrorTolerance: Float) {
    for (vId <- inversePPREstimates.keysIterator) {
      if (vId > reversePPRSignificanceThreshold) {
        inversePPREstimates(vId) += pprErrorTolerance / 2.0f
        val v = graph.getNodeById(vId).get
        for (uId <- v.inboundNodes()) {
          val u =  graph.getNodeById(uId).get
          inversePPREstimates(uId) +=  (1.0f - config.teleportProbability) / u.outboundCount * pprErrorTolerance / 2.0f
        }
      }
    }
  }
}