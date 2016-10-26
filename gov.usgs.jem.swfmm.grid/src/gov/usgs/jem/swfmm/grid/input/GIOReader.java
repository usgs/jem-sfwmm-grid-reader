package gov.usgs.jem.swfmm.grid.input;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;

import org.apache.log4j.Level;

import com.google.common.base.Objects;
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
	 * Class logger
	 */
	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
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
	 * The {@link ByteOrder} to read from the file.
	 *
	 * @since Oct 25, 2016
	 */
	private final ByteOrder				m_ByteOrder;

	/**
	 * The data input stream used to read from the file.
	 *
	 * @since Oct 25, 2016
	 */
	private SeekableDataFileInputStream	m_DIS;

	/**
	 * The path to the SFWMM GridIO file
	 *
	 * @see #getFilePath()
	 * @since Oct 25, 2016
	 */
	private final String				m_FilePath;

	/**
	 * The header for the SFWMM GridIO file
	 *
	 * @see #getHeader()
	 * @since Oct 25, 2016
	 */
	private GIOHeader					m_Header;

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

			final int[] xstart = new int[numRows];
			final int[] xend = new int[numRows];
			final int[] cumNodeCount = new int[numRows];

			for (int i = 0; i < numRows; i++)
			{
				xstart[i] = m_DIS.readInt();
			}
			for (int i = 0; i < numRows; i++)
			{
				xend[i] = m_DIS.readInt();
			}
			for (int i = 0; i < numRows; i++)
			{
				cumNodeCount[i] = m_DIS.readInt();
			}

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
		}
		catch (final Throwable t)
		{
			log.error("Error reading file.", t);
			close();
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
