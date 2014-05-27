package org.necros.webmvc.controllers;

public class ContextBindings {
	public static final String SCRIPT_DONNOT_RENDER = "skipView";
	public static final String SCRIPT_JSON_RESULT = "__json__";
	public static final String SCRIPT_FORWARD_PAGE = "forwardTo";
	
	public static final String[] YES_VALUES = {
		"y", "yes", "true", "t"
	};
	
	public static final String[] NO_VALUES = {
		"n", "no", "false", "f"
	};
	
	public static boolean viewSkipped(Object param) {
		if (param == null) return false;
		if (param instanceof Boolean) return (Boolean)param;
		String sval = param.toString();
		return isYes(sval);
	}

	private static boolean inStrArr(String[] sarr, String sval) {
		String s = sval.toLowerCase();
		for (String y: sarr) {
			if (y.equals(s)) return true;
		}
		return false;
	}

	/**
	 * 从给出的字符串推断是否为“yes”。
	 * @param sval 给定字符串
	 * @return true表示“yes”，false表示不是“yes”；默认是false
	 */
	public static boolean isYes(String sval) {
		if (sval == null) return false;
		return inStrArr(YES_VALUES, sval);
	}
	
	/**
	 * 从给出的字符串推断是否为“no”。
	 * @param sval 给定字符串
	 * @return true表示“no”，false表示不是“no”；默认是false
	 */
	public static boolean isNo(String sval) {
		if (sval == null) return false;
		return inStrArr(NO_VALUES, sval);
	}
}
