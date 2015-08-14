/*
 * Copyright 2014 Volcano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.volcano.persistent;

/**
 * An extended queue with these functionality:
 *  <p>
 *  <li> enqueue </li>
 *  <li> dequeue </li>
 *  <li> peek </li>
 *  <li> peekAt </li>
 *  <li> insertAt </li>
 *  <li> removeAt </li>
 *  <li> size </li>
 *  <li> isEmpty </li>
 */
public interface XQueue {

    /**
     * Add an item to queue
     * @param item The string item
     */
    void enqueue(String item);

    /**
     * @return The first (eldest) item and remove it from queue. Return {@code null} if
     * the queue is empty
     */
    String dequeue();

    /**
     * @return The first (head) of the queue or {@code null} if the queue is empty.
     * Does not modify the queue
     */
    String peek();

    /**
     * @param position The position to return item at it
     * @return Return the item specified by given position. {@code null} if the queue is empty.
     * Does not modify the queue
     * @throws java.lang.IndexOutOfBoundsException If given position is below 1 and above total
     * number of items in the queue
     */
    String peekAt(int position);

    /**
     * Insert an item at given position
     * @param position The position to insert item
     * @param item The string item
     * @throws java.lang.IndexOutOfBoundsException If given position is below 1 and above total
     * number of items in the queue
     */
    void insertAt(int position, String item);

    /**
     * Remove the item at given position
     * @param position The position to remove item
     * @throws java.lang.IndexOutOfBoundsException If given position is below 1 and above total
     * number of items in the queue
     * @throws java.util.NoSuchElementException If the queue is empty
     */
    void removeAt(int position);

    /**
     * @return The number of items in the queue
     */
    int size();

    /**
     * @return True if the queue is empty, otherwise false
     */
    boolean isEmpty();
}
