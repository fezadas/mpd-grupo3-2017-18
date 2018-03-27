package util;

import util.iterator.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * @author Miguel Gamboa
 *         created on 01-03-2018
 */
public class Queries {

    public static <T> Iterable<T> empty() {
        return () -> new EmptyIterator<>();
    }

    /**
     * Returns a new Iterable whose elements are the specified values.
     */
    public static <T> Iterable<T> of(T...values) {
        return () -> new ArrayIterator(values);
    }

    /**
     * Returns a new Iterable whose elements are supplied by values.
     */
    public static <T> Iterable<T> of(Supplier<T[]> values) {
        return () -> new ArrayIterator(values.get());
    }

    /**
     * Returns a new Iterable consisting of the elements of
     * src Iterable that match the given predicate.
     */
    public static <T> Iterable<T> filter(
            Predicate<T> p,
            Iterable<T> src) {
        return () -> new FilterIterator<>(src, p);
    }

    /**
     * Returns a new Iterable consisting of the results of
     * applying the given function mapper to the elements of
     * the src Iterable.
     */
    public static <T, R> Iterable<R> map(
            Function<T, R> mapper,
            Iterable<T> src) {
        return () -> new MapIterator<>(src, mapper);
    }

    /**
     * Performs a reduction on the elements of the src Iterable,
     * using an associative accumulation function acc, and returns
     * the reduced value.
     */
    public static <T, R> R reduce(
            Iterable<T> src,
            R seed,
            BiFunction<R, T, R> acc) {
        for (T item : src) {
            seed = acc.apply(seed,item);
        }
        return seed;
    }

    /**
     * Performs an action for each element of the src Iterable.
     */
    public static <T> void forEach(
            Iterable<T> src,
            Consumer<T> cons) {
        for (T item : src) {
            cons.accept(item);
        }
    }

    /**
     * Returns the count of elements in the src Iterable.
     */
    public static <T> int count(Iterable<T> src) {
        int n = 0;
        for(T item : src) n++;
        return n;
    }

    /**
     * Returns an infinite sequence where each element is
     * generated by the provided Supplier src.
     * !!!! CANNOT be EAGER => cannot use an auxiliary List !!!!
     */
    public static <T> Iterable<T> generate(Supplier<T> src) {
        return () -> new Generator<>(src);
    }

    /**
     * Returns an infinite sequence produced by iterative application
     * of a function op to an initial element seed, producing a
     * new Iterable consisting of seed, op(seed), op(op(seed)), etc.
     */

    public static <T> Iterable<T> iterate(T seed, UnaryOperator<T> op) {
        return () -> new Iterate<>(seed, op);
    }

    /**
     * <=> Top do SQL
     * Returns a new Iterable consisting of the elements of the
     * src Iterable, truncated to be no longer than maxSize in length.
     */
    public static <T> Iterable<T> limit(Iterable<T> src, int maxSize) {
        return () -> new Limiter<>(src.iterator(), maxSize);
    }

    /**
     * Returns a sequence consisting of the remaining elements of the
     * src Iterable after discarding the first n elements of the sequence.
     */
    public static <T> Iterable<T> skip(Iterable<T> src, int n) {
        return () -> {
            Iterator<T> iter = src.iterator();
            int count = n;
            while(iter.hasNext() && count-- > 0) iter.next();
            return iter;
        };
    }

    /**
     * Returns a new Iterable consisting of the longest prefix of elements
     * taken from teh src Iterable that match the given predicate.
     */
    public static <T> Iterable<T> takeWhile(Predicate<T> p, Iterable<T> src) {
        return ()-> new TakeWhileIterator<T>(src,p);
    }



    /**
     * Returns a new Iterable consisting of the results of replacing each
     * element of the src Iterable with the contents of a mapped Iterable
     * produced by applying the provided mapping function mapper to
     * each element.
     *
     * If path is the path to a file, then the following produces an Iterable
     * of the words contained in that file:
     * <pre>{@code
     *     Iterable<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
     *     Iterable<String> words = Queries.flatMap(lines, line -> Queries.of(line.split(" +")));
     * }</pre>
     */
    public static <T, R> Iterable<R> flatMap(
            Function<T, Iterable<R>> mapper,
            Iterable<T> src) {
        return ()->new FlatMapIterator<T,R>(src,mapper);
    }

    /**
     * Returns an array containing the elements of the src Iterable.
     */
    public static <T> Object[] toArray(Iterable<T> src) {
        List<T> res = new ArrayList<>();
        for (T item : src) {
            res.add(item);
        }
        return res.toArray();
    }
}
