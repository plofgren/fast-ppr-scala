package soal.fastppr.experiments

object Main {
  def main(args: Array[String]) {
    val graph = ExperimentUtils.readGraph(args.drop(1))
    args.head match {
      case "runtime" => RuntimeExperiments.measureRuntime(graph, 100, false)
      case "runtime_balanced" => RuntimeExperiments.measureRuntime(graph, 100, false)
      case "accuracy" => AccuracyExperiments.measureRelativeError(graph, 10, 100)
      case otherString => println("Unrecognized experiment " + otherString)
    }

  }
}
