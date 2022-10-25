package com.artifex.mupdf.viewer;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

public class Pallet {
	private static Pallet instance = new Pallet();
	private final Map<Integer, Object> pallet = new HashMap<>();
	private int sequenceNumber = 0;

	private Pallet() {
	}

	private static Pallet getInstance() {
		return instance;
	}

	public static int sendBundle(Bundle bundle) {
		Pallet instance = getInstance();
		int i = instance.sequenceNumber++;
		if (instance.sequenceNumber < 0)
			instance.sequenceNumber = 0;
		instance.pallet.put(new Integer(i), bundle);
		return i;
	}

	public static Bundle receiveBundle(int number) {
		Bundle bundle = (Bundle) getInstance().pallet.get(new Integer(number));
		if (bundle != null)
			getInstance().pallet.remove(new Integer(number));
		return bundle;
	}

	public static boolean hasBundle(int number) {
		return getInstance().pallet.containsKey(new Integer(number));
	}
}
