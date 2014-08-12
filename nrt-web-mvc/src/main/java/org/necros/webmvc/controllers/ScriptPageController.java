package org.necros.webmvc.controllers;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.necros.webmvc.ContextBindingRequestHandler;
import org.necros.webmvc.util.ResponseUtil;
import org.necros.webmvc.util.SessionUtil;
import org.necros.webmvc.util.SessionKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

@Controller
public class ScriptPageController {
	private static final Logger logger = LoggerFactory.getLogger(ScriptPageController.class);
	@Resource(name="responseUtil")
	private ResponseUtil responseUtil;
	@Resource(name="sessionUtil")
	private SessionUtil sessionUtil;
	@Resource(name="contextBindingRequestHandler")
	private ContextBindingRequestHandler requestHandler;

    @RequestMapping("/**")
    public void html(HttpServletRequest req, HttpServletResponse resp) {
        String path = (String)req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        logger.trace("Processing request to resource: {}", path);
        storeSession(req);
    	try {
			requestHandler.processRequest(path, null, req, resp);
		} catch (IOException e) {
			responseUtil.handleException(req, resp, e, path);
		} catch (Exception e) {
			responseUtil.handleException(req, resp, e, path);
		} finally {
			clearSession();
		}
    }

    private void storeSession(HttpServletRequest req) {
    	HttpSession session = req.getSession(false);
    	if (session == null) return;
    	for (String k: SessionKeys.KEYS) {
    		sessionUtil.set(k, session.getAttribute(k));
    	}
    }

    private void clearSession() {
    	sessionUtil.clear();
    }
}
