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

case class FastPPRConfiguration(
                                 teleportProbability: Float,
                                 /**  In forward work, we will do nWalksConstant / forwardPPRSignificanceThreshold walks. */
                                 nWalksConstant : Float,
                                  /** In reverse PPR computation, our additive bound is
                                    * reversePPRApproximationFactor  * reversePPRSignificanceThreshold.
                                    * (beta in paper) */
                                 reversePPRApproximationFactor: Float,
                                 /** PPR values above this threshold will be detected by Fast-PPR.
                                   * (delta in paper)*/
                                 pprSignificanceThreshold: Float,
                                  /** The number of average forward steps (random edge visit) we can do
                                    * in the time it takes to do one average reverse step (priority queue update).
                                    * TODO(some automating tuning option)*/
                                 forwardStepsPerReverseStep: Float
                                 )         {
  def walkCount(forwardPPRSignificanceThreshold: Float): Int = (nWalksConstant / forwardPPRSignificanceThreshold).toInt
}

object FastPPRConfiguration {
  val defaultConfiguration = FastPPRConfiguration(
    teleportProbability=0.2f,
    nWalksConstant=24 *  math.log(1.0e6).toFloat,
    reversePPRApproximationFactor=1.0f  / 6.0f,
    pprSignificanceThreshold=1.0e-6f,
    forwardStepsPerReverseStep=6.7f)
}