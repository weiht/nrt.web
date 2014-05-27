package org.necros.webmvc.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.necros.scripting.ScriptRunner;
import org.necros.webmvc.PageRenderer;
import org.necros.webmvc.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

@Controller
public class ScriptPageController {
	private static final Logger logger = LoggerFactory.getLogger(ScriptPageController.class);
	@Resource(name="pageRenderer")
	private PageRenderer pageRenderer;
	@Resource(name="responseUtil")
	private ResponseUtil responseUtil;
	@Resource(name="scriptRunner")
	private ScriptRunner scriptRunner;

    @RequestMapping("/**")
    public void html(HttpServletRequest req, HttpServletResponse resp) {
    	String servletPath = req.getServletPath();
    	String pathInfo = req.getPathInfo();
    	logger.trace("Servlet path: {}, path info: {}, file path: {}", servletPath, pathInfo);
        String path = (String)req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    	try {
			processPath(path, null, req, resp);
		} catch (IOException e) {
			responseUtil.handleException(req, resp, e, path);
		} catch (Exception e) {
			responseUtil.handleException(req, resp, e, path);
		}
    }

	public Map<String, Object> processPath(String path, Map<String, Object> bindings,
			HttpServletRequest req, HttpServletResponse resp)
					throws IOException {
		logger.trace("File path: {}", path);
        Map<String, Object> extraContext = new HashMap<String, Object>();
        if (bindings != null && !bindings.isEmpty()) {
        	extraContext.putAll(bindings);
        }
        extraContext.put("viewPath", path);
        extraContext.put("request", req);
        extraContext.put("response", resp);
        logger.debug("Checking whether a script is bound to path: {}", path);
        Map<String, Object> extra = null;
        try {
			extra = scriptRunner.runScript(path, extraContext);
		} catch (Exception e) {
			throw new IOException(e);
		}
        if (extra != null && !extra.isEmpty()) {
        	Object r = extra.get(ContextBindings.SCRIPT_DONNOT_RENDER);
        	if (ContextBindings.viewSkipped(r)) return extraContext;
        	
        	Object fwd = extra.get(ContextBindings.SCRIPT_FORWARD_PAGE);
        	if (fwd != null) {
        		String p = calcForwardPath(path, (String)fwd);
        		processPath(p, extra, req, resp);
        		return extraContext;
        	}
        	
        	r = extra.get(ContextBindings.SCRIPT_JSON_RESULT);
        	if (r == null) {
                logger.debug("Rendering page: {}", path);
        		pageRenderer.render(path, req, resp, extra);
        	} else {
                logger.debug("Rendering json: {}", path);
        		renderJson(resp, r);
        	}
        } else {
            logger.debug("Rendering page: {}", path);
        	pageRenderer.render(path, req, resp, null);
        }
        return  extraContext;
	}

	private String calcForwardPath(String path, String fwd) {
		if (fwd.startsWith("/")) {
			return fwd;
		}
		//TODO Resolve relative paths.
		int ix = path.lastIndexOf("/");
		if (ix < 1) {
			return fwd;
		} else {
			return path.substring(0, ix + 1) + fwd;
		}
	}

	public void renderJson(HttpServletResponse resp, Object r)
			throws IOException {
		//TODO Write a JSON renderer.
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(resp.getOutputStream(), r);
	}
}
