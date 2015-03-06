package jReto.meta;

import jReto.util.OrderVerifier;
import jReto.util.RunLoop;

import org.junit.Test;

public class RunLoopTest {
	@Test
	public void testDispatchedRunLoop() {
		final RunLoop runloop = new RunLoop(false);
		final OrderVerifier verifyer = new OrderVerifier();
		
		verifyer.check(1);
		
		runloop.execute(new Runnable() {
			@Override
			public void run() {
				verifyer.check(3);

				runloop.execute(new Runnable() {
					
					@Override
					public void run() {
						verifyer.check(5);
						runloop.stop();
					}
				});
				
				verifyer.check(4);
			}
		});
		
		verifyer.check(2);

		runloop.start();
	}
	
	@Test
	public void testImmediateRunloop() {
		final RunLoop runloop = new RunLoop(true);
		final OrderVerifier verifyer = new OrderVerifier();
		
		verifyer.check(1);
		
		runloop.execute(new Runnable() {
			@Override
			public void run() {
				verifyer.check(2);

				runloop.execute(new Runnable() {
					
					@Override
					public void run() {
						verifyer.check(3);
						runloop.stop();
					}
				});
				
				verifyer.check(4);
			}
		});
		
		verifyer.check(5);

		runloop.start();
	}
}
