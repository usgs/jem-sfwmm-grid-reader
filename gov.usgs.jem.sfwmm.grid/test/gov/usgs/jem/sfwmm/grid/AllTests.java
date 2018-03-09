package gov.usgs.jem.sfwmm.grid;

import java.io.File;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Tests all cases
 *
 * @author mckelvym
 * @since Oct 26, 2016
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ SeekableDataFileInputStreamImplTest.class, GIOHeaderTest.class,
		GIOReaderTest.class })
public class AllTests
{
	/**
	 * Ensure that a testing class has defined all required testing methods.
	 *
	 * @param p_ClassToTest
	 *            the class that is being tested
	 * @param p_TestingClass
	 *            the class that is doing the testing
	 * @since Oct 26, 2016
	 */
	public static void assertHasRequiredMethods(final Class<?> p_ClassToTest,
			final Class<?> p_TestingClass)
	{
		try
		{
			setUpBeforeClass();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			Assert.fail("Unable to set up tests: " + e.getMessage());
		}
		try
		{
			Tests.assertHasRequiredMethods(p_ClassToTest, p_TestingClass);
		}
		catch (final NoSuchMethodException e)
		{
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * @return the test file to use
	 * @since Oct 26, 2016
	 */
	public static final File getTestFile()
	{
		return new File("../test/data/eomth_stage.bin");
	}

	/**
	 * Setup logging
	 * 
	 * @throws Exception
	 * @since Mar 9, 2018
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		BasicConfigurator.configure();
	}
}
