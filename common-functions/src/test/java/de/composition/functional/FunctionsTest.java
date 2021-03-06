package de.composition.functional;

import static com.google.common.base.Functions.compose;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static de.composition.functional.Comparison.invert;
import static de.composition.functional.Functions.curriedMap;
import static de.composition.functional.Functions.foldLeft;
import static de.composition.functional.examples.ExampleFunctions.add;
import static de.composition.functional.examples.ExampleFunctions.average;
import static de.composition.functional.examples.ExampleFunctions.count;
import static de.composition.functional.examples.ExampleFunctions.insertAsFirstElem;
import static de.composition.functional.examples.ExampleFunctions.mult;
import static de.composition.functional.series.SlidingWindows.idealWindowFunction;
import static de.composition.functional.testing.EquivalenceMatchers.matchesEquivalently;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import de.composition.functional.series.SlidingWindows;
import de.composition.functional.series.SlidingWindows.Window;

public class FunctionsTest {

	protected static final int SIZE = 0;

	@Test
	public void foldLeft_count() throws Exception {
		assertEquals(Integer.valueOf(6), foldLeft(newArrayList(1, 2, 3, 4, 5, 6), count(), 0));
	}

	@Test
	public void foldLeft_composedFunctions() throws Exception {
		assertEquals(newArrayList(6, 4, 2),
				foldLeft(newArrayList(1, 2, 3), compose(insertAsFirstElem(), mult(2)), emptyList()));
	}

	@Test
	public void foldLeft_composedAbstractFunctions() throws Exception {
		AbstractFunction<Integer, Integer> mult2 = AbstractFunction.from(mult(2));
		assertEquals(newArrayList(6, 4, 2),
				foldLeft(newArrayList(1, 2, 3), mult2.andThen(insertAsFirstElem()), emptyList()));

		AbstractFunction<Integer, Function<List<Integer>, List<Integer>>> insertAsFirstElem = AbstractFunction
				.from(insertAsFirstElem());
		assertEquals(newArrayList(6, 4, 2),
				foldLeft(newArrayList(1, 2, 3), insertAsFirstElem.compose(mult(2)), emptyList()));
	}

	@Test
	public void foldLeft_sum() throws Exception {
		assertEquals(Integer.valueOf(21), foldLeft(newArrayList(1, 2, 3, 4, 5, 6), add(), 0));
	}

	@Test
	public void sequence_functionsAppliedSequentially() throws Exception {
		@SuppressWarnings("unchecked")
		Function<Integer, List<Integer>> sequence = Functions.sequence(mult(2), mult(3), mult(4));
		assertEquals(newArrayList(4, 6, 8), sequence.apply(2));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void movingAverage() throws Exception {
		List<Window<Integer>> initialWindowsList = newArrayList(new Window<Integer>(newArrayList(0, 0)));
		List<Window<Integer>> windows = foldLeft(newArrayList(1, 2, 4, 1, 5), SlidingWindows.<Integer> windows(3),
				initialWindowsList);
		List<Double> avgs = transform(windows, compose(average(), SlidingWindows.<Integer> referenceValues()));
		assertThat(avgs, matchesEquivalently(newArrayList(0.33, 1.0, 2.33, 2.33, 3.33), rounded(0.01)));
	}

	private Equivalence<Double> rounded(final double delta) {
		return new Equivalence<Double>() {

			@Override
			protected boolean doEquivalent(Double a, Double b) {
				return Math.abs(a - b) <= delta;
			}

			@Override
			protected int doHash(Double t) {
				return 0;
			}

		};
	}

	@Test
	public void findSmallestWindow() throws Exception {
		int maxSummand = Integer.MAX_VALUE / 3;
		Window<Integer> initial = new Window<Integer>(newArrayList(maxSummand, maxSummand, maxSummand));
		Window<Integer> smallest = foldLeft(newArrayList(1, 2, 4, 1, 3, 1, 1, 2, 3, 1, 1, 1, 4),
				idealWindowFunction(3, sumCompare()), initial);

		assertEquals(newArrayList(1, 1, 1), smallest.getReferenceWindow());
	}

	@Test
	public void findBiggestWindow() throws Exception {
		int minSummand = Integer.MIN_VALUE / 3;
		Window<Integer> initial = new Window<Integer>(newArrayList(minSummand, minSummand, minSummand));
		Window<Integer> biggest = foldLeft(newArrayList(1, 2, 4, 1, 3, 1, 1, 2, 3, 1, 1, 1, 4),
				idealWindowFunction(3, invert(sumCompare())), initial);

		assertEquals(newArrayList(4, 1, 3), biggest.getReferenceWindow());
	}

	private Function<List<Integer>, Comparable<List<Integer>>> sumCompare() {
		return new Function<List<Integer>, Comparable<List<Integer>>>() {

			public Comparable<List<Integer>> apply(final List<Integer> first) {
				return new Comparable<List<Integer>>() {

					public int compareTo(List<Integer> second) {
						Integer firstSum = Functions.foldLeft(first, add(), 0);
						Integer secondSum = Functions.foldLeft(second, add(), 0);
						return firstSum.compareTo(secondSum);
					}
				};
			}
		};
	}

	@Test
	public void map() throws Exception {
		assertEquals(newArrayList(2, 4, 6, 8, 10, 12), Functions.map(newArrayList(1, 2, 3, 4, 5, 6), mult(2)));
	}

	@Test
	public void weaveIn_onAbstractFunction2() throws Exception {
		Function<Integer, Integer> fct = add().weaveIn(mult(2));
		assertEquals(6, fct.apply(2).intValue());
	}

	@Test
	public void windows() throws Exception {
		ArrayList<Integer> list = newArrayList(1, 2, 3, 4, 5, 6, 7, 8);

		List<List<Integer>> empty = newArrayList();
		List<List<Integer>> result = foldLeft(list, win(3), empty);
		System.out.println(result);
	}

	@Test
	public void curriedMap_fixesAFunctionToTheMapOperator() throws Exception {
		Function<Iterable<Integer>, Iterable<Integer>> add2Mapping = curriedMap(add(2));
		assertEquals(newArrayList(2, 3, 4, 5), newArrayList(add2Mapping.apply(newArrayList(0, 1, 2, 3))));
	}

	@Test
	public void curriedMap_whichReturnsAnArrayList() throws Exception {
		Function<Iterable<Integer>, List<Integer>> add2Mapping = curriedMap(add(2), IterablesFunctions.<Integer>asArrayList());
		assertEquals(newArrayList(2, 3, 4, 5), add2Mapping.apply(newArrayList(0, 1, 2, 3)));
	}

	private Function<Integer, Function<List<List<Integer>>, List<List<Integer>>>> win(final int length) {
		return new AbstractFunction2<Integer, List<List<Integer>>, List<List<Integer>>>() {

			public List<List<Integer>> apply(Integer a, List<List<Integer>> b) {
				b.add(newArrayList(a));
				for (int i = b.size() - 2; i >= (b.size() - length) && i >= 0; i--) {
					b.get(i).add(a);
				}
				return b;
			}
		};
	}

	private ArrayList<Integer> emptyList() {
		return Lists.<Integer> newArrayList();
	}

}
