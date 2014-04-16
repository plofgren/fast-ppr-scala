import com.twitter.cassovary.graph.DirectedGraph
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

object FastPPR {
  def fastPPRBalanced(graph: DirectedGraph,
              startId: Int,
              targetId: Int,
              config: FastPPRConfiguration): Double = {
    val (frontier, epsilonR) = frontierBalanced(graph, targetId, config)
    -1.0
  }

  /** Compute the frontier to a fixed accuracy additiveBound.
    *
    * @return inversePPRMap
    */
  def frontier(
                graph: DirectedGraph,
                targetId: Int,
                config: FastPPRConfiguration,
                additiveBound: Float): mutable.Map[Int, Float] = {
    val queue = mutable.Queue[Int]()
    queue.enqueue(targetId)

    // inversePPREstimates(uId) estimates ppr(u, target)
    val inversePPREstimates = mutable.HashMap[Int, Float]()
    val priorities = mutable.HashMap[Int, Float]()
    inversePPREstimates(targetId) = config.alpha
    priorities(targetId) =  config.alpha

    val priorityThreshold = additiveBound * config.alpha // priorities about this must be enqueued and pushed

    while (!queue.isEmpty) {
      val vId = queue.dequeue()
      val vPriority = priorities(vId)
      priorities(vId) = 0.0f
      val v = graph.getNodeById(vId).get
      for (uId <- v.inboundNodes()) {
        val u = graph.getNodeById(uId).get
        val deltaPriority = (1.0f - config.alpha) / u.outboundCount * vPriority
        priorities(uId) = priorities.getOrElse(uId, 0.0f) + deltaPriority
        inversePPREstimates(uId) = inversePPREstimates.getOrElse(uId, 0.0f) + deltaPriority
        if (priorities(uId) >= priorityThreshold && priorities(uId) - deltaPriority < priorityThreshold)
          queue.enqueue(uId)

      }
    }

    debias(graph, config, inversePPREstimates, additiveBound / config.beta, additiveBound)

    inversePPREstimates
  }


  /** Compute the frontier to a dynamic accuracy with the goal of balancing forward and reverse work.
    *
    * @param graph
    * @param targetId
    * @param config
    * @return (inversePPRMap, epsilonR)
    */
  def frontierBalanced(graph: DirectedGraph, targetId: Int, config: FastPPRConfiguration): (mutable.Map[Int, Float], Float) = {
    val queue = new HeapMappedPriorityQueue[Int]()
    queue.insert(targetId, config.alpha)

    val inversePPREstimates = mutable.HashMap[Int, Float]() // inversePPREstimates(uId) estimates ppr(u, target)
    inversePPREstimates(targetId) = 1.0f / config.alpha

    var reverseSteps = 0L

    def predictedForwardSteps(maxPriority: Float): Long = {
      val epsilonR = maxPriority / config.alpha / config.beta
      if (epsilonR < config.delta(graph.nodeCount))
        return 0
      else {
        val epsilonF = config.delta(graph.nodeCount) / epsilonR
        (config.nWalks(epsilonF) / config.alpha).toLong
      }
    }

    while( predictedForwardSteps(queue.maxPriority) >= reverseSteps) {
      val vPriority = queue.maxPriority
      val vId = queue.extractMax()
      val v = graph.getNodeById(vId).get
      for (uId <- v.inboundNodes()) {
        val u = graph.getNodeById(uId).get
        val deltaPriority = (1.0f - config.alpha) / u.outboundCount * vPriority
        queue.increasePriority(uId, queue.getPriority(uId) + deltaPriority)
        inversePPREstimates(uId) = inversePPREstimates.getOrElse(uId, 0.0f) + deltaPriority
        reverseSteps += 1
      }
    }
    val additiveBound = queue.maxPriority / config.alpha
    val epsilonR = additiveBound / config.beta

    debias(graph, config, inversePPREstimates, epsilonR, additiveBound)

    (inversePPREstimates, epsilonR)
  }

  /*
    Estimates are within an interval
      estimate <= trueValue <= estimate + additiveBound
    This function heuristically centers the estimates in the Target Set, and propagates those new estimates to the frontier
   */
  def debias(
                graph: DirectedGraph,
                config: FastPPRConfiguration,
                inversePPREstimates: mutable.Map[Int, Float],
                epsilonR: Float,
                additiveBound: Float) {
    for (vId <- inversePPREstimates.keysIterator) {
      if (vId > epsilonR) {
        inversePPREstimates(vId) += additiveBound / 2.0f
        val v = graph.getNodeById(vId).get
        for (uId <- v.inboundNodes()) {
          val u =  graph.getNodeById(uId).get
          inversePPREstimates(uId) +=  (1.0f - config.alpha) / u.outboundCount * additiveBound / 2.0f
        }
      }
    }
  }
}