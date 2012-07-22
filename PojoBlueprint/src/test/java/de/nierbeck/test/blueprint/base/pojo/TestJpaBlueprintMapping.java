package de.nierbeck.test.blueprint.base.pojo;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nierbeck.test.blueprint.base.BlueprintTestSupport;
import de.nierbeck.test.blueprint.entity.Store;
import de.nierbeck.test.blueprint.jpa.MyService;
import de.nierbeck.test.blueprint.jpa.MyServiceImpl;

/**
 * @author anierbeck
 * 
 */
public class TestJpaBlueprintMapping extends BlueprintTestSupport {

	private static final Logger logger = LoggerFactory.getLogger(TestJpaBlueprintMapping.class);

	@Override
	protected String getBlueprintDescriptor() {
		return "classpath:OSGI-INF/blueprint/my-service.xml,blueprint.xml";
	}

	@Override
	protected TinyBundle createTinyBundle() throws FileNotFoundException, MalformedURLException {
		TinyBundle tinyBundle = createTestBundle(getClass().getSimpleName(), "1.0.0",
				getBlueprintDescriptor());

		tinyBundle.add(Store.class);
		tinyBundle.add(MyService.class);
		tinyBundle.add(MyServiceImpl.class);
		tinyBundle.add("META-INF/persistence.xml", new File(
				"src/main/resources/META-INF/persistence.xml").toURL());
		tinyBundle
				.set("Import-Package",
						"javax.annotation, javax.persistence, org.apache.openjpa.enhance, "
								+ "org.apache.openjpa.persistence, org.apache.openjpa.util, org.objectweb.asm.commons, org.apache.aries.util");
		tinyBundle.set("Meta-Persistence", "META-INF/persistence.xml");

		return tinyBundle;
	}

	@Test
	public final void testDAOsFromContainerNotNull() {

		DataSource dataSource = getOsgiService(DataSource.class,
				"(osgi.jndi.service.name=jdbc/testds)");
		assertNotNull("Data source must not be null!", dataSource);

		MyService service = getOsgiService(MyService.class);
		
		assertNotNull("Service must not be null", service);
		
		Store store = new Store();
		store.setState(5);
		
		store = service.safe(store);
		
		assertNotNull(store);
		
	}
}
