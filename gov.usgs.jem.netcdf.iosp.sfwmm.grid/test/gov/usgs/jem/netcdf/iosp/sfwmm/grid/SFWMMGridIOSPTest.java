package gov.usgs.jem.netcdf.iosp.sfwmm.grid;

import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

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

	/**
	 * Makes a new {@link NetcdfFile} instance.
	 *
	 * @author mckelvym
	 * @since Aug 20, 2015
	 *
	 */
	private static class MakeNetcdfFile extends NetcdfFile
	{
		/**
		 * Ctor
		 *
		 * @param p_SPI
		 *            IOServiceProvider
		 * @param p_Raf
		 *            RandomAccessFile
		 * @param p_Location
		 *            location of file?
		 * @param p_CancelTask
		 *            CancelTask
		 *
		 * @throws IOException
		 *             problem opening the file
		 */
		MakeNetcdfFile(final IOServiceProvider p_SPI,
				final RandomAccessFile p_Raf, final String p_Location,
				final CancelTask p_CancelTask) throws IOException
		{
			super(p_SPI, p_Raf, p_Location, p_CancelTask);
		}
	}

	/**
	 * @since Oct 28, 2016
	 */
	private static final int	NUM_COLS	= 42;

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
	 * Create a new, empty {@link NetcdfFile} instance served by the
	 * {@link IOServiceProvider} and using the provided {@link RandomAccessFile}
	 * . The random access file will be closed when {@link NetcdfFile#close()}
	 * is called. The path of the random access file is used as the location for
	 * the NetCDF file.
	 *
	 * @param p_SPI
	 * @param p_Raf
	 * @return
	 * @throws IOException
	 * @since Aug 28, 2015
	 */
	private static NetcdfFile createNetcdfFile(final IOServiceProvider p_SPI,
			final RandomAccessFile p_Raf) throws IOException
	{
		return new MakeNetcdfFile(p_SPI, p_Raf, p_Raf.getLocation(), null)
		{
			@Override
			public synchronized void close() throws IOException
			{
				super.close();
				p_Raf.close();
			}
		};
	}

	/**
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 28, 2016
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		final Class<?> classToTest = SFWMMGridIOSP.class;
		final Class<?> testingClass = SFWMMGridIOSPTest.class;
		AllTests.assertHasRequiredMethods(classToTest, testingClass);
	}

	/**
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 28, 2016
	 */
	@SuppressWarnings("static-method")
	@Before
	public void setUp() throws Exception
	{
		NetcdfFile.registerIOProvider(SFWMMGridIOSP.class, true);
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP#close()}.
	 *
	 * @throws IOException
	 */
	@Test
	public void testClose() throws IOException
	{
		final File refFile = AllTests.getTestFile();
		final File inputFile = File.createTempFile(getClass().getSimpleName(),
				refFile.getName(), refFile.getParentFile());
		inputFile.deleteOnExit();
		Files.copy(refFile, inputFile);

		try (NetcdfFile nc = NetcdfFile.open(inputFile.getAbsolutePath()))
		{
			nc.close();
			Assert.assertTrue("Unable to delete.", inputFile.delete());
		}
		catch (final IOException e)
		{
			final String message = String.format("Unable to close file: %s",
					inputFile);
			e.printStackTrace();
			fail(message);
		}
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP#getFileTypeDescription()}.
	 *
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void testGetFileTypeDescription() throws IOException
	{
		try (NetcdfFile nc = NetcdfFile
				.open(AllTests.getTestFile().getAbsolutePath()))
		{
			Assert.assertEquals(SFWMMGridIOSP.class.getSimpleName(),
					nc.getFileTypeDescription());
		}
		catch (final IOException e)
		{
			final String message = String.format(
					"Unable to get file type description for %s",
					AllTests.getTestFile());
			e.printStackTrace();
			fail(message);
		}
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP#getFileTypeId()}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public void testGetFileTypeId()
	{
		try (NetcdfFile nc = NetcdfFile
				.open(AllTests.getTestFile().getAbsolutePath()))
		{
			Assert.assertEquals(SFWMMGridIOSP.class.getCanonicalName(),
					nc.getFileTypeId());
		}
		catch (final IOException e)
		{
			final String message = String.format(
					"Unable to get file type description for %s",
					AllTests.getTestFile());
			e.printStackTrace();
			fail(message);
		}
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP#isValidFile(ucar.unidata.io.RandomAccessFile)}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public void testIsValidFile()
	{
		final SFWMMGridIOSP iosp = new SFWMMGridIOSP();
		try (RandomAccessFile raf = new RandomAccessFile(
				AllTests.getTestFile().getAbsolutePath(), "r"))
		{
			Assert.assertEquals(true, iosp.isValidFile(raf));
		}
		catch (final IOException e)
		{
			final String message = String.format(
					"Unable to test if the file is valid: %s",
					AllTests.getTestFile());
			e.printStackTrace();
			fail(message);
		}
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP#open(RandomAccessFile, NetcdfFile, ucar.nc2.util.CancelTask)}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public void testOpen()
	{
		try (RandomAccessFile raf = new RandomAccessFile(
				AllTests.getTestFile().getAbsolutePath(), "r"))
		{
			final SFWMMGridIOSP iosp = new SFWMMGridIOSP();
			try (final NetcdfFile nc = createNetcdfFile(iosp, raf))
			{
				final Set<String> coordVars = Sets.newHashSet(
						SFWMMGridIOSP.TIME_VAR_NAME, SFWMMGridIOSP.Y_VAR_NAME,
						SFWMMGridIOSP.X_VAR_NAME);
				for (final String coordVar : coordVars)
				{
					final Dimension d = nc.findDimension(coordVar);
					Assert.assertNotNull(d);
					Assert.assertTrue(d.getLength() > 0);
					final Variable v = nc.findVariable(coordVar);
					Assert.assertNotNull(v);
					Assert.assertEquals(d.getLength(), v.getSize());

					if (coordVar.equals(SFWMMGridIOSP.TIME_VAR_NAME))
					{
						Assert.assertEquals(NUM_DATES, d.getLength());
					}
					else if (coordVar.equals(SFWMMGridIOSP.Y_VAR_NAME))
					{
						Assert.assertEquals(NUM_ROWS, d.getLength());
					}
					else if (coordVar.equals(SFWMMGridIOSP.X_VAR_NAME))
					{
						Assert.assertEquals(NUM_COLS, d.getLength());
					}

					for (final String attr : new String[] {
							SFWMMGridIOSP.COORDINATE_AXIS_TYPE,
							SFWMMGridIOSP.LONG_NAME,
							SFWMMGridIOSP.STANDARD_NAME, SFWMMGridIOSP.AXIS,
							SFWMMGridIOSP.CHUNK_SIZES, SFWMMGridIOSP.UNITS })
					{
						if (coordVar.equals(SFWMMGridIOSP.TIME_VAR_NAME)
								&& attr.equals(SFWMMGridIOSP.STANDARD_NAME))
						{
							continue;
						}
						final Attribute cat = v.findAttribute(attr);
						Assert.assertNotNull(cat);
						if (attr.equals(SFWMMGridIOSP.AXIS))
						{
							/**
							 * Attribut contains first char of dimension name
							 */
							Assert.assertTrue(
									cat.getStringValue().toLowerCase().contains(
											d.getShortName().substring(0, 1)));
						}
						else if (attr.equals(SFWMMGridIOSP.CHUNK_SIZES)
								|| attr.equals(SFWMMGridIOSP.UNITS))
						{
							continue;
						}
						else
						{
							/**
							 * Attribute contains dimension name
							 */
							Assert.assertTrue(cat.getStringValue().toLowerCase()
									.contains(d.getShortName()));
						}
					}

				}

				Assert.assertNotNull(nc.findVariable("transverse_mercator"));
			}
		}
		catch (final IOException e)
		{
			fail("Unable to test if file is valid: " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP#readData(ucar.nc2.Variable, ucar.ma2.Section)}.
	 *
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void testReadData() throws IOException
	{
		try (NetcdfFile nc = NetcdfFile
				.open(AllTests.getTestFile().getAbsolutePath()))
		{
			final Variable variable = nc.findVariable(Files
					.getNameWithoutExtension(AllTests.getTestFile().getName()));
			final IndexIterator indexIterator = variable.read()
					.getIndexIterator();
			final List<Float> values = Lists.newArrayList();
			while (indexIterator.hasNext())
			{
				values.add(indexIterator.getFloatNext());
			}

			final DoubleSummaryStatistics summaryStatistics = values.stream()
					.filter(v -> !Float.isNaN(v))
					.mapToDouble(Float::doubleValue).summaryStatistics();
			Assert.assertEquals(NUM_NODES,
					summaryStatistics.getCount() / NUM_DATES);
			Assert.assertEquals(8.634088f, summaryStatistics.getAverage(),
					0.000001);
		}
	}

	/**
	 * Test method for
	 * {@link gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP#readData(ucar.nc2.Variable, ucar.ma2.Section)}.
	 *
	 * @throws IOException
	 * @throws InvalidRangeException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void testReadData2() throws IOException, InvalidRangeException
	{
		try (NetcdfFile nc = NetcdfFile
				.open(AllTests.getTestFile().getAbsolutePath()))
		{
			final Variable variable = nc.findVariable(Files
					.getNameWithoutExtension(AllTests.getTestFile().getName()));
			final IndexIterator indexIterator = variable
					.read(new int[] { 1, NUM_ROWS - 1, 23 },
							new int[] { 1, 1, 1 })
					.getIndexIterator();
			final List<Float> values = Lists.newArrayList();
			while (indexIterator.hasNext())
			{
				values.add(indexIterator.getFloatNext());
			}

			final DoubleSummaryStatistics summaryStatistics = values.stream()
					.filter(v -> !Float.isNaN(v))
					.mapToDouble(Float::doubleValue).summaryStatistics();
			Assert.assertEquals(1, summaryStatistics.getCount());
			Assert.assertEquals(13.79547f, summaryStatistics.getAverage(),
					0.00001);
		}
	}

}
