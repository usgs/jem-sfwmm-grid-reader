package gov.usgs.jem.sfwmm.grid.iosp;

import java.util.Random;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Tests {@link SFWMMGridIOSP}
 *
 * @author mckelvym
 * @since Oct 28, 2016
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SFWMMGridIOSPTest
{

	private static final Random r;

	static
	{
		r = new Random(System.currentTimeMillis());
	}

	/**
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 28, 2016
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		// final Class<?> classToTest = SFWMMGridIOSP.class;
		// final Class<?> testingClass = SFWMMGridIOSPTest.class;
		// AllTests.assertHasRequiredMethods(classToTest, testingClass);
	}

	/**
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 28, 2016
	 */
	@Before
	public void setUp() throws Exception
	{
	}

	/**
	 * @throws Exception
	 */
	@Test
	public final void testNothing() throws Exception
	{
	}

}
