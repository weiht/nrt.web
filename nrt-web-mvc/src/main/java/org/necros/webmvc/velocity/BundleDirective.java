package org.necros.webmvc.velocity;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.necros.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleDirective
extends Directive {
	private static final Logger logger = LoggerFactory.getLogger(BundleDirective.class);
	
	public static final String BUNDLE_KEY = "bundle";

	@Override
	public String getName() {
		return "bundle";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter ctx, Writer w, Node n)
			throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		String bundleName = getBundleName(ctx, n);
		if (StringUtils.isEmpty(bundleName)) {
			logger.trace("No bundle name specified.");
			return false;
		}
		Locale lc = getLocaleName(ctx);
		logger.trace("Locale for resource bundle {}: {}", bundleName, lc);
		String repos = getRepos(ctx);
		logger.trace("Repos for resources: {}", repos);
		try {
			loadBundle(ctx, bundleName, lc, repos);
			return true;
		} catch (MissingResourceException e) {
			logger.warn("", e);
			return false;
		}
	}

	private void loadBundle(InternalContextAdapter ctx, String bundleName,
			Locale lc, String repos) {
		if (StringUtils.isEmpty(repos)) {
			loadClasspathBundles(ctx, bundleName, lc);
		} else {
			loadMixedBundles(ctx, bundleName, lc, repos);
		}
	}

	private void loadClasspathBundles(InternalContextAdapter ctx,
			String bundleName, Locale lc) {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName, lc);
		ctx.put(BUNDLE_KEY, bundle);
	}

	private void loadMixedBundles(InternalContextAdapter ctx,
			String bundleName, Locale lc, String repos) {
		URL[] urls = reposToUrls(repos);
		ClassLoader cloader = new URLClassLoader(urls, getClass().getClassLoader());
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName, lc, cloader);
		ctx.put(BUNDLE_KEY, bundle);
	}

	private URL[] reposToUrls(String repos) {
		String[] repoarr = repos.split(",");
		URL[] result = new URL[repoarr.length];
		for (int i = 0; i < result.length; i ++) {
			try {
				result[i] = new File(repoarr[i]).toURI().toURL();
			} catch (MalformedURLException e) {
				//Does nothing
			}
		}
		return result;
	}

	private Locale getLocaleName(InternalContextAdapter ctx) {
		HttpServletRequest request = (HttpServletRequest) ctx.get(VelocityPageRenderer.REQUEST_KEY);
		if (request == null) return Locale.getDefault();
		return request.getLocale();
	}

	private String getRepos(InternalContextAdapter ctx) {
		return (String) ctx.get(VelocityPageRenderer.REPO_DIRS_KEY);
	}

	private String getBundleName(InternalContextAdapter ctx, Node n) {
		//TODO Retrieve bundle to current template.
		if (n.jjtGetNumChildren() < 1) {
			// 获取默认资源
			String bundleName = pathToBundleName((String) ctx.get(VelocityPageRenderer.PATH_KEY));
			logger.trace("No bundle name specified in directive. Using full path: {}", bundleName);
			return bundleName;
		} else {
			SimpleNode nbundle = (SimpleNode) n.jjtGetChild(0);
			String bundleName = (String) nbundle.value(ctx);
			logger.trace("Bundle name specified in directive: {}", bundleName);
			return bundleName;
		}
	}

	private String pathToBundleName(String bundleName) {
		//最后一段路径中不允许出现点

		int ix = bundleName.lastIndexOf('/');
		if (ix >= 0) {
			ix = bundleName.indexOf('.', ix);
		}
		String result = ix > 0 ? bundleName.substring(0, ix) : bundleName;
		logger.trace("Translated bundle name: {}", result);
		if (result.charAt(0) == '/') return result.substring(1);
		return result;
	}
}
