package edu.upenn.cis.tomato.experiment;

public class Compare {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String a = "AHA";
		String b = "AHA";
		Integer i = new Integer(11);
		Integer j = new Integer(3);
		compare(a, j);

	}
	
	public static void compare(Object a, Object b) {
		if (a instanceof Comparable<?> && b instanceof Comparable<?>) {
			Comparable<Object> l = (Comparable<Object>) a;
			Comparable<Object> r = (Comparable<Object>) b;
			System.out.println(l.compareTo(r));;
		}
	}

}
