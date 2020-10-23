package gov.usgs.jem.sfwmm.grid;

import com.google.common.collect.Range;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
	 * @since Oct 28, 2016
	 */
	private static final int	NUM_DATES	= 433;
	/**
	 * @since Oct 28, 2016
	 */
	private static final int	NUM_NODES	= 1746;
	/**
	 * @since Oct 28, 2016
	 */
	private static final int	NUM_ROWS	= 65;

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

	/**
	 * Unpacks the values in the provided array as a {@link Stream} of
	 * {@link Float}
	 *
	 * @param p_Values
	 *            the values
	 * @return a {@link Stream} of {@link Float}
	 * @since Nov 4, 2016
	 */
	private static Stream<Float> toStream(final float[] p_Values)
	{
		return IntStream.range(0, p_Values.length)
				.mapToObj(i -> (Float) p_Values[i]);
	}

	@SuppressWarnings("javadoc")
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
	 * Test method for {@link gov.usgs.jem.sfwmm.grid.GIOReader#close()}.
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
	 * Test method for {@link gov.usgs.jem.sfwmm.grid.GIOReader#getDates()}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testGetDates() throws ParseException, IOException
	{
		final List<Date> dates = m_Reader.getDates();
		Assert.assertEquals(NUM_DATES, dates.size());
		Assert.assertEquals(-157766400000L, dates.get(0).getTime());
		Assert.assertEquals(978220800000L,
				dates.get(dates.size() - 1).getTime());
	}

	/**
	 * Test method for {@link gov.usgs.jem.sfwmm.grid.GIOReader#getFilePath()}.
	 */
	@Test
	public void testGetFilePath()
	{
		Assert.assertEquals(AllTests.getTestFile().getAbsolutePath(),
				m_Reader.getFilePath());
	}

	/**
	 * Test method for {@link gov.usgs.jem.sfwmm.grid.GIOReader#getHeader()}.
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
		Assert.assertEquals(NUM_ROWS, rowsSize);
		Assert.assertEquals(NUM_NODES, nodesSize);
		Assert.assertEquals(10560.0f, sizeX, Float.MIN_NORMAL);
		Assert.assertEquals(10560.0f, sizeY, Float.MIN_NORMAL);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#getNoDataValue()}.
	 *
	 */
	@Test
	public void testGetNoDataValue()
	{
		Assert.assertTrue(Float.isNaN(m_Reader.getNoDataValue()));
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#open(java.lang.String)}.
	 */
	@SuppressWarnings("static-method")
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
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#openDebug(java.lang.String)}.
	 */
	@SuppressWarnings("static-method")
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

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#readData(com.google.common.collect.Range, com.google.common.collect.Range, com.google.common.collect.Range)}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testReadData() throws IOException, ParseException
	{
		final Stream<Float> values = toStream(
				m_Reader.readData(Range.all(), Range.all(), Range.all()));
		final DoubleSummaryStatistics summaryStatistics = values
				.filter(v -> !Float.isNaN(v)).mapToDouble(Float::doubleValue)
				.summaryStatistics();
		Assert.assertEquals(NUM_NODES,
				summaryStatistics.getCount() / NUM_DATES);
		Assert.assertEquals(8.634088f, summaryStatistics.getAverage(),
				0.000001);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#readData(com.google.common.collect.Range, com.google.common.collect.Range, com.google.common.collect.Range)}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testReadData2() throws IOException, ParseException
	{
		final Stream<Float> values = toStream(
				m_Reader.readData(Range.singleton(1),
						Range.singleton(NUM_ROWS - 1), Range.singleton(23)));
		final DoubleSummaryStatistics summaryStatistics = values
				.filter(v -> !Float.isNaN(v)).mapToDouble(Float::doubleValue)
				.summaryStatistics();
		Assert.assertEquals(1, summaryStatistics.getCount());
		Assert.assertEquals(13.79547f, summaryStatistics.getAverage(), 0.00001);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#readData(com.google.common.collect.Range, com.google.common.collect.Range, com.google.common.collect.Range)}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testReadData3() throws IOException, ParseException
	{
		final Stream<Float> values = toStream(
				m_Reader.readData(Range.singleton(1),
						Range.singleton(NUM_ROWS - 1), Range.atMost(23)));
		final DoubleSummaryStatistics summaryStatistics = values
				.filter(v -> !Float.isNaN(v)).mapToDouble(Float::doubleValue)
				.summaryStatistics();
		Assert.assertEquals(1, summaryStatistics.getCount());
		Assert.assertEquals(13.79547f, summaryStatistics.getAverage(), 0.00001);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#readData(com.google.common.collect.Range, com.google.common.collect.Range, com.google.common.collect.Range)}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testReadData4() throws IOException, ParseException
	{
		final Stream<Float> values = toStream(m_Reader.readData(Range.atMost(0),
				Range.atLeast(NUM_ROWS - 1), Range.atMost(23)));
		final DoubleSummaryStatistics summaryStatistics = values
				.filter(v -> !Float.isNaN(v)).mapToDouble(Float::doubleValue)
				.summaryStatistics();
		Assert.assertEquals(1, summaryStatistics.getCount());
		Assert.assertEquals(13.72564f, summaryStatistics.getAverage(), 0.00001);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#readData(com.google.common.collect.Range, com.google.common.collect.Range, com.google.common.collect.Range)}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testReadData5() throws IOException, ParseException
	{
		final Stream<Float> values = toStream(
				m_Reader.readData(Range.closed(0, 1),
						Range.singleton(NUM_ROWS - 1), Range.singleton(23)));
		final DoubleSummaryStatistics summaryStatistics = values
				.filter(v -> !Float.isNaN(v)).mapToDouble(Float::doubleValue)
				.summaryStatistics();
		Assert.assertEquals(2, summaryStatistics.getCount());
		Assert.assertEquals(13.760555f, summaryStatistics.getAverage(),
				0.00001);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#readData(com.google.common.collect.Range, com.google.common.collect.Range, com.google.common.collect.Range)}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testReadData6() throws IOException, ParseException
	{
		final Stream<Float> values = toStream(
				m_Reader.readData(Range.singleton(0),
						Range.singleton(NUM_ROWS - 1), Range.singleton(29)));
		final DoubleSummaryStatistics summaryStatistics = values
				.filter(v -> !Float.isNaN(v)).mapToDouble(Float::doubleValue)
				.summaryStatistics();
		Assert.assertEquals(1, summaryStatistics.getCount());
		Assert.assertEquals(25.337408f, summaryStatistics.getAverage(),
				0.00001);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.sfwmm.grid.GIOReader#readData(com.google.common.collect.Range, com.google.common.collect.Range, com.google.common.collect.Range)}.
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testReadData7() throws IOException, ParseException
	{
		final Stream<Float> values = toStream(
				m_Reader.readData(Range.closed(0, 1),
						Range.singleton(NUM_ROWS - 2), Range.singleton(28)));
		final DoubleSummaryStatistics summaryStatistics = values
				.filter(v -> !Float.isNaN(v)).mapToDouble(Float::doubleValue)
				.summaryStatistics();
		Assert.assertEquals(2, summaryStatistics.getCount());
		Assert.assertEquals(24.3613235f, summaryStatistics.getAverage(),
				0.00001);
	}
}
