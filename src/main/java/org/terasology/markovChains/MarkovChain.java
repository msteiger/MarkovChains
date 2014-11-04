/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.markovChains;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;


/**
 * N-order Markov Chain implementation.
 * This is the user friendly version.
 *
 * If more control over the internal state is necessary, use {@link RawMarkovChain} instead.
 * @param <S> The type of the states.
 *
 * @version 1.01
 * @author Linus van Elswijk
 */
public class MarkovChain<S> extends MarkovChainBase {

    // variables ////////////////////////////////////////////////////////

    private static final String ALL_STATES_UNIQUE_MESSAGE =
            "All objects in the state list should be unique.";

    /**
     * The states of the Markov Chain.
     */
    public final ImmutableList<S> states;

    /**
     * History of states ordered from least recent to most recent.
     * history.first() is the nth previous state, where n={@link #order}
     * history.last() is the current state.
     */
    private final List<S> history;

    /**
     * The state indices for the states in {@link #history},
     * such that for all x: states.get(rawHistory(x)) == history(x)
     */
    private final List<Integer> rawHistory;

    private final Random random;
    private final RawMarkovChain rawMarkovChain;


    // 2nd order Markov Chain constructors //////////////////////////////

    /**
     * Constructs a second order Markov Chain.
     *
     * @param states The states in the chain.
     * @param transitionMatrix A 3d matrix of transition probabilities.
     *                      Every element probabilities[x][y][z] determines the probability of transitioning
     *                      to state z, given current state y and previous state x.
     *                      The matrix must be cubical.
     *
     * @since 1.00
     */
    public MarkovChain(List<S> states, float[][][] transitionMatrix) {
        this(2, states, flatten(transitionMatrix));
    }

    /**
     * Constructs a second order Markov Chain.
     *
     * @param states The states in the chain.
     * @param transitionMatrix A 3d matrix of transition probabilities.
     *                      Every element probabilities[x][y][z] determines the probability of transitioning
     *                      to state z, given current state y and previous state x.
     *                      The matrix must be cubical.
     * @param seed The seed for the random number generator used for determining next states.
     *
     * @since 1.00
     */
    public MarkovChain(List<S> states, float[][][] transitionMatrix, long seed) {
        this(2, states, flatten(transitionMatrix), seed);
    }

    /**
     * Constructs a second order Markov Chain.
     *
     * @param states The states in the chain.
     * @param transitionMatrix A 3d matrix of transition probabilities.
     *                      Every element probabilities[x][y][z] determines the probability of transitioning
     *                      to state z, given current state y and previous state x.
     *                      The matrix must be cubical.
     * @param random The random number generator used for determining next states.
     *
     * @since 1.00
     */
    public MarkovChain(List<S> states, float[][][] transitionMatrix, Random random) {
        this(2, states, flatten(transitionMatrix), random);
    }

    // 1st order Markov Chain constructors //////////////////////////////

    /**
     * Constructs a first order Markov Chain.
     *
     * @param states The states in the chain.
     * @param transitionMatrix A 2d matrix of transition probabilities.
     *                      Every element probabilities[x][y] determines the probability of transitioning
     *                      to state y, given current state x.
     *                      The matrix must be square.
     *
     * @since 1.00
     */
    public MarkovChain(List<S> states, float[][] transitionMatrix) {
        this(1, states, flatten(transitionMatrix));
    }

    /**
     * Constructs a first order Markov Chain.
     *
     * @param states The states in the chain.
     * @param transitionMatrix A 2d matrix of transition probabilities.
     *                      Every element probabilities[x][y] determines the probability of transitioning
     *                      to state y, given current state x.
     *                      The matrix must be square.
     * @param seed The seed for the random number generator used for determining next states.
     *
     * @since 1.00
     */
    public MarkovChain(List<S> states, float[][] transitionMatrix, long seed) {
        this(1, states, flatten(transitionMatrix), seed);
    }

    /**
     * Constructs a first order Markov Chain.
     *
     * @param states The states in the chain.
     * @param transitionMatrix A 2d matrix of transition probabilities.
     *                      Every element probabilities[x][y] determines the probability of transitioning
     *                      to state y, given current state x.
     *                      The matrix must be square.
     * @param random The random number generator used for determining next states.
     *
     * @since 1.00
     */
    public MarkovChain(List<S> states, float[][] transitionMatrix, Random random) {
        this(1, states, flatten(transitionMatrix), random);
    }

    // n-th order Markov Chain constructors //////////////////////////////

    /**
     * Constructs a Markov Chain of any order and any set of states.
     *
     * <b>Note:</b> Avoid calling this constructor directly. Use the more user friendly, 2D and 3D array versions
     * whenever possible.
     *
     * @param order The order (>= 1) of the Markov Chain,
     *      i.e. how many (previous) states are considered to compute the next.
     * @param states The states in the chain.
     * @param transitionProbabilities The transition probabilities of length pow(nrOfStates, order + 1).
     *      The provided array should be a flattened n-dimensional array of probabilities, with
     *      n being the order of the markov chain.
     *
     * @since 1.00
     */
    public MarkovChain(int order, List<S> states, float[] transitionProbabilities) {
        this(order, states, transitionProbabilities, new FastRandom());
    }

    /**
     * Constructs a Markov Chain of any order and any set of states.
     *
     * <b>Note:</b> Avoid calling this constructor directly. Use the more user friendly, 2D and 3D array versions
     * whenever possible.
     *
     * @param order The order (>= 1) of the Markov Chain,
     *      i.e. how many (previous) states are considered to compute the next.
     * @param states The states in the chain.
     * @param transitionProbabilities The transition probabilities of length pow(nrOfStates, order + 1).
     *      The provided array should be a flattened n-dimensional array of probabilities, with
     *      n being the order of the markov chain.
     * @param seed The seed for the random number generator used for determining next states.
     *
     * @since 1.00
     */
    public MarkovChain(int order, List<S> states, float[] transitionProbabilities, long seed) {
        this(order, states, transitionProbabilities, new FastRandom(seed));
    }

    /**
     * Constructs a Markov Chain of any order and any set of states.
     *
     * <b>Note:</b> Avoid calling this constructor directly. Use the more user friendly, 2D and 3D array versions
     * whenever possible.
     *
     * @param order The order (>= 1) of the Markov Chain,
     *      i.e. how many (previous) states are considered to compute the next.
     * @param states The states in the chain.
     * @param transitionProbabilities The transition probabilities of length pow(nrOfStates, order + 1).
     *      The provided array should be a flattened n-dimensional array of probabilities, with
     *      n being the order of the markov chain.
     * @param random The random number generator used for determining next states.
     *
     * @since 1.00
     */
    public MarkovChain(int order, List<S> states, float[] transitionProbabilities, Random random) {
        this(order, states, new RawMarkovChain(order, states.size(), transitionProbabilities), random);
    }

    /**
     * Wraps a MarkovChain interface over a {@link RawMarkovChain}.
     *
     *  <b>Note:</b> In most cases you won't need to construct a {@link RawMarkovChain} before constructing
     *  a {@link MarkovChain}. Use one of the other constructors where possible.
     *
     * @param order The order (>= 1) of the Markov Chain,
     *      i.e. how many (previous) states are considered to compute the next.
     * @param states The states in the chain.
     * @param rawMarkovChain The RawMarkovChain controlling the MarkovChain.
     * @param random The random number generator used for determining next states.
     *
     * @since 1.00
     */
    public MarkovChain(int order, List<S> states, RawMarkovChain rawMarkovChain, Random random) {
        super(order, states.size());

        Preconditions.checkArgument(
                allUnique(states),
                ALL_STATES_UNIQUE_MESSAGE
        );

        this.states = ImmutableList.copyOf(states);
        this.rawMarkovChain = rawMarkovChain;
        this.rawMarkovChain.normalizeProbabilities();
        this.random = random;
        this.history = new LinkedList<>();
        this.rawHistory = new LinkedList<>();

        resetHistory();
    }

    // public interface //////////////////////////////////////////////////

    /**
     * Moves the chain to the next state.
     *
     * @return The next state.
     *
     * @since 1.00
     */
    public S next() {
        history.remove(0);
        rawHistory.remove(0);

        final float randomNumber = random.nextFloat();
        final int rawNext = rawMarkovChain.getNext(randomNumber, rawHistory);

        final S next = states.get(rawNext);

        history.add(next);
        rawHistory.add(rawNext);

        return next;
    }

    /**
     * Returns the current state.
     * Equivalent to previous(0).
     *
     * @return The current state.
     *
     * @since 1.00
     */
    public S current() {
        return history.get(history.size() - 1);
    }

    /**
     * Returns the previous state.
     * Equivalent to previous(1).
     *
     * @return The previous state.
     *
     * @since 1.01
     */
    public S previous() {
        return history.get(history.size() - 2);
    }

    /**
     * Returns the nth previous state.
     * @param n How many states to look back.
     *          Input of 0 returns the current state.
     *          0 <= n < {@link #order}.
     * @return the nth previous state.
     *
     * @since 1.01
     */
    public S previous(final int n) {
        final String illegalNMessage = "Expected 0 <= n <= %s, received n = %s.";
        Preconditions.checkArgument(
                0 <= n && n <= order,
                illegalNMessage, order, n
        );

        return history.get(history.size() - n - 1);
    }

    /**
     * Resets the history.
     *
     * @since 1.00
     */
    public void resetHistory() {
        while (history.size() > 0) {
            history.remove(0);
            rawHistory.remove(0);
        }
        while (history.size() <= order) {
            history.add(states.get(0));
            rawHistory.add(0);
        }
    }

    // private /////////////////////////////////////////////////////////

    private static <S> boolean  allUnique(List<S> objects) {
        Set<S> set = new HashSet<>(objects);
        return set.size() == objects.size();
    }
}
