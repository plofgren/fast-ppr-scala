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
import org.scalatest.FlatSpec

class MappedPriorityQueueSpec extends FlatSpec {
  "A MappedPriorityQueueSpec" should "store priorities" in {
    val q = new HeapMappedPriorityQueue[String]
    q.insert("one", 1.0f)
    assert(q.getPriority("one") == 1.0f)
  }

  it should "store give default priority 0.0" in {
    val q = new HeapMappedPriorityQueue[String]
    q.insert("one", 1.0f)
    assert(q.getPriority("new") == 0.0f)
  }

  it should "sort items" in {
    val q = new HeapMappedPriorityQueue[String]
    q.insert("two", 2.0f)
    q.insert("one", 1.0f)
    q.insert("negFive", -5.0f)
    q.insert("four", 4.0f)
    q.insert("three", 3.0f)
    assert(q.maxPriority == 4.0f)
    assert(q.extractMax() == "four")
    assert(q.extractMax() == "three")
    assert(q.extractMax() == "two")
    assert(q.extractMax() == "one")
    assert(q.extractMax() == "negFive")
    assert(q.isEmpty)
    //intecept[NoSuchElementException] {
    //  q.extractMax()
    //}
  }
  it should "sort items respecting  increased priorities" in {
    val q = new HeapMappedPriorityQueue[String]
    q.insert("two", 0.5f)
    q.insert("one", 1.0f)
    q.insert("four", 0.04f)
    q.insert("three", 0.3f)
    assert(q.maxPriority == 1.0f)
    q.increasePriority("two", 2.0f)
    q.increasePriority("four", 4.0f)
    q.increasePriority("three", 3.0f)
    assert(q.getPriority("three") == 3.0f)
    assert(q.maxPriority == 4.0f)
    assert(q.extractMax() == "four")
    assert(q.extractMax() == "three")
    assert(q.extractMax() == "two")
    assert(q.extractMax() == "one")
    assert(q.isEmpty)
    //intecept[NoSuchElementException] {
    //  q.extractMax()
    //}
  }
}
