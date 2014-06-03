package org.necros.webmvc;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ContextBindingRequestHandler {
	public Map<String, Object> processRequest(String path, Map<String, Object> bindings,
			HttpServletRequest req, HttpServletResponse resp) throws IOException;
}
