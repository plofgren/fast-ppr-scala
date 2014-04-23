/*
Copyright 2014 Stanford Social Algorithms Lab

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
  limitations under the License.
*/

package soal.fastppr

import com.twitter.cassovary.graph.DirectedGraph
import scala.collection.mutable
import soal.util.HeapMappedPriorityQueue
import scala.util.Random
import grizzled.slf4j.Logging

object FastPPR {

  /** Returns an estimate of ppr(start, target).  Its accuracy depends on the parameters in config.  If balanced is true,
    * it attempts to balanced forward and backward work to decrease running time without significantly changing the accuracy.
   */
  def estimatePPR(graph: DirectedGraph,
              startId: Int,
              targetId: Int,
              config: FastPPRConfiguration,
              balanced: Boolean = true): Float = {
    val (inversePPREstimates, reversePPRSignificanceThreshold) =
      if (balanced)
        estimateInversePPRBalanced(graph, targetId, config)
      else {
        val reverseThreshold = math.sqrt(config.pprSignificanceThreshold).toFloat
        (estimateInversePPR(graph, targetId, config, config.reversePPRApproximationFactor * reverseThreshold),
          reverseThreshold)
      }
    val frontier = computeFrontier(graph, inversePPREstimates, reversePPRSignificanceThreshold)

    val forwardPPRSignificanceThreshold = config.pprSignificanceThreshold / reversePPRSignificanceThreshold

    val startNodeInTargetSet = (inversePPREstimates.getOrElse(startId, 0.0f) >= reversePPRSignificanceThreshold)
    if (startNodeInTargetSet || frontier.contains(startId))
      return inversePPREstimates(startId)
    
    val pprEstimate = pprToFrontier(graph, startId, forwardPPRSignificanceThreshold, config, frontier, inversePPREstimates)

    pprEstimate
  }

  /**
   * Returns an estimate of the PPR from start to the frontier, using weights in inversePPREstimates.
   */
  private[fastppr] def pprToFrontier(graph: DirectedGraph,
                    startId: Int,
                    forwardPPRSignificanceThreshold: Float,
                    config: FastPPRConfiguration,
                    frontier: mutable.Set[Int],
                    inversePPREstimates: mutable.Map[Int, Float]): Float = {
    val walkCount = config.walkCount(forwardPPRSignificanceThreshold)
    var estimate = 0.0
    for (walkIndex <- 0 until walkCount) {
      var currentNode = graph.getNodeById(startId).get
      while (Random.nextFloat() > config.teleportProbability &&
             currentNode.outboundCount > 0 &&
             !frontier.contains(currentNode.id)) {
        currentNode = graph.getNodeById(currentNode.randomOutboundNode.get).get
      }
      if (frontier.contains(currentNode.id)) {
        estimate += 1.0 / walkCount * inversePPREstimates(currentNode.id)
      }
      
    }
    estimate.toFloat
  }

  /** Returns a map from nodeId to ppr(node, target) up to a fixed additive accuracy pprErrorTolerance. */
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


  /** Computes inversePPR to the target up to a dynamic accuracy with the goal of balancing forward and reverse work.
    * @return (inversePPREstimates, reversePPRSignificanceThreshold)
    */
  def estimateInversePPRBalanced(
                                  graph: DirectedGraph,
                                  targetId: Int,
                                  config: FastPPRConfiguration): (mutable.Map[Int, Float], Float) = {
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
    val pprErrorTolerance =
      if(inversePPRResiduals.isEmpty)
        0.0f
      else
        inversePPRResiduals.maxPriority / config.teleportProbability
    val reversePPRSignificanceThreshold = pprErrorTolerance / config.reversePPRApproximationFactor

    debias(graph, config, inversePPREstimates, reversePPRSignificanceThreshold, pprErrorTolerance)

    (inversePPREstimates, reversePPRSignificanceThreshold)
  }

  /**
    * Modifies inversePPREstimates to remove the negative bias.
    * Given estimates are within an interval
    *   estimate <= trueValue <= estimate + pprErrorTolerance
    * This function heuristically centers the estimates in the target set, and propagates those new estimates to the frontier.
   */

  def debias(
              graph: DirectedGraph,
              config: FastPPRConfiguration,
              inversePPREstimates: mutable.Map[Int, Float],
              reversePPRSignificanceThreshold: Float,
              pprErrorTolerance: Float) {
    for (vId <- inversePPREstimates.keysIterator
         if inversePPREstimates(vId) > reversePPRSignificanceThreshold) {
      inversePPREstimates(vId) += pprErrorTolerance / 2.0f
      val v = graph.getNodeById(vId).get
      for (uId <- v.inboundNodes()) {
        val u = graph.getNodeById(uId).get
        inversePPREstimates(uId) += (1.0f - config.teleportProbability) / u.outboundCount * pprErrorTolerance / 2.0f
      }
    }
  }


  /** Returns the set of nodes with some out-neighbor in the target set (those nodes v with
    * ppr(v, target) > reversePPRSignificanceThreshold)
    */

  def computeFrontier(
                      graph: DirectedGraph,
                      inversePPREstimates: mutable.Map[Int, Float],
                      reversePPRSignificanceThreshold: Float): mutable.Set[Int] = {
    val frontier = new mutable.HashSet[Int]()
    for (vId <- inversePPREstimates.keysIterator) {
      val vInTargetSet = (inversePPREstimates(vId) >= reversePPRSignificanceThreshold)
      if (vInTargetSet) {
        val v = graph.getNodeById(vId).get
        for (uId <- v.inboundNodes()) {
          frontier.add(uId)
        }
      }
    }
    frontier
  }
}