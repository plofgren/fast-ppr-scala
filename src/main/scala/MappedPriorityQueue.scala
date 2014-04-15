import scala.collection.mutable.ArrayBuffer

/** A max prioity queue with the additional property that it is possible to increase the priority of elements
  *
  */
trait MappedPriorityQueue[A] {
  def maxPriority: Float
  def dequeue(): A
  def increasePriority(a: A, change: Float): Unit
  def getPriority(a: A): Float
  def isEmpty: Boolean
}

class HeapMappedPriorityQueue[A] {
  val heap = new ArrayBuffer[Float]()
}