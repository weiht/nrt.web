package org.necros.webmvc.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.necros.util.StringUtils;
import org.necros.webmvc.PageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseUtil {
	private static final Logger logger = LoggerFactory.getLogger(ResponseUtil.class);
	
	private PageRenderer pageRenderer;
	private Map<Integer, String> pages;
	
	private String getPage(int code) {
		if (pages == null || pages.isEmpty()) return null;
		return pages.get(code);
	}
	
	public String fullRequestPath(HttpServletRequest request) {
		StringBuilder buff = new StringBuilder();
		StringUtils.appendIfNotEmpty(buff, request.getContextPath());
		StringUtils.appendIfNotEmpty(buff, request.getServletPath());
		StringUtils.appendIfNotEmpty(buff, request.getPathInfo());
		return buff.toString();
	}

	public void renderNotFound(HttpServletRequest request, HttpServletResponse response, String path) {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		// Render 404 page with path.
		if (pageRenderer != null) {
			String page = getPage(HttpServletResponse.SC_NOT_FOUND);
			if (!StringUtils.isEmpty(page)) {
				HashMap<String, Object> bindings = new HashMap<String, Object>();
				bindings.put("requestPath", path);
				try {
					pageRenderer.render(page, request, response, bindings);
				} catch (IOException e) {
					logger.error("Error rendering 404 page. Requesting page: {}", path, e);
				} catch (Exception e) {
					logger.error("Error rendering 404 page. Requesting page: {}", path, e);
				}
			}
		}
	}
	
	public void renderNotAuthorized(HttpServletRequest request, HttpServletResponse response, String path, String uid) {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		if (pageRenderer != null) {
			String page = getPage(HttpServletResponse.SC_UNAUTHORIZED);
			if (!StringUtils.isEmpty(page)) {
				HashMap<String, Object> bindings = new HashMap<String, Object>();
				bindings.put("requestPath", path);
				bindings.put("currentUser", uid);
				try {
					pageRenderer.render(page, request, response, bindings);
				} catch (IOException e) {
					logger.error("Error rendering 401 page. Requesting page: {}, uid: {}", path, uid, e);
				} catch (Exception e) {
					logger.error("Error rendering 401 page. Requesting page: {}, uid: {}", path, uid, e);
				}
			}
		}
	}

	public void renderInternalError(HttpServletRequest request, HttpServletResponse response, Throwable t) {
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		// Render 500 page.
		if (pageRenderer != null) {
			String page = getPage(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			if (!StringUtils.isEmpty(page)) {
				HashMap<String, Object> bindings = new HashMap<String, Object>();
				bindings.put("exception", t);
				try {
					pageRenderer.render(page, request, response, bindings);
				} catch (IOException e) {
					logger.error("Error rendering 500 page.", e);
				} catch (Exception e) {
					logger.error("Error rendering 500 page.", e);
				}
			}
		}
	}
	
	public void handleException(HttpServletRequest request, HttpServletResponse response, Throwable t, String path) {
		//TODO Handle more exceptions.
		logger.info("", t);
		if (t instanceof FileNotFoundException) {
			renderNotFound(request, response, path);
		} else {
			renderInternalError(request, response, t);
		}
	}
	
	public void setPageRenderer(PageRenderer pageRenderer) {
		this.pageRenderer = pageRenderer;
	}

	public void setPages(Map<Integer, String> pages) {
		this.pages = pages;
	}
}
