package gov.usgs.jem.swfmm.grid.input;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.usgs.jem.swfmm.grid.AllTests;
import gov.usgs.jem.swfmm.grid.GIOHeader;

/**
 * Tests {@link GIOReader}
 *
 * @author mckelvym
 * @since Oct 26, 2016
 *
 */
public class GIOReaderTest
{

	/**
	 *
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 26, 2016
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		final Class<?> classToTest = GIOReader.class;
		final Class<?> testingClass = GIOReaderTest.class;
		AllTests.assertHasRequiredMethods(classToTest, testingClass);
	}

	private GIOReader m_Reader;

	/**
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 26, 2016
	 */
	@Before
	public void setUp() throws Exception
	{
		m_Reader = GIOReader.open(AllTests.getTestFile().getAbsolutePath());
	}

	/**
	 *
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 26, 2016
	 */
	@After
	public void tearDown() throws Exception
	{
		m_Reader.close();
	}

	/**
	 * Test method for {@link gov.usgs.jem.swfmm.grid.input.GIOReader#close()}.
	 *
	 * @throws IOException
	 *             unable to close
	 */
	@Test
	public void testClose() throws IOException
	{
		m_Reader.close();
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.swfmm.grid.input.GIOReader#getDates()}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testGetDates() throws ParseException, IOException
	{
		final List<Date> dates = m_Reader.getDates();
		Assert.assertEquals(433, dates.size());
		Assert.assertEquals(-157766400000L, dates.get(0).getTime());
		Assert.assertEquals(978220800000L,
				dates.get(dates.size() - 1).getTime());
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.swfmm.grid.input.GIOReader#getFilePath()}.
	 */
	@Test
	public void testGetFilePath()
	{
		Assert.assertEquals(AllTests.getTestFile().getAbsolutePath(),
				m_Reader.getFilePath());
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.swfmm.grid.input.GIOReader#getHeader()}.
	 */
	@Test
	public void testGetHeader()
	{
		final GIOHeader header = m_Reader.getHeader();
		Assert.assertNotNull(header);
		final String title = header.getTitle();
		final int rowsSize = header.getRowsSize();
		final int nodesSize = header.getNodesSize();
		final float sizeX = header.getSizeX();
		final float sizeY = header.getSizeY();

		Assert.assertEquals("OPTB2 - CERP with LORS2008", title);
		Assert.assertEquals(65, rowsSize);
		Assert.assertEquals(1746, nodesSize);
		Assert.assertEquals(10560.0f, sizeX, Float.MIN_NORMAL);
		Assert.assertEquals(10560.0f, sizeY, Float.MIN_NORMAL);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.swfmm.grid.input.GIOReader#open(java.lang.String)}.
	 */
	@Test
	public void testOpen()
	{
		try (GIOReader open = GIOReader
				.open(AllTests.getTestFile().getAbsolutePath());)
		{
			Assert.assertNotNull(open);
		}
		catch (final Throwable t)
		{
			t.printStackTrace();
			Assert.fail("Unable to open file.");
		}
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.swfmm.grid.input.GIOReader#openDebug(java.lang.String)}.
	 */
	@Test
	public void testOpenDebug()
	{
		try (GIOReader open = GIOReader
				.openDebug(AllTests.getTestFile().getAbsolutePath());)
		{
			Assert.assertNotNull(open);
		}
		catch (final Throwable t)
		{
			t.printStackTrace();
			Assert.fail("Unable to open file.");
		}
	}

}
