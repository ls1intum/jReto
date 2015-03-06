package jReto.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Condition {
	private String name;
	private boolean isConfirmed;
	private boolean verbose;
	private boolean singleConfirmationOnly;
	
	public Condition(String name) {
		this(name, false, true);
	}
	
	public Condition(String name, boolean verbose, boolean singleConfirmationOnly) {
		this.name = name;
		this.isConfirmed = false;
		this.verbose = verbose;
		this.singleConfirmationOnly = singleConfirmationOnly;
	}
	
	public void confirm() {
		if (this.isConfirmed && this.singleConfirmationOnly) throw new IllegalStateException("Trying to confirm a condition that was previously confirmed: "+this.name);
		
		this.isConfirmed = true;
		if (this.verbose) System.out.println("Confirmed: "+this.name);
	}
	
	public boolean isConfirmed() {
		return this.isConfirmed;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void verify() {
		assertTrue("Condition \""+this.name+"\" was not met.", this.isConfirmed);
	}
	
	public static void verifyAll(Condition... conditions) {
		verifyAll(Arrays.asList(conditions));
	}

	public static void verifyAll(Collection<Condition> conditions) {
		ArrayList<String> results = new ArrayList<String>();
		
		for (Condition condition : conditions) {
			if (!condition.isConfirmed()) results.add(condition.getName());
		}
		
		assertTrue("The following conditions were not met: "+results, results.size() == 0);
	}
}
