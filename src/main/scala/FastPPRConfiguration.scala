case class FastPPRConfiguration(
                                 //delta: Double, // Minimum value of pi(s,t) for which Fast-PPR is accurate
                                 //pFail: Double, // The probability that the Monte Carlo walks introduce too much error
                                 nWalksConstant : Double, // In forward work, we will do nWalksConstant / epsilon_f walks
                                 beta: Double, // In reverse PPR computation, our additive threshold is beta * epsilon_r
                                 deltaTimesN: Double // delta, the minimum ppr value we reliably detect, is deltaTimesN / (nNodes)
                                 )         {
  def nWalks(epsilonF: Double): Int = (nWalksConstant / epsilonF).toInt
  def delta(nNodes: Int): Double = deltaTimesN / nNodes
}

object FastPPRConfiguration {
  val DefaultConfiguration = FastPPRConfiguration(24 *  math.log(1.0e6) , 1.0  / 6.0, 4.0)
}