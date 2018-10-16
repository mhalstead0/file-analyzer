package com.matthalstead.fileanalyzer.cmep.consumers;

import java.util.HashSet;
import java.util.Set;

public class DistinctObjectCounterWithCondense<T> {
	
	private int lastCondenseCount = 0;
	private Set<T> set = null;
	
	private final Object LOCK = new Object();

	public void addObject(T t) {
		synchronized(LOCK) {
			Set<T> set = this.set;
			if (set == null) {
				set = new HashSet<T>();
				this.set = set;
			}
			set.add(t);
		}
	}
	
	public Set<T> getObjects() {
		synchronized(LOCK) {
			return (set == null) ? null : new HashSet<T>(set);
		}
	}
	
	public void condense() {
		synchronized(LOCK) {
			Set<T> set = this.set;
			if (set == null) {
				lastCondenseCount = 0;
			} else {
				lastCondenseCount = set.size();
			}
			set = null;
		}
	}
	
	public void reset() {
		synchronized(LOCK) {
			lastCondenseCount = 0;
			set = null;
		}
	}
	
	public int getCount() {
		synchronized(LOCK) {
			Set<T> set = this.set;
			if (set == null) {
				return lastCondenseCount;
			} else {
				return set.size();
			}
		}
	}
}
