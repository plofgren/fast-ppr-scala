package soal.fastppr.experiments

import com.twitter.cassovary.graph.{Node, DirectedGraph}
import scala.io.Source
import scala.collection.mutable
import soal.fastppr.{FastPPRConfiguration, FastPPR}
import scala.util.parsing.json.JSON


object RankingPreliminaryExperiment {
  val maxOutneighborCount = 500
  val ashishTwitterId = 23217821
  val sidTwitterId = 54712503
  val peterTwitterId = 355903117

  def topTargetsForUsers(graph: DirectedGraph, nodeIdToTwitterIdPath: String, startTwitterId: Int, walkCount: Int) {
    val idMapping = new NodeTwitterIdMapping(nodeIdToTwitterIdPath)
    val startId = idMapping.toNodeId(startTwitterId)
    val start = graph.getNodeById(startId).get
    val teleportProbability = FastPPRConfiguration.defaultConfiguration.teleportProbability
    System.err.println("Computing PPR using Monte Carlo")
    val pprEstimates = FastPPR.monteCarloPPR(graph, startId, walkCount, teleportProbability)
    val pprEstimatePairs = pprEstimates.toSeq.filter{_._2 >= 5.0f / walkCount}.sortBy{-_._2}

    System.err.println("Computing intermediate Friend Counts")
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
    for (outNeighborId <- start.outboundNodes(maxOutneighborCount)) {
      val outNeighbor = graph.getNodeById(outNeighborId).get
      for (secondDegreeNeighborId <- outNeighbor.outboundNodes(maxOutneighborCount)) {
        intermediateFriendCounts(secondDegreeNeighborId) += 1
      }
    }
    intermediateFriendCounts
  }

  def csvToExpandedHTML(csvPath: String) {
    println("<table><tr>")
    for (x <- List("pprEstimate", "isFollowed", "numTwoHopPaths", "name", "description", "twitteId")) {
      println("<th>" + x + "</th>")
    }

    val idToJSON = new mutable.HashMap[String, Map[String, Any]]()
    val csvSource = Source.fromFile(csvPath)
    val lines = csvSource.getLines().toSeq
    val twitterLookupIDsPerRequest = 100
    for (lookupGroup <- lines.grouped(twitterLookupIDsPerRequest)) {
      val ids = lookupGroup map {
        _.split("\t")(0)
      } mkString ","
      //println("ids: " + ids)
      val temporaryJSONFile = java.io.File.createTempFile("twitter_api", "json")
      //println("tempFile: " + temporaryJSONFile)
      val apiProcess = Runtime.getRuntime().exec(Array("/home/peter/fast-ppr-scala/lookup_ids.sh", ids, temporaryJSONFile.getAbsolutePath))
      apiProcess.waitFor()
      val jsonSource = Source.fromFile(temporaryJSONFile.getAbsolutePath)
      val jsonString = jsonSource.getLines().mkString
      jsonSource.close()
      //println("jsonString: " + jsonString)
      val parsedJSON = JSON.parseFull(jsonString).get.asInstanceOf[List[Map[String, Any]]]
      for (jsonMap <- parsedJSON) {
        idToJSON(jsonMap("id_str").asInstanceOf[String]) = jsonMap
        System.err.println("json id '" + jsonMap("id_str") + "'")
      }
    }
    for (csvLine <- lines) {
      val csvParts = csvLine.split("\t")
      val twitterId = csvParts(0)
      System.err.println("csv twitter id: '" + twitterId + "'")
      val jsonMap = idToJSON.getOrElse(twitterId, new mutable.HashMap[String, Any]().withDefaultValue("--"))
      val pprEstimate = csvParts(1)
      val isFollowed = csvParts(2)
      val numTwoHopPaths = csvParts(3)
      val name = jsonMap("name")
      val description = jsonMap("description")
      val url = jsonMap("url")
      val screenName = jsonMap("screen_name")
      println("<tr>" )
      for (x <- List(pprEstimate, isFollowed, numTwoHopPaths, "<a href=\"" + url + "\">" + name + "</a>", description, "<a href=\"http://twitter.com/" + screenName + "\">" +  twitterId + "</a>")) {
        println("<td>" + x + "</td>")
      }
      println("</tr>")

    }
    println("</table>")
    csvSource.close()
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