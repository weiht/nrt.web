package org.necros.webmvc.velocity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.tools.ToolManager;
import org.necros.res.repo.RepositoryLocator;
import org.necros.util.MimeUtils;
import org.necros.util.ResourceUtils;
import org.necros.util.StringUtils;
import org.necros.webmvc.PageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class VelocityPageRenderer
implements PageRenderer, ApplicationContextAware {
	private static final Logger logger = LoggerFactory.getLogger(VelocityPageRenderer.class);

	private static final String APP_CONTEXT_KEY = "applicationContext";
	public static final String VELOCITOOLS_KEY = "tools";
	public static final String CLASS_PATH_LOADER = "classpath";
	public static final String FILE_LOADER = "file";
	public static final String RES_LOADER_KEY = "resource.loader";
	public static final String LOADER_CLASS_KEY = ".resource.loader.class";
	public static final String FILE_LOADER_NAME = "org.apache.velocity.runtime.resource.loader.FileResourceLoader";
	public static final String CLASS_PATH_LOADER_NAME = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";
	public static final String FILE_LOADER_PATH_KEY = ".resource.loader.path";
	public static final String INCLUDE_HANDLER_KEY = "eventhandler.include.class";
	public static final String INCLUDE_HANDLER_VALUE = "org.apache.velocity.app.event.implement.IncludeRelativePath";
	public static final String REQUEST_KEY = "request";
	public static final String RESPONSE_KEY = "response";
	public static final String PATH_KEY = "fullPath";
	public static final String MAVEN_RESOURCE_POSITION = "/src/main/resources";
	public static final String DEF_MIME_TYPE = "text/html";
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	protected MimeUtils mimeUtil;
	protected String encoding = DEFAULT_ENCODING;
	protected String rootPathInRepo = MAVEN_RESOURCE_POSITION;
	private Properties config = new Properties();
	private VelocityEngine engine;
	private VelocityContext context;
	private ApplicationContext appContext;
	private String configFile;
	private RepositoryLocator locator;

	public void render(String fullPath,
			HttpServletRequest request, HttpServletResponse response,
			Map<String, Object> extraContext) throws IOException {
		ensureEngine();
		try {
			doRender(fullPath, request, response, extraContext);
		} catch (IOException e) {
			throw e;
		} catch (ResourceNotFoundException e) {
			throw new FileNotFoundException(fullPath);
		} catch (VelocityException e) {
			throw e;
		} catch (Exception e) {
			throw new VelocityException(e);
		}
	}

	private void doRender(String fullPath,
			HttpServletRequest request, HttpServletResponse response,
			Map<String, Object> extraContext) throws IOException {
		String t = mimeUtil.getTypeDef(fullPath, DEF_MIME_TYPE);
		logger.debug("Content type of resource: {}", t);
		response.setContentType(t);
		//TODO Binary and text mime types go separate ways.
		VelocityContext ctx = initScope(fullPath, request, response, extraContext);
		try {
			logger.debug("Merging velocity template: {}", fullPath);
			engine.mergeTemplate(fullPath, encoding, ctx, response.getWriter());
		} catch (IOException e) {
			throw e;
		} catch (ResourceNotFoundException e) {
			throw new FileNotFoundException(fullPath);
		} catch (Exception e) {
			throw new VelocityException(e);
		}
	}

	private VelocityContext initScope(String fullPath,
			HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> extraContext) {
		logger.debug("Initializing velocity context for: {}", fullPath);
		VelocityContext ctx;
		if (context == null) {
            if (extraContext == null) {
            	ctx = new VelocityContext();
            } else {
                ctx = new VelocityContext(extraContext);
            }
		} else {
            if (extraContext == null) {
            	ctx = new VelocityContext(context);
            } else {
                ctx = new VelocityContext(extraContext, context);
            }
		}
		ctx.put(PATH_KEY, fullPath);
		ctx.put(REQUEST_KEY, request);
		ctx.put(RESPONSE_KEY, response);
		logger.debug("Velocity context for {} initialized.", fullPath);
		return ctx;
	}

	private synchronized void ensureEngine() {
		if (engine == null) {
			logger.debug("Initializing velocity engine...");
			loadConfig(configFile);
			engine = new VelocityEngine();
			initGlobalContext();
			if (config != null) {
				initRepos(config);
				logger.debug("Velocity engine init config: {}", config);
				engine.init(config);
			}
			logger.debug("Velocity engine initialized.");
		}
	}

	private synchronized void initRepos(Properties props) {
		File[] repos = loadRepos();
		StringBuilder loader = new StringBuilder();
		if (repos != null && repos.length > 0) {
			Arrays.sort(repos);
			for (File r: repos) {
				if (loader.length() > 0) {
					loader.append(',');
				}
				loader.append(locateRepo(r));
			}
		}
		props.put(FILE_LOADER + FILE_LOADER_PATH_KEY, loader.toString());
		props.put(CLASS_PATH_LOADER + LOADER_CLASS_KEY, CLASS_PATH_LOADER_NAME);
		props.put(INCLUDE_HANDLER_KEY, INCLUDE_HANDLER_VALUE);
		props.put(FILE_LOADER + LOADER_CLASS_KEY, FILE_LOADER_NAME);
		props.put(RES_LOADER_KEY, FILE_LOADER + ',' + CLASS_PATH_LOADER);
	}

	private File[] loadRepos() {
		return locator.getRepositories();
	}

	private String locateRepo(File r) {
		if (StringUtils.isEmpty(rootPathInRepo)) return r.getAbsolutePath();
		File rd = new File(r, rootPathInRepo);
		if (rd.exists() && rd.isDirectory()) return rd.getAbsolutePath();
		return r.getAbsolutePath();
	}

	private synchronized void initGlobalContext() {
		if (context == null) {
			ToolManager tmgr = new ToolManager();
			context = new VelocityContext(tmgr.createContext());
			context.put(APP_CONTEXT_KEY, appContext);
		}
	}

	private synchronized void loadConfig(String fn) {
		if (StringUtils.isEmpty(fn)) return;

		Properties props = null;
		InputStream ins = ResourceUtils.loadResource(fn);
		if (ins != null) {
			try {
				props = new Properties();
				props.load(ins);
			} catch (Exception e) {
				//
			} finally {
				ResourceUtils.closeStream(ins);
			}
		}
		
		if (props != null && !props.isEmpty()) {
			config.putAll(props);
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext)
	throws BeansException {
		this.appContext = applicationContext;
	}

	public void setConfig(Properties config) {
		if (config != null && !config.isEmpty()) {
			this.config.putAll(config);
		}
	}

	public void setConfigFile(String fileName) {
		this.configFile = fileName;
	}

	public void setLocator(RepositoryLocator locator) {
		this.locator = locator;
	}

	public void setMimeUtil(MimeUtils mimeUtil) {
		this.mimeUtil = mimeUtil;
	}
}
