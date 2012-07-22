package de.nierbeck.test.blueprint.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.camel.impl.DefaultClassResolver;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.test.CamelTestSupport;
import org.apache.camel.test.blueprint.CamelBlueprintHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ResourceHelper;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class BlueprintTestSupport {
	private static final Logger LOG = LoggerFactory.getLogger(BlueprintTestSupport.class);

	private static final ClassResolver RESOLVER = new DefaultClassResolver();

	private BundleContext bundleContext;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		String symbolicName = getClass().getSimpleName();

		this.bundleContext = CamelBlueprintHelper.createBundleContext(symbolicName, getBundleFilter(), createTinyBundle());

		// must wait for blueprint container to be published then the namespace
		// parser is complete and we are ready for testing
		LOG.debug("Waiting for BlueprintContainer to be published with symbolicName: {}", symbolicName);
		getOsgiService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		CamelBlueprintHelper.disposeBundleContext(bundleContext);
		CamelTestSupport.deleteDirectory("target/bundles");
	}

	/**
	 * Gets the bundle descriptor from the classpath.
	 * <p/>
	 * Return the location(s) of the bundle descriptors from the classpath.
	 * Separate multiple locations by comma, or return a single location.
	 * <p/>
	 * For example override this method and return
	 * <tt>OSGI-INF/blueprint/camel-context.xml</tt>
	 *
	 * @return the location of the bundle descriptor file.
	 */
	protected abstract String getBlueprintDescriptor();


	protected abstract TinyBundle createTinyBundle() throws FileNotFoundException, MalformedURLException;

	/**
	 * Gets filter expression of bundle descriptors. Modify this method if you
	 * wish to change default behavior.
	 *
	 * @return filter expression for OSGi bundles.
	 */
	protected String getBundleFilter() {
		return CamelBlueprintHelper.BUNDLE_FILTER;
	}

	/**
	 * Gets test bundle version. Modify this method if you wish to change
	 * default behavior.
	 *
	 * @return test bundle version
	 */
	protected String getBundleVersion() {
		return CamelBlueprintHelper.BUNDLE_VERSION;
	}

	protected <T> T getOsgiService(Class<T> type) {
		return CamelBlueprintHelper.getOsgiService(bundleContext, type);
	}

	protected <T> T getOsgiService(Class<T> type, long timeout) {
		return CamelBlueprintHelper.getOsgiService(bundleContext, type, timeout);
	}

	protected <T> T getOsgiService(Class<T> type, String filter) {
		return CamelBlueprintHelper.getOsgiService(bundleContext, type, filter);
	}

	protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
		return CamelBlueprintHelper.getOsgiService(bundleContext, type, filter, timeout);
	}

	/**
	 * Copied from CamelBlueprintHelper since there it is also a protected method.
	 *
	 * @param name
	 * @param version
	 * @param descriptors
	 * @return
	 * @throws FileNotFoundException
	 * @throws MalformedURLException
	 */
	protected static TinyBundle createTestBundle(String name, String version, String descriptors) throws FileNotFoundException, MalformedURLException {
        TinyBundle bundle = TinyBundles.newBundle();
        for (URL url : getBlueprintDescriptors(descriptors)) {
            LOG.info("Using Blueprint XML file: " + url.getFile());
            bundle.add("OSGI-INF/blueprint/blueprint-" + url.getFile().replace("/", "-"), url);
        }
        bundle.set("Manifest-Version", "2")
                .set("Bundle-ManifestVersion", "2")
                .set("Bundle-SymbolicName", name)
                .set("Bundle-Version", version);
        return bundle;
    }

    /**
     * Copied from CamelBlueprintHelper since there it is also a private method.
     *
     * Gets the bundle descriptors as {@link URL} resources.
     *
     * @param descriptors the bundle descriptors, can be separated by comma
     * @return the bundle descriptors.
     * @throws FileNotFoundException is thrown if a bundle descriptor cannot be found
     */
    private static Collection<URL> getBlueprintDescriptors(String descriptors) throws FileNotFoundException, MalformedURLException {
        List<URL> answer = new ArrayList<URL>();
        String descriptor = descriptors;
        if (descriptor != null) {
            // there may be more resources separated by comma
            Iterator<Object> it = ObjectHelper.createIterator(descriptor);
            while (it.hasNext()) {
                String s = (String) it.next();
                LOG.trace("Resource descriptor: {}", s);

                // remove leading / to be able to load resource from the classpath
                s = FileUtil.stripLeadingSeparator(s);

                // if there is wildcards for *.xml then we need to find the urls from the package
                if (s.endsWith("*.xml")) {
                    String packageName = s.substring(0, s.length() - 5);
                    // remove trailing / to be able to load resource from the classpath
                    Enumeration<URL> urls = ObjectHelper.loadResourcesAsURL(packageName);
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        File dir = new File(url.getFile());
                        if (dir.isDirectory()) {
                            File[] files = dir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    if (file.isFile() && file.exists() && file.getName().endsWith(".xml")) {
                                        String name = packageName + file.getName();
                                        LOG.debug("Resolving resource: {}", name);
                                        URL xmlUrl = ObjectHelper.loadResourceAsURL(name);
                                        if (xmlUrl != null) {
                                            answer.add(xmlUrl);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOG.debug("Resolving resource: {}", s);
                    URL url = ResourceHelper.resolveMandatoryResourceAsUrl(RESOLVER, s);
                    if (url == null) {
                        throw new FileNotFoundException("Resource " + s + " not found");
                    }
                    answer.add(url);
                }
            }
        } else {
            throw new IllegalArgumentException("No bundle descriptor configured. Override getBlueprintDescriptor() or getBlueprintDescriptors() method");
        }

        if (answer.isEmpty()) {
            throw new IllegalArgumentException("Cannot find any resources in classpath from descriptor " + descriptors);
        }
        return answer;
    }
}
