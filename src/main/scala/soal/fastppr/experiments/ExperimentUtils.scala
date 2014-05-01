package soal.fastppr.experiments

import com.twitter.cassovary.graph.{Node, StoredGraphDir, DirectedGraph}
import com.twitter.cassovary.util.io.AdjacencyListGraphReader
import com.twitter.cassovary.graph.StoredGraphDir._
import com.twitter.cassovary.graph.StoredGraphDir.StoredGraphDir
import scala.util.Random
import soal.fastppr.FastPPRConfiguration

object ExperimentUtils {
  def readGraph(graphPath: String): DirectedGraph = {
    val filenameStart = graphPath.lastIndexOf('/') + 1
    val graphDirectory = graphPath.take(filenameStart)
    val graphFilename = graphPath.drop(filenameStart)
    println("graph:" + graphFilename)

    val reader = new AdjacencyListGraphReader(graphDirectory, graphFilename) {
      override def storedGraphDir: StoredGraphDir = StoredGraphDir.BothInOut
    }
    val graph = reader.toArrayBasedDirectedGraph()
    graph
  }

  def randomElement[A](xs: Seq[A]): A = {
    xs(Random.nextInt(xs.size))
  }

  /* pass in Node sequence of graph because graph doesn't seem to have random access to nodes */
  def sampleFromPageRank(graph: DirectedGraph, nodes: Seq[Node], config: FastPPRConfiguration): Node = {
    var currentNode = randomElement(nodes)
    while(Random.nextFloat() > config.teleportProbability) {
      currentNode = currentNode.randomOutboundNode match {
        case Some(uId) => graph.getNodeById(uId).get
        case None  => randomElement(nodes) // Teleport away from sink nodes
      }
    }
    currentNode
  }

  def mean(xs: Seq[Float]): Float = xs.sum / xs.size
}
