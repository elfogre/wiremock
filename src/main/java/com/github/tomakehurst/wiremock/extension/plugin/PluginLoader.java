package com.github.tomakehurst.wiremock.extension.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.extension.Extension;

public class PluginLoader {
	
	public static List<Extension> initExtensionsInstances(ExtensionFile extensionFile, List<URLClassLoader> jarClassLoaders) {
		try {
			// Load all helpers in new object HashMap<String, Helper>
			Map<String, Helper> helpersInstances = initHelpers();
			// Load all extensions and instantiate them, modify param if value = @helpers
			return initExtensions(helpersInstances);
		} catch (Exception ex) {
			//TODO log exception
			return Collections.emptyList();
		}
	}

	private static List<Extension> initExtensions(Map<String, Helper> helpersInstances) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		List<Extension> extensions = new ArrayList<>();
		for (ExtensionDefinition extensionDefinition : extensionList) {
			extensions.add(initExtension(extensionDefinition, helpersInstances));
		}
		return extensions;
	}

	private static Extension initExtension(ExtensionDefinition extensionDefinition, Map<String, Helper> helpersInstances)
			throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
			InvocationTargetException {
		// Search constructor by params
		Class<Extension> extensionClass = (Class<Extension>) Class.forName(extensionDefinition.getExtensionClassname());
		Constructor<Extension> extensionConstructor = extractConstructor(extensionClass,
				extensionDefinition.getArguments());

		// Init params
		List<Object> initargs = new ArrayList<>();
		for (ArgumentDefinition argumentDefinition : extensionDefinition.getArguments()) {
			if (StringUtils.equalsIgnoreCase(HELPER_REF, argumentDefinition.getValue())) {
				initargs.add(helpersInstances);
			} else {
				initargs.add(instantiateParam(argumentDefinition));
			}
		}

		// Instantiate extension with constructor and params
		return extensionConstructor.newInstance(initargs);
	}

	private static Constructor<Extension> extractConstructor(Class<Extension> extensionClass,
			List<ArgumentDefinition> arguments) throws ClassNotFoundException, NoSuchMethodException {

		Class<?>[] parameterTypes = new Class[arguments.size()];
		for (int i = 0; i < arguments.size(); i++) {
			parameterTypes[i] = Class.forName(arguments.get(i).getType());
		}

		return extensionClass.getConstructor(parameterTypes);
	}

	private static Object instantiateParam(ArgumentDefinition definition) throws InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		Class paramClass = Class.forName(definition.getType());
		if (paramClass.isPrimitive()) {
			paramClass = ClassUtils.primitiveToWrapper(paramClass);
		}
		Constructor<Object> constructor = paramClass.getConstructor(String.class);
		return constructor.newInstance(definition.getValue());

	}

	private static Map<String, Helper> initHelpers()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Map<String, Helper> instantiatedHelpers = new HashMap<>();
		for (HelperDefinition helperDefinition : helpers) {
			Class<Helper> helperClass = (Class<Helper>) Class.forName(helperDefinition.getHelperClass());
			instantiatedHelpers.put(helperDefinition.getHelperName(), helperClass.newInstance());
		}
		return instantiatedHelpers;
	}
}
