case class FastPPRConfiguration(
                                 alpha: Float, // teleport probability
                                 //delta: Float, // Minimum value of pi(s,t) for which Fast-PPR is accurate
                                 //pFail: Float, // The probability that the Monte Carlo walks introduce too much error
                                 nWalksConstant : Float, // In forward work, we will do nWalksConstant / epsilon_f walks
                                 beta: Float, // In reverse PPR computation, our additive bound is beta * epsilon_r
                                 deltaTimesN: Float // delta, the minimum ppr value we reliably detect, is deltaTimesN / (nodeCount)
                                 )         {
  def nWalks(epsilonF: Float): Int = (nWalksConstant / epsilonF).toInt
  def delta(nodeCount: Int): Float = deltaTimesN / nodeCount
}

object FastPPRConfiguration {
  val defaultConfiguration = FastPPRConfiguration(
    alpha=0.2f,
    nWalksConstant=24 *  math.log(1.0e6).toFloat,
    beta=1.0f  / 6.0f,
    deltaTimesN=4.0f)
}