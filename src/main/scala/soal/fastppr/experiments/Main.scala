package soal.fastppr.experiments

object Main {
  def main(args: Array[String]) {
    lazy val graph = ExperimentUtils.readGraph(args(1)) // Lazy evaluation to save time if other parameters are invalid
    args.head match {
      case "runtime" => RuntimeExperiments.measureRuntime(graph, 100, false)
      case "runtime_balanced" => RuntimeExperiments.measureRuntime(graph, 100, true)
      case "accuracy" => AccuracyExperiments.measureRelativeError(graph, 10, 100)
      case "ppr_ranking" =>  {
        assert(args.size == 5)
        val idRemappingPath = args(2)
        val startTwitterId = args(3).toInt
        val walkCount = args(4).toInt
        RankingPreliminaryExperiment.topTargetsForUsers(graph, idRemappingPath, startTwitterId, walkCount)
      }
      case "ranking_csv_expand" => {
        assert(args.size == 2)
        val rankingPath = args(1)
        RankingPreliminaryExperiment.csvToExpandedHTML(rankingPath)
      }
      case "csv_experiment" => {
        assert(args.size == 4)
        val csvFilePathPrefix = args(2)
        val nodeCount = args(3).toInt
        CSVExportExperiment.createCSVFiles(graph, csvFilePathPrefix, nodeCount)
      }
      case otherString => println("Unrecognized experiment " + otherString)
    }

  }
}
