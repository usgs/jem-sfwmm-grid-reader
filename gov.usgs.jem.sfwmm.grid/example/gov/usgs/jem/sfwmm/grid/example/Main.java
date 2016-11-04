package gov.usgs.jem.sfwmm.grid.example;

import static com.google.common.base.Preconditions.checkElementIndex;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.BasicConfigurator;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import gov.usgs.jem.sfwmm.grid.AllTests;
import gov.usgs.jem.sfwmm.grid.GIOHeader;
import gov.usgs.jem.sfwmm.grid.GIOReader;

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
					final float[] data = reader.readData(Range.singleton(0),
							Range.all(), Range.all());

					for (int row = header.getRowsSize() - 1; row >= 0; row--)
					{
						for (int col = 0; col < header.getColsSize(); col++)
						{
							final int index = row * header.getColsSize() + col;
							checkElementIndex(index, data.length);
							final Float value = data[index];
							if (!Float.isNaN(value))
							{
								System.out.print("*");
							}
							else
							{
								System.out.print(".");
							}

						}
						System.out.println();
					}
				}
				log.info("Day 1 values...");
				{
					final float[] data = reader.readData(Range.singleton(0),
							Range.all(), Range.all());
					for (int row = header.getRowsSize() - 1; row >= 0; row--)
					{
						for (int col = 0; col < header.getColsSize(); col++)
						{
							final int index = row * header.getColsSize() + col;
							checkElementIndex(index, data.length);
							final Float value = data[index];
							if (!Float.isNaN(value))
							{
								System.out
										.print(String.format("%5.2f ", value));
							}
							else
							{
								System.out.print(" . ");
							}
						}
						System.out.println();
					}
				}
			}
			catch (final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
}
