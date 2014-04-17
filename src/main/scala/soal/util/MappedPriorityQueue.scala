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

package soal.util

import java.util.NoSuchElementException
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

/** A max prioity queue with the additional property that it is possible to increase and look-up
  * the priority of elements.
  *
  */
trait MappedPriorityQueue[A] {
  def insert(a: A, priority: Float): Unit
  def contains(a: A): Boolean
  def increasePriority(a: A, newPriority: Float): Unit
  def getPriority(a: A): Float  // Returns 0.0 if a is not in the queue
  def maxPriority: Float
  def extractMax(): A
  def isEmpty: Boolean
}

/** Standard binary heap, based on Chapter 6 of CLRS Algorithms 2nd Ed.
  * */
class HeapMappedPriorityQueue[A] extends MappedPriorityQueue[A] {

  private val priorities = ArrayBuffer[Float](0.0f) //the first entry will be ignored to make arithmetic simpler

  private val itemToIndex = mutable.HashMap[A, Int]()
  private val indexToItem = ArrayBuffer[A](null.asInstanceOf[A]) //the first entry will be ignored to make arithmetic simpler

  private def parent(i: Int) = i / 2
  private def left(i: Int) = i * 2
  private def right(i: Int) = i * 2 + 1

  private def swap(i: Int, j: Int): Unit = {
    val temp = priorities(i)
    priorities(i) = priorities(j)
    priorities(j) = temp

    val itemI = indexToItem(i)
    val itemJ = indexToItem(j)
    itemToIndex(itemI) = j
    itemToIndex(itemJ) = i
    indexToItem(i) = itemJ
    indexToItem(j) = itemI
  }

  /**
  If the max-heap invariant is satisfied except for index i possibly being smaller than a child, restore the invariant.
    */
  private def maxHeapify(i: Int): Unit = {
    var largest = i
    if (left(i) < priorities.size && priorities(left(i)) > priorities(i)) {
      largest = left(i)
    }
    if (right(i) < priorities.size && priorities(right(i)) > priorities(largest)) {
      largest = right(i)
    }
    if (largest != i) {
      swap(i, largest)
      maxHeapify(largest)
    }
  }

  override def insert(a: A, priority: Float): Unit = {
    itemToIndex(a) = indexToItem.size
    indexToItem.append(a)
    priorities.append(Float.NegativeInfinity)
    increasePriority(a, priority)
  }

  override def isEmpty: Boolean = {
    indexToItem.size == 1 // first entry is dummy entry
  }

  override def extractMax(): A = {
    if (isEmpty)
      throw new NoSuchElementException
    val maxItem = indexToItem(1)
    swap(1, priorities.size - 1)
    priorities.remove(priorities.size - 1)
    indexToItem.remove(indexToItem.size - 1)
    itemToIndex.remove(maxItem)

    maxHeapify(1)
    maxItem
  }

  override def maxPriority: Float = {
    if (isEmpty)
      throw new NoSuchElementException
    priorities(1)
  }

  override def getPriority(a: A): Float = {
    itemToIndex.get(a) match {
      case Some(i) => priorities(i)
      case None => 0.0f // Default priority is 0.0
    }
  }

  override def increasePriority(a: A, newPriority: Float): Unit = {
    assert(newPriority >= getPriority(a))
    var i = itemToIndex(a)
    priorities(i) = newPriority
    while (i > 1 && priorities(i) > priorities(parent(i))) {
      swap(i, parent(i))
      i = parent(i)
    }
  }

  override def contains(a: A): Boolean = itemToIndex.contains(a)
}