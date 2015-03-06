package de.tum.in.www1.jReto.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Collections {
	public static interface Function1<T, R> {
		R f(T item);
	}
	
	public static <T, R> Collection<R> map(Iterable<T> list, Function1<T, R> lambda) {
		ArrayList<R> results = new ArrayList<R>();
		
		for (T item : list) {
			results.add(lambda.f(item));
		}
		
		return results;
	}
	
	public static <T, R> Set<R> map(Set<T> set, Function1<T, R> lambda) {
		HashSet<R> results = new HashSet<R>();
		
		for (T item : set) {
			results.add(lambda.f(item));
		}
		
		return results;
	}
}
