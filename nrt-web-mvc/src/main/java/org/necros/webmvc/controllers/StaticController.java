package org.necros.webmvc.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.necros.res.repo.RepositoryLocator;
import org.necros.util.ResourceUtils;
import org.necros.webmvc.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class StaticController {
	private static final Logger logger = LoggerFactory.getLogger(StaticController.class);
	
	@Resource(name="responseUtil")
	private ResponseUtil responseUtil;
	@Resource(name="repositoryLocator")
	private RepositoryLocator locator;
	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
	
	@RequestMapping("/**")
	public void resource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    	String servletPath = req.getServletPath();
    	String pathInfo = req.getPathInfo();
        String path = servletPath == null ? "" : servletPath;
        path += pathInfo == null ? "" : pathInfo;
        logger.trace("Processing request to static resource: {}", path);
        logger.trace("Finding {} in file system repositories...", path);
		for (File repo: locator.getRepositories()) {
			File res = new File(repo, path);
			logger.trace("Does file exist? {}", res);
			if (res.exists() && res.isFile()) {
				logger.trace("File system resource found: {}", res);
				renderFile(res, resp);
				return;
			}
		}
        logger.trace("Finding {} in classpath...", path);
		org.springframework.core.io.Resource res = resolver.getResource("classpath:" + path);
		if (res != null && res.exists() && res.isReadable()) {
			logger.trace("Classpath resource found: {}", res);
			renderResource(res, resp);
			return;
		}
		logger.warn("Resource not found: {}", path);
		responseUtil.renderNotFound(req, resp, path);
	}

	private void renderFile(File res, HttpServletResponse resp) throws IOException {
		FileInputStream ins = new FileInputStream(res);
		try {
			ResourceUtils.transfer(ins, resp.getOutputStream());
		} finally {
			ResourceUtils.closeStream(ins);
		}
	}

	private void renderResource(org.springframework.core.io.Resource res, HttpServletResponse resp) throws IOException {
		InputStream ins = res.getInputStream();
		try {
			ResourceUtils.transfer(ins, resp.getOutputStream());
		} finally {
			ResourceUtils.closeStream(ins);
		}
	}
}
