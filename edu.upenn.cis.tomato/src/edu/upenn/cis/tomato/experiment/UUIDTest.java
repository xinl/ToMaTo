package edu.upenn.cis.tomato.experiment;

import java.util.UUID;

public class UUIDTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i = 0; i < 5; i++) {
			UUID uuid = UUID.randomUUID();
			String encoded = longToBase64(uuid.getMostSignificantBits()) + longToBase64(uuid.getLeastSignificantBits());
			System.out.println(encoded);
			//System.out.println(65 & 0x3f);
//			System.out.println(uuid.getMostSignificantBits());
//			System.out.println(uuid.getLeastSignificantBits());
//			System.out.println(longToBase64(uuid.getMostSignificantBits()));
//			System.out.println(longToBase64(uuid.getLeastSignificantBits()));
//			System.out.println("");
		}

	}

	static protected String makeBaseObjectName() {
		UUID uuid = UUID.randomUUID();
		return longToBase64(uuid.getMostSignificantBits()) + longToBase64(uuid.getLeastSignificantBits());
	}

	static protected String longToBase64(long num) {
		String base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "$_";
		String encoded = "";
		for (int i = 0; i < 11; i++) {
			int index = (int) (num & 0x3f);
			//System.out.println("I: " + index);
			encoded += base64chars.charAt(index);
			num >>= 6;
		}
		return encoded;
	}
}
