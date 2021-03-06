package gov.usgs.jem.sfwmm.grid;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;
import com.google.common.collect.BoundType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.io.Files;
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
	 * Formats the provided input range in such a way that it has closed bounds
	 * and those bounds obey the provided minimum and maximum parameters.
	 *
	 * @param p_InputRange
	 *            the user-supplied, possibly unbounded or "out-of-bounds" range
	 * @param p_Min
	 *            the minimum value to enforce
	 * @param p_Max
	 *            the maximum value to enforce
	 * @return a range that respects the provided minimum and maximum arguments
	 * @since Oct 28, 2016
	 */
	private static Range<Integer> formatRange(final Range<Integer> p_InputRange,
			final int p_Min, final int p_Max)
	{
		int min = p_Min;
		if (p_InputRange.hasLowerBound())
		{
			if (p_InputRange.lowerBoundType().equals(BoundType.CLOSED))
			{
				min = Math.max(p_Min, p_InputRange.lowerEndpoint());
			}
			else
			{
				min = Math.max(p_Min, p_InputRange.lowerEndpoint() + 1);
			}
		}

		int max = p_Max;
		if (p_InputRange.hasUpperBound())
		{
			if (p_InputRange.upperBoundType().equals(BoundType.CLOSED))
			{
				max = Math.min(p_Max, p_InputRange.upperEndpoint());
			}
			else
			{
				max = Math.max(p_Max, p_InputRange.upperEndpoint() - 1);
			}
		}

		return Range.closed(min, max);
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
	 * Get the no data value used by the dataset
	 *
	 * @return the no data value used
	 * @since Oct 31, 2016
	 */
	@SuppressWarnings("static-method")
	public Float getNoDataValue()
	{
		return Float.NaN;
	}

	/**
	 * Read data from the file. If provided parameters are out of bounds, they
	 * are silently corrected to be within the bounds of the dataset. This is to
	 * facilitate the "all" range and unbounded ranges as valid arguments.
	 *
	 * @param p_DateIndices
	 *            the range (could be open-ended) of date indices to read from
	 * @param p_RowIndices
	 *            the range of rows to read from
	 * @param p_ColumnIndices
	 *            the range of columns to read from
	 * @return a list of tables, one for each date index corresponding to the
	 *         input parameters, where each table maps row, column to
	 *         corresponding float value. If there is not a corresponding valid
	 *         value in the dataset, then NaN is used in its place.
	 * @throws IOException
	 *             problem reading from file
	 * @throws ParseException
	 *             problem parsing dates
	 * @since Oct 28, 2016
	 */
	public float[] readData(final Range<Integer> p_DateIndices,
			final Range<Integer> p_RowIndices,
			final Range<Integer> p_ColumnIndices)
			throws ParseException, IOException
	{
		java.util.Objects.requireNonNull(p_DateIndices,
				"Date index range required.");
		java.util.Objects.requireNonNull(p_RowIndices,
				"Row index range required.");
		java.util.Objects.requireNonNull(p_ColumnIndices,
				"Column index range required.");

		/**
		 * getDates will ensure tags are read first
		 */
		final List<Date> tsteps = getDates();
		final GIOHeader header = getHeader();
		final Range<Integer> dateIndices = formatRange(p_DateIndices, 0,
				tsteps.size() - 1);
		final int rowsSize = header.getRowsSize();
		final Range<Integer> rows = formatRange(p_RowIndices, 0, rowsSize - 1);
		final int colsSize = header.getColsSize();
		final Range<Integer> cols = formatRange(p_ColumnIndices, 0,
				colsSize - 1);

		final int thisTSteps = dateIndices.upperEndpoint()
				- dateIndices.lowerEndpoint() + 1;
		final int thisRows = rows.upperEndpoint() - rows.lowerEndpoint() + 1;
		final int thisCols = cols.upperEndpoint() - cols.lowerEndpoint() + 1;
		final float[] data = new float[thisTSteps * thisRows * thisCols];

		/**
		 * Skip to the start of data for the specific tstep
		 */
		m_DIS.seek((int) (m_GridStartByte + GRID_TAG_LENGTH
				+ m_GridSize * dateIndices.lowerEndpoint()));

		int index = 0;
		for (int tstep = 0; tstep < thisTSteps; tstep++)
		{
			/**
			 * If the lower endpoint is not the first row, we need to move the
			 * read pointer ahead in the file accordingly.
			 */
			if (rows.lowerEndpoint() > 0)
			{
				for (Integer row = 0; row < rows.lowerEndpoint(); row++)
				{
					/**
					 * Determine how many nodes really need to be skipped.
					 */
					final Range<Integer> availableCols = m_AvailabilityMap
							.get(row);
					final int skipNodes = availableCols.upperEndpoint()
							- availableCols.lowerEndpoint() + 1;
					m_DIS.skipBytes(Float.BYTES * skipNodes);
				}
			}

			for (int rIndex = 0; rIndex < thisRows; rIndex++)
			{
				final Integer row = rIndex + rows.lowerEndpoint();
				final Range<Integer> availableCols = m_AvailabilityMap.get(row);

				/**
				 * If the lower endpoint is after the start of data for this
				 * row, we need to move the read pointer ahead in the file
				 * accordingly.
				 */
				if (cols.lowerEndpoint() > availableCols.lowerEndpoint())
				{
					final int skipNodes = cols.lowerEndpoint()
							- availableCols.lowerEndpoint();
					m_DIS.skipBytes(Float.BYTES * skipNodes);
				}

				for (int cIndex = 0; cIndex < thisCols; cIndex++)
				{
					final Integer col = cIndex + cols.lowerEndpoint();
					final boolean isAvailCol = availableCols.contains(col);

					float value = Float.NaN;
					/**
					 * If this column falls within the range of data in the file
					 * for this row, read it. Otherwise just use NaN
					 */
					if (isAvailCol)
					{
						value = m_DIS.readFloat();
					}
					data[index++] = value;
				}

				/**
				 * If the upper endpoint is before the end of data for this row,
				 * we need to move the read pointer ahead in the file
				 * accordingly.
				 */
				if (cols.upperEndpoint() < availableCols.upperEndpoint())
				{
					final int skipNodes = availableCols.upperEndpoint()
							- cols.upperEndpoint();
					m_DIS.skipBytes(Float.BYTES * skipNodes);
				}
			}

			/**
			 * If the upper endpoint is not the last row, we need to move the
			 * read pointer ahead in the file accordingly.
			 */
			if (rows.upperEndpoint() < rowsSize - 1)
			{
				for (Integer row = rows.upperEndpoint()
						+ 1; row < rowsSize; row++)
				{
					/**
					 * Determine how many nodes really need to be skipped.
					 */
					final Range<Integer> availableCols = m_AvailabilityMap
							.get(row);
					final int skipNodes = availableCols.upperEndpoint()
							- availableCols.lowerEndpoint() + 1;
					m_DIS.skipBytes(Float.BYTES * skipNodes);
				}
			}

			m_DIS.skipBytes(GRID_TAG_LENGTH);
		}
		return data;
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

			int numCols = 0;
			for (int row = 0; row < numRows; row++)
			{
				final int xstart = config.get(row);
				final int xend = config.get(row + numRows);
				m_AvailabilityMap.put(row, Range.closed(xstart, xend));
				numCols = Math.max(numCols, xend + 1);
			}

			try
			{
				m_Header = headerBuilder.withTitle(title).withNumRows(numRows)
						.withNumCols(numCols).withNumNodes(numNodes)
						.withSize(sizeX, sizeY).build();
				log.debug(m_Header);
			}
			catch (final Exception e)
			{
				final String message = "Unable to read header from file: "
						+ m_FilePath;
				throw new IOException(message, e);
			}

			/**
			 * Debugging information
			 */
			int row = 0;
			final String starts = IntStream
					.range(numRows * row, numRows * (row + 1))
					.mapToObj(config::get).map(x -> String.format("%4d", x))
					.collect(Collectors.joining(" "));
			row++;
			final String ends = IntStream
					.range(numRows * row, numRows * (row + 1))
					.mapToObj(config::get).map(x -> String.format("%4d", x))
					.collect(Collectors.joining(" "));
			row++;
			final String sums = IntStream
					.range(numRows * row, numRows * (row + 1))
					.mapToObj(config::get).map(x -> String.format("%4d", x))
					.collect(Collectors.joining(" "));
			log.debug(String.format("xStarts: %s", starts));
			log.debug(String.format("xEnds:   %s", ends));
			log.debug(String.format("nSums:   %s", sums));

			m_GridSize = GRID_TAG_LENGTH + numNodes * Float.BYTES;
			m_GridStartByte = m_DIS.getPosition();
			/**
			 * Find the start of the first tag. Should be pretty close...
			 */
			for (int i = 0; i < 4; i++)
			{
				if (i > 0)
				{
					m_DIS.skipBytes(1);
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
					checkNotNull(e);
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
	 * @return reads an integer from the {@link #m_DIS}
	 * @param <T>
	 *            any object type, parameter added so that function reference
	 *            can be used for streams
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
