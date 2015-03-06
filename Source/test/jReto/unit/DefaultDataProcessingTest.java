package jReto.unit;

import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.connectivity.DefaultDataConsumer;
import de.tum.in.www1.jReto.connectivity.DefaultDataSource;

/**
 * Tests for DefaultDataConsumer and DefaultDataSource.
 * */
public class DefaultDataProcessingTest {

	
	@Test (expected = IllegalArgumentException.class)
	public void invalidSourceTest1() {
		DefaultDataSource source = new DefaultDataSource(TestData.generate(0));
		source.getData(0, 1);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void invalidSourceTest2() {
		DefaultDataSource source = new DefaultDataSource(TestData.generate(100));
		source.getData(50, 51);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void invalidConsumerTest1() {
		DefaultDataConsumer consumer = new DefaultDataConsumer(0);
		consumer.consume(TestData.generate(1));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void invalidConsumerTest2() {
		DefaultDataConsumer consumer = new DefaultDataConsumer(100);
		consumer.consume(TestData.generate(50));
		consumer.consume(TestData.generate(51));
	}
	
	@Test
	public void testDataGeneration() {
		int length = 100;
		TestData.verify(TestData.generate(length), length);
	}
	
	void testDataProcessing(int dataLength, int stepSize) {
		DefaultDataSource source = new DefaultDataSource(TestData.generate(dataLength));
		DefaultDataConsumer consumer = new DefaultDataConsumer(dataLength);
		
		for (int i=0; i<dataLength; i+=stepSize) {
			consumer.consume(source.getData(i, Math.min(dataLength-i, stepSize)));
		}
		
		TestData.verify(consumer.getData(), dataLength);
	}
	
	@Test
	public void testSimpleDataProcessing1() {
		testDataProcessing(1, 100);
	}
	
	@Test
	public void testSimpleDataProcessing2() {
		testDataProcessing(2, 1);
	}
	
	@Test
	public void testSimpleDataProcessing3() {
		testDataProcessing(500, 13);
	}
}
