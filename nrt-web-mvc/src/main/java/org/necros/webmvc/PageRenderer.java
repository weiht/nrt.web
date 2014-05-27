package org.necros.webmvc;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PageRenderer {
	public abstract void render(String fullPath,
			HttpServletRequest request, HttpServletResponse response,
			Map<String, Object> extraContext) throws IOException;
}
