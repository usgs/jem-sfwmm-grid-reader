package gov.usgs.jem.swfmm.grid.example;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.BasicConfigurator;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.Table;

import gov.usgs.jem.swfmm.grid.AllTests;
import gov.usgs.jem.swfmm.grid.GIOHeader;
import gov.usgs.jem.swfmm.grid.GIOReader;

/**
 * Example using {@link GIOReader}
 *
 * @author mckelvym
 * @since Oct 28, 2016
 *
 */
public final class Main
{
	/**
	 * Class logger
	 */
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(Main.class);

	public static void main(final String[] args) throws IOException
	{
		BasicConfigurator.configure();

		final File testFile = AllTests.getTestFile();
		for (final File file : testFile.getParentFile().listFiles())
		{
			try (
				GIOReader reader = GIOReader.openDebug(file.getAbsolutePath());)
			{
				final GIOHeader header = reader.getHeader();
				log.info(header);
				final List<Date> dates = reader.getDates();
				log.info(String.format("Dates: %s (%s - %s)", dates.size(),
						dates.get(0), Iterables.getLast(dates)));
				{
					final List<Table<Integer, Integer, Float>> data = reader
							.readData(Range.singleton(0), Range.all(),
									Range.all());
					data.forEach(slice ->
					{
						for (int row = header.getRowsSize()
								- 1; row >= 0; row--)
						{
							for (int col = 0; col < header.getColsSize(); col++)
							{
								final boolean contains = slice.contains(row,
										col);
								if (contains)
								{
									final Float value = slice.get(row, col);
									if (!Float.isNaN(value))
									{
										System.out.print("*");
									}
									else
									{
										System.out.print(".");
									}

								}
								else
								{
									System.out.print("_");
								}
							}
							System.out.println();
						}
					});
				}
				log.info("Day 1 values...");
				{
					final List<Table<Integer, Integer, Float>> data = reader
							.readData(Range.singleton(0), Range.all(),
									Range.all());
					data.forEach(slice ->
					{
						for (int row = header.getRowsSize()
								- 1; row >= 0; row--)
						{
							for (int col = 0; col < header.getColsSize(); col++)
							{
								final boolean contains = slice.contains(row,
										col);
								if (contains)
								{
									final Float value = slice.get(row, col);
									if (!Float.isNaN(value))
									{
										System.out.print(String.format("%5.2f ",
												slice.get(row, col)));
									}
									else
									{
										System.out.print("   . ");
									}

								}
								else
								{
									System.out.print("   _ ");
								}
							}
							System.out.println();
						}
					});
				}
			}
			catch (final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
}
