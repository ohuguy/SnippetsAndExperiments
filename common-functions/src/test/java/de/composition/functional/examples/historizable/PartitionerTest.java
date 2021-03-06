package de.composition.functional.examples.historizable;

import static de.composition.functional.testing.Times.utcDateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.both;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Stopwatch;

import de.composition.functional.testing.PreCastArgumentMatcher;
import de.composition.functional.testing.Times;

@RunWith(MockitoJUnitRunner.class)
public class PartitionerTest {

	private static final DateTime FROM = Times.utcDateTime("2011-11-22 00:00");
	private static final DateTime UNTIL = Times.utcDateTime("2011-11-23 00:00");

	@Mock
	HistorizableFactProvider factProvider;

	private Partitioner partitioner;

	@Before
	public void setUp() {
		partitioner = new Partitioner(factProvider, FROM, UNTIL);
	}

	@Test
	public void partition_factBoundariesDontOverlapRequestedInterval() throws Exception {
		List<HistorizableFact<?>> facts = FactBuilder.facts().
				aFact("2011-11-22 00:00", "2011-11-22 12:00", "A1").
				aFact("2011-11-22 12:00", "2011-11-23 00:00", "A2").
				bFact("2011-11-22 00:00", "2011-11-22 08:00", "B1").
				bFact("2011-11-22 08:00", "2011-11-22 16:00", "B2").
				bFact("2011-11-22 16:00", "2011-11-23 00:00", "B3").
				getSortedByEndDate();
		factProviderShouldReturn(facts);

		List<TimePartition> partitions = partitioner.partition();

		assertEquals(4, partitions.size());
		assertThat(partitions.get(0), both(
				hasBoundaries("2011-11-22 00:00", "2011-11-22 08:00")).
				and(hasAFact("A1")).
				and(hasBFact("B1")));
		assertThat(partitions.get(1), both(
				hasBoundaries("2011-11-22 08:00", "2011-11-22 12:00")).
				and(hasAFact("A1")).
				and(hasBFact("B2")));
		assertThat(partitions.get(2), both(
				hasBoundaries("2011-11-22 12:00", "2011-11-22 16:00")).
				and(hasAFact("A2")).
				and(hasBFact("B2")));
		assertThat(partitions.get(3), both(
				hasBoundaries("2011-11-22 16:00", "2011-11-23 00:00")).
				and(hasAFact("A2")).
				and(hasBFact("B3")));
	}

	@Test
	public void partition_factBoundariesOverlapRequestedInterval() throws Exception {
		List<HistorizableFact<?>> facts = FactBuilder.facts().
				aFact("2011-11-21 00:00", "2011-11-22 12:00", "A1").
				aFact("2011-11-22 12:00", "2011-11-24 00:00", "A2").
				bFact("2011-11-21 00:00", "2011-11-22 08:00", "B1").
				bFact("2011-11-22 08:00", "2011-11-22 16:00", "B2").
				bFact("2011-11-22 16:00", "2011-11-24 00:00", "B3").
				getSortedByEndDate();
		factProviderShouldReturn(facts);

		List<TimePartition> partitions = partitioner.partition();

		assertEquals(4, partitions.size());
		assertThat(partitions.get(0), both(
				hasBoundaries("2011-11-22 00:00", "2011-11-22 08:00")).
				and(hasAFact("A1")).
				and(hasBFact("B1")));
		assertThat(partitions.get(1), both(
				hasBoundaries("2011-11-22 08:00", "2011-11-22 12:00")).
				and(hasAFact("A1")).
				and(hasBFact("B2")));
		assertThat(partitions.get(2), both(
				hasBoundaries("2011-11-22 12:00", "2011-11-22 16:00")).
				and(hasAFact("A2")).
				and(hasBFact("B2")));
		assertThat(partitions.get(3), both(
				hasBoundaries("2011-11-22 16:00", "2011-11-23 00:00")).
				and(hasAFact("A2")).
				and(hasBFact("B3")));
	}

	@Ignore("might blow the stack size")
	@Test
	public void partition_performance() throws Exception {
		List<HistorizableFact<?>> facts = FactBuilder.facts().
				aFacts("2011-01-01 00:00", "2011-12-21 00:00", 15, "A1").
				bFacts("2011-01-01 00:00", "2011-12-31 00:00", 30, "B1").
				getSortedByEndDate();
		when(
				factProvider.getFactsSortedByEndTime(Times.utcDateTime("2011-01-01 00:00"),
						Times.utcDateTime("2011-12-21 00:00"))).thenReturn(facts);
		partitioner = new Partitioner(factProvider, Times.utcDateTime("2011-01-01 00:00"),
				Times.utcDateTime("2011-12-21 00:00"));
		Stopwatch stopwatch = new Stopwatch().start();
		List<TimePartition> partitions = partitioner.partition();
		System.out.println(stopwatch.elapsedMillis());
	}

	private void factProviderShouldReturn(List<HistorizableFact<?>> facts) {
		when(factProvider.getFactsSortedByEndTime(FROM, UNTIL)).thenReturn(facts);
	}

	private Matcher<TimePartition> hasBoundaries(String start, String end) {
		final DateTime startDateTime = utcDateTime(start);
		final DateTime endDateTime = utcDateTime(end);
		return new PreCastArgumentMatcher<TimePartition>() {

			@Override
			public boolean precastMatches(TimePartition argument) {
				return argument.getStart().isEqual(startDateTime) && argument.getEnd().isEqual(endDateTime);
			}
		};
	}

	private Matcher<TimePartition> hasAFact(final String data) {
		final String uniqueName = AFact.UNIQUE_NAME;
		return hasFact(data, uniqueName);
	}

	private Matcher<TimePartition> hasBFact(final String data) {
		final String uniqueName = BFact.UNIQUE_NAME;
		return hasFact(data, uniqueName);
	}

	private Matcher<TimePartition> hasFact(final String data, final String uniqueName) {
		return new PreCastArgumentMatcher<TimePartition>() {

			@Override
			public boolean precastMatches(TimePartition argument) {
				HistorizableFact<?> fact = argument.getFacts().get(uniqueName);
				return fact != null && fact.getData().equals(data);
			}
		};
	}
}
