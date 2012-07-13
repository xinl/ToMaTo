package edu.upenn.cis.tomato.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A collection of Suspects that supports filtering and logic operation: union,
 * difference and intersection.
 * 
 * @author Xin Li
 * @version July 11, 2012
 * 
 */
public class SuspectList implements Set<Suspect> {
	/*
	 * Here we use composition instead of inheritance because future feature and
	 * optimization may require we use data structure other than HashSet.
	 */
	Set<Suspect> suspects = new HashSet<Suspect>();

	public SuspectList() {

	}

	/**
	 * Copy constructor
	 * 
	 * @param otherList
	 *            The SuspectList to copy from.
	 */
	public SuspectList(SuspectList otherList) {
		this.suspects = new HashSet<Suspect>(otherList.suspects);
	}

	/**
	 * Remove all Suspect that do not satisfy the given PolicyTerm.
	 * 
	 * @param filter
	 *            The PolicyTerm to used as filtering criterion.
	 */
	public void filter(PolicyTerm filter) {
		for (Suspect s : suspects) {
			if (!filter.appliesTo(s)) {
				remove(s);
			}
		}
	}
	
	@Override
	public String toString() {
		return suspects.toString();
	}

	@Override
	public boolean add(Suspect e) {
		return suspects.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends Suspect> c) {
		return suspects.addAll(c);
	}

	@Override
	public void clear() {
		suspects.clear();
	}

	@Override
	public boolean contains(Object o) {
		return suspects.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return suspects.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return suspects.isEmpty();
	}

	@Override
	public Iterator<Suspect> iterator() {
		return suspects.iterator();
	}

	@Override
	public boolean remove(Object o) {
		return suspects.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return suspects.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return suspects.retainAll(c);
	}

	@Override
	public int size() {
		return suspects.size();
	}

	@Override
	public Object[] toArray() {
		return suspects.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return suspects.toArray(a);
	}

}
