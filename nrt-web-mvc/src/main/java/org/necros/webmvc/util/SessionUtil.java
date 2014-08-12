package org.necros.webmvc.util;

import java.util.HashMap;

public class SessionUtil {
	private ThreadLocal<HashMap<String,Object>> internalSession = new ThreadLocal<HashMap<String,Object>>();

	private HashMap<String,Object> getSession() {
		HashMap<String,Object> s = internalSession.get();
		if (s == null) {
			s = new HashMap<String,Object>();
			internalSession.set(s);
		}
		return s;
	}

	public void set(String key, Object value) {
		getSession().put(key, value);
	}

	public Object get(String key) {
		return getSession().get(key);
	}

	public void setCurrentLogin(String loginId) {
		getSession().put(SessionKeys.LOGIN_ID, loginId);
	}

	public String getCurrentLogin() {
		return (String)getSession().get(SessionKeys.LOGIN_ID);
	}

	public void clear() {
		internalSession.remove();
	}
}
