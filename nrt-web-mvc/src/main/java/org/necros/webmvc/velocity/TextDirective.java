package org.necros.webmvc.velocity;

import java.io.IOException;
import java.io.Writer;
import java.util.ResourceBundle;

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

public class TextDirective
extends Directive {
	private static final Logger logger = LoggerFactory.getLogger(TextDirective.class);

	@Override
	public String getName() {
		return "text";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter ctx, Writer w, Node n)
			throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		String key = getKey(ctx, n);
		if (StringUtils.isEmpty(key)) {
			logger.trace("No key for resource text specified.");
			return false;
		}
		ResourceBundle bundle = getBundle(ctx);
		if (bundle == null) {
			logger.trace("No resource bundle found.");
			w.write(key);
			return false;
		}
		String str = bundle.getString(key);
		if (StringUtils.isEmpty(str)) {
			logger.trace("No resource with name {} found.", key);
			//TODO Format string with consequent nodes passed into this directive.
			w.write(key);
			return false;
		}
		logger.trace("Resource found: {} = {}", key, str);
		w.write(str);
		return true;
	}

	private ResourceBundle getBundle(InternalContextAdapter ctx) {
		return (ResourceBundle) ctx.get(BundleDirective.BUNDLE_KEY);
	}

	private String getKey(InternalContextAdapter ctx, Node n) {
		if (n.jjtGetNumChildren() < 1) return null;
		SimpleNode nkey = (SimpleNode) n.jjtGetChild(0);
		String key = (String) nkey.value(ctx);
		return key;
	}
}
