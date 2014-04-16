package soal.fastppr

case class FastPPRConfiguration(
                                 teleportProbability: Float,
                                 /**  In forward work, we will do nWalksConstant / epsilon_f walks. */
                                 nWalksConstant : Float,
                                  /** In reverse PPR computation, our additive bound is reversePPRApproximationFactor  * reversePPRSignificanceThreshold. (beta in paper) */
                                 reversePPRApproximationFactor: Float,
                                 /** PPR values above this threshold will be detected by Fast-PPR (delta in paper)*/
                                 pprSignificanceThreshold: Float
                                 ///** The minimum ppr value we reliably detect is pprSignificanceThresholdTimesN / nodeCount */
                                 //pprSignificanceThresholdTimesN: Float
                                 )         {
  def walkCount(forwardPPRSignificanceThreshold: Float): Int = (nWalksConstant / forwardPPRSignificanceThreshold).toInt
  
  
  //def pprSignificanceThreshold(nodeCount: Int): Float = pprSignificanceThresholdTimesN / nodeCount
}

object FastPPRConfiguration {
  val defaultConfiguration = FastPPRConfiguration(
    teleportProbability=0.2f,
    nWalksConstant=24 *  math.log(1.0e6).toFloat,
    reversePPRApproximationFactor=1.0f  / 6.0f,
    pprSignificanceThreshold=1.0e-6f)
}