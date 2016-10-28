package gov.usgs.jem.swfmm.grid.input;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Level;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.io.Files;

import gov.usgs.jem.swfmm.grid.GIOHeader;

/**
 * Reads SFWMM GridIO files.
 *
 * @author mckelvym
 * @since Oct 25, 2016
 *
 */
public final class GIOReader implements Closeable
{
	/**
	 * The number of bytes used for grid tags
	 *
	 * @since Oct 25, 2016
	 */
	private static final int				GRID_TAG_LENGTH	= 80;

	/**
	 * Class logger
	 */
	private static org.apache.log4j.Logger	log				= org.apache.log4j.Logger
			.getLogger(GIOReader.class);

	/**
	 * Used mainly for "toString" implementations, this takes an array
	 * transforms it into a new array that retains the first two elements, adds
	 * ellipses as the third element, and retains the last element as the fourth
	 * output element. For each element, calls its toString() method.
	 *
	 * @param p_Array
	 *            An input array
	 * @return a new array representing an "abbreviated" version of the input
	 *         array
	 * @since Oct 25, 2016
	 */
	private static String[] abbreviate(final Object[] p_Array)
	{
		if (p_Array == null)
		{
			return new String[0];
		}
		if (p_Array.length <= 4)
		{
			final String[] output = new String[p_Array.length];
			for (int i = 0; i < p_Array.length; i++)
			{
				output[i] = String.valueOf(p_Array[i]);
			}
			return output;
		}

		final String[] output = new String[4];
		output[0] = String.valueOf(p_Array[0]);
		output[1] = String.valueOf(p_Array[1]);
		output[2] = "...";
		output[3] = String.valueOf(p_Array[p_Array.length - 1]);
		return output;
	}

	/**
	 * Open the SFWMM GridIO file at the provided path and read its header
	 *
	 * @param p_FilePath
	 *            the path to the SFWMM GridIO file
	 * @return the {@link GIOReader}
	 * @throws IOException
	 *             if the file could not be opened for any reason
	 * @since Oct 25, 2016
	 */
	public static GIOReader open(final String p_FilePath) throws IOException
	{
		log.setLevel(Level.INFO);
		return openInternal(p_FilePath);
	}

	/**
	 * Open the SFWMM GridIO file at the provided path and read its header.
	 * Debugging messages will be logged.
	 *
	 * @param p_FilePath
	 *            the path to the SFWMM GridIO file
	 * @return the {@link GIOReader}
	 * @throws IOException
	 *             if the file could not be opened for any reason
	 * @since Oct 25, 2016
	 */
	public static GIOReader openDebug(final String p_FilePath)
			throws IOException
	{
		return openInternal(p_FilePath);
	}

	/**
	 * Open the SFWMM GridIO file at the provided path and read its header.
	 * Debugging messages will be logged.
	 *
	 * @param p_FilePath
	 *            the path to the SFWMM GridIO file
	 * @return the {@link GIOReader}
	 * @throws IOException
	 *             if the file could not be opened for any reason
	 * @since Oct 25, 2016
	 */
	private static GIOReader openInternal(final String p_FilePath)
			throws IOException
	{
		checkNotNull(p_FilePath, "File path required.");
		checkArgument(
				Objects.equal("bin",
						Files.getFileExtension(p_FilePath).toLowerCase()),
				"SFWMM GridIO file required, but got %s instead", p_FilePath);
		final GIOReader binReader = new GIOReader(p_FilePath);
		binReader.readHeader();
		return binReader;
	}

	/**
	 * Maps 0-based row to range of available cells (columns) within the row
	 *
	 * @since Oct 28, 2016
	 */
	private final Map<Integer, Range<Integer>>	m_AvailabilityMap;

	/**
	 * The {@link ByteOrder} to read from the file.
	 *
	 * @since Oct 25, 2016
	 */
	private final ByteOrder						m_ByteOrder;

	/**
	 * Date formatter for tags
	 *
	 * @since Oct 27, 2016
	 */
	private final SimpleDateFormat				m_DateFormat;

	/**
	 * @see #getDates()
	 * @since Oct 27, 2016
	 */
	private final List<Date>					m_Dates;

	/**
	 * The data input stream used to read from the file.
	 *
	 * @since Oct 25, 2016
	 */
	private SeekableDataFileInputStream			m_DIS;

	/**
	 * Number of bytes in the file.
	 *
	 * @since Oct 28, 2016
	 */
	private final long							m_FileLength;

	/**
	 * The path to the SFWMM GridIO file
	 *
	 * @see #getFilePath()
	 * @since Oct 25, 2016
	 */
	private final String						m_FilePath;

	/**
	 * Number of bytes for one timestep grid
	 *
	 * @since Oct 27, 2016
	 */
	private long								m_GridSize;

	/**
	 * The byte offset in the file to read grids from
	 *
	 * @since Oct 27, 2016
	 */
	private long								m_GridStartByte;

	/**
	 * The header for the SFWMM GridIO file
	 *
	 * @see #getHeader()
	 * @since Oct 25, 2016
	 */
	private GIOHeader							m_Header;

	/**
	 * Create a new reader for the SFWMM GridIO file at the provided path
	 *
	 * @param p_FilePath
	 *            path to the SFWMM GridIO file
	 * @since Oct 25, 2016
	 */
	private GIOReader(final String p_FilePath)
	{
		m_FilePath = checkNotNull(p_FilePath);
		m_ByteOrder = ByteOrder.BIG_ENDIAN;
		/**
		 * Tags have been observed to look like "January 1, 1965"
		 */
		m_DateFormat = new SimpleDateFormat("MMMM d, yyyy");
		m_DateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		m_FileLength = new File(p_FilePath).length();
		m_Dates = Lists.newArrayList();
		m_AvailabilityMap = Maps.newTreeMap();
	}

	/**
	 * Close the reader.
	 *
	 * @throws IOException
	 *             if closing the internal {@link SeekableDataFileInputStream}
	 *             failed
	 * @since Oct 25, 2016
	 */
	@Override
	public void close() throws IOException
	{
		if (m_DIS != null)
		{
			try
			{
				m_DIS.close();
			}
			catch (final IOException e)
			{
				throw e;
			}
			finally
			{
				m_DIS = null;
			}
		}
	}

	/**
	 * Get the list of "tags" in the file
	 *
	 * @return the list of tags
	 * @throws ParseException
	 *             if a date could not be parsed successfully from the file
	 * @throws IOException
	 *             unable to read a portion of the file
	 * @since Oct 27, 2016
	 */
	public List<Date> getDates() throws ParseException, IOException
	{
		/**
		 * Tag names (dates) are dynamically read in and parsed once, on-the-fly
		 */
		if (m_Dates.isEmpty())
		{
			/**
			 * It has been observed that the same tag name will appear multiple
			 * times in a dataset
			 */
			final SortedMultiset<Date> dates = TreeMultiset.create();
			/**
			 * Start at the first grid tag location
			 */
			m_DIS.seek((int) m_GridStartByte);

			/**
			 * Read until EOF
			 */
			while (m_DIS.getPosition() < m_FileLength)
			{
				/**
				 * Ensure that enough bytes exist to continue reading tag name
				 */
				if (m_DIS.getPosition() + GRID_TAG_LENGTH >= m_FileLength)
				{
					break;
				}
				/**
				 * Read tag and parse as date
				 */
				final String tag = new String(
						m_DIS.readCharsAsAscii(GRID_TAG_LENGTH)).trim();
				final Date date = m_DateFormat.parse(tag);
				dates.add(date);
				/**
				 * Skip the data that follows the tag name
				 */
				final int skipBytes = (int) (m_GridSize - GRID_TAG_LENGTH);
				if (m_DIS.skipBytes(skipBytes) < skipBytes)
				{
					break;
				}
			}

			/**
			 * Since multiple entries for the same date (tag name) may exist,
			 * subdivide the milliseconds in that day by the count of
			 * occurrences. E.g. if a tag appears twice, the first date will be
			 * at midnight, the second will be at noon.
			 */
			final long dayMS = 1000 * 60 * 60 * 24;
			for (final Date date : dates.elementSet())
			{
				final long count = dates.count(date);
				final long addMS = dayMS / count;

				for (long i = 0; i < count; i++)
				{
					m_Dates.add(new java.util.Date(date.getTime() + i * addMS));
				}
			}

		}
		return Collections.unmodifiableList(m_Dates);
	}

	/**
	 * Get the opened file path
	 *
	 * @return the opened file path
	 * @since Oct 25, 2016
	 */
	public String getFilePath()
	{
		validate();
		return m_FilePath;
	}

	/**
	 * Get the file header
	 *
	 * @return the file header
	 * @since Oct 25, 2016
	 */
	public GIOHeader getHeader()
	{
		validate();
		return m_Header;
	}

	/**
	 * Reads the header from the file, initializing the {@link #m_DIS},
	 * {@link #m_Header} fields and retrieving the variable names, variable
	 * units, segment names, min/max over variables, and min/max over variable
	 * segments.
	 *
	 * Times are not retrieved (aside from header information).
	 *
	 * @throws IOException
	 *             if the header does not match expectations
	 * @since Oct 25, 2016
	 */
	private void readHeader() throws IOException
	{
		final GIOHeader.Builder headerBuilder = GIOHeader.builder();
		log.debug(String.format("Open %s", m_FilePath));
		m_DIS = new SeekableDataFileInputStreamImpl(m_FilePath, m_ByteOrder);

		try
		{
			/**
			 * Read header. Do not modify the order that these local variables
			 * are declared as the data read process is order-dependent.
			 */
			final String title = new String(
					m_DIS.readCharsAsAscii(GIOHeader.GRID_TITLE_LENGTH)).trim();

			final int numRows = m_DIS.readInt();
			final int numNodes = m_DIS.readInt();
			final float sizeX = m_DIS.readFloat();
			final float sizeY = m_DIS.readFloat();

			try
			{
				m_Header = headerBuilder.withTitle(title).withNumRows(numRows)
						.withNumNodes(numNodes).withSize(sizeX, sizeY).build();
				log.debug(m_Header);
			}
			catch (final Exception e)
			{
				final String message = "Unable to read header from file: "
						+ m_FilePath;
				throw new IOException(message, e);
			}

			final List<Integer> config = IntStream.range(0, numRows * 3)
					.map(this::readInt).boxed().collect(Collectors.toList());

			/**
			 * There might be "junk" data in this config. Adjust for this
			 * possibility with some simple sanity checks.
			 */
			for (int row = 0; row < numRows - 1; row++)
			{
				final int xstart = config.get(row);
				final int xend = config.get(row + numRows);
				final int sumPrev = config.get(row + numRows * 2);
				final int sum = config.get(row + numRows * 2 + 1);
				if (xstart > xend || xend > numNodes)
				{
					config.remove(row + numRows);
					config.add(readInt());
					row--;
				}
				else if (sum != sumPrev + xend - xstart + 1)
				{
					config.remove(row + numRows * 2);
					config.add(readInt());
					row--;
				}
			}

			for (int row = 0; row < numRows; row++)
			{
				final int xstart = config.get(row);
				final int xend = config.get(row + numRows);
				m_AvailabilityMap.put(row, Range.closed(xstart, xend));
			}

			int row = 0;
			IntStream.range(numRows * row, numRows * (row + 1))
					.forEach(x -> System.out
							.print(String.format("%4d ", config.get(x))));
			System.out.println();
			row++;
			IntStream.range(numRows * row, numRows * (row + 1))
					.forEach(x -> System.out
							.print(String.format("%4d ", config.get(x))));
			System.out.println();
			row++;
			IntStream.range(numRows * row, numRows * (row + 1))
					.forEach(x -> System.out
							.print(String.format("%4d ", config.get(x))));
			System.out.println();

			m_GridSize = GRID_TAG_LENGTH + numNodes * Float.BYTES;
			m_GridStartByte = m_DIS.getPosition();
			/**
			 * Find the start of the first tag. Should be pretty close...
			 */
			for (int i = 0; i < 4; i++)
			{
				if (i > 0)
				{
					m_DIS.seek((int) m_GridStartByte);
				}
				final String tag = new String(
						m_DIS.readCharsAsAscii(GRID_TAG_LENGTH)).trim();
				try
				{
					m_DateFormat.parse(tag);
					break;
				}
				catch (final ParseException e)
				{
					m_GridStartByte++;
					continue;
				}
			}
		}
		catch (final Throwable t)
		{
			log.error("Error reading file.", t);
			close();
		}
	}

	/**
	 * Reads an intenger from the input stream
	 *
	 * @param p_Anything
	 *            ignore this parameter, used for streams
	 * @return
	 * @since Oct 27, 2016
	 */
	private <T> int readInt(
			@SuppressWarnings("unchecked") final T... p_Anything)
	{
		try
		{
			return m_DIS.readInt();
		}
		catch (final IOException e)
		{
			final String message = String
					.format("Unable to read from input file.");
			log.error(message, e);
			throw new RuntimeException(message, e);
		}
	}

	/**
	 * Validate the reader.
	 *
	 * @throws IllegalStateException
	 *             if the {@link SeekableDataFileInputStream} instance was not
	 *             initialized
	 * @since Oct 25, 2016
	 */
	private void validate() throws IllegalStateException
	{
		checkState(m_DIS != null, "File is not open.");
	}
}
