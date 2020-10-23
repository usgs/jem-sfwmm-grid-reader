package gov.usgs.jem.sfwmm.grid;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.io.LittleEndianDataInputStream;
import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Package-private implementation of {@link SeekableDataFileInputStream},
 * created by {@link SeekableDataFileInputStreamImpl#open(String, ByteOrder)}
 *
 * @author mckelvym
 * @since Oct 26, 2016
 *
 */
final class SeekableDataFileInputStreamImpl
		implements SeekableDataFileInputStream
{
	/**
	 * Endianness that controls the type of {@link DataInput} initialized
	 *
	 * @since Oct 26, 2016
	 */
	private final ByteOrder		m_ByteOrder;

	/**
	 * Retained so the underlying stream can be closed. Initialized to the same
	 * value as {@link #m_DataInputStream}
	 *
	 * @since Oct 26, 2016
	 */
	private FilterInputStream	m_Closeable;

	/**
	 * Used for reading data. One of {@link DataInputStream} or
	 * {@link LittleEndianDataInputStream}
	 *
	 * @since Oct 26, 2016
	 */
	private DataInput			m_DataInputStream;

	/**
	 * File to read from
	 *
	 * @since Oct 26, 2016
	 */
	private final String		m_FilePath;

	/**
	 * Input stream used to construct the underlying {@link DataInputStream} or
	 * {@link LittleEndianDataInputStream}
	 *
	 * @since Oct 26, 2016
	 */
	private BufferedInputStream	m_InputStream;

	/**
	 * Read position within the stream
	 *
	 * @author mckelvym
	 */
	private long				m_Position;

	/**
	 * Create a new {@link DataInputStream} using the provided file path and
	 * endianness.
	 *
	 * @param p_FilePath
	 *            the file path to read from
	 * @param p_ByteOrder
	 *            the {@link ByteOrder} endianness to use
	 * @throws IOException
	 *             if the stream could not be initialized
	 * @since Oct 26, 2016
	 */
	public SeekableDataFileInputStreamImpl(final String p_FilePath,
			final ByteOrder p_ByteOrder) throws IOException
	{
		m_FilePath = p_FilePath;
		m_ByteOrder = p_ByteOrder;

		initialize(m_FilePath, m_ByteOrder);
	}

	/**
	 * Close the input
	 *
	 * @throws IOException
	 *             an error occurred while attempting to close the reader
	 *
	 * @since Oct 26, 2016
	 */

	@Override
	public void close() throws IOException
	{
		IOException ioE = null;

		m_Position = -1;

		try
		{
			m_InputStream.close();
		}
		catch (final IOException e)
		{
			ioE = e;
		}

		try
		{
			m_Closeable.close();
		}
		catch (final IOException e)
		{
			if (ioE != null)
			{
				ioE.addSuppressed(e);
			}
			else
			{
				ioE = e;
			}
		}

		if (ioE != null)
		{
			throw ioE;
		}
	}

	@SuppressWarnings("resource")
	@Override
	public boolean equals(final Object p_Obj)
	{
		if (this == p_Obj)
		{
			return true;
		}
		if (!(p_Obj instanceof SeekableDataFileInputStreamImpl))
		{
			return false;
		}
		return Objects.equal(m_ByteOrder,
				SeekableDataFileInputStreamImpl.class.cast(p_Obj).m_ByteOrder)
				&& Objects.equal(m_FilePath,
						SeekableDataFileInputStreamImpl.class
								.cast(p_Obj).m_FilePath);
	}

	@Override
	public ByteOrder getByteOrder()
	{
		return m_ByteOrder;
	}

	@Override
	public String getFilePath()
	{
		return m_FilePath;
	}

	@Override
	public long getPosition()
	{
		return m_Position;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(m_ByteOrder, m_FilePath);
	}

	/**
	 * Close any previous streams and initialize new ones from the provided file
	 * path using the provided byte order.
	 *
	 * {@link #m_InputStream}, {@link #m_DataInputStream}, and
	 * {@link #m_Closeable} will be set.
	 *
	 * @param p_FilePath
	 *            the file path to open
	 * @param p_ByteOrder
	 *            the endianness to use
	 * @throws IOException
	 *             an error occurred while attempting to initialize input
	 *             stream(s)
	 * @since Oct 26, 2016
	 */
	private void initialize(final String p_FilePath,
			final ByteOrder p_ByteOrder) throws IOException
	{
		checkNotNull(p_FilePath, "File path required.");
		checkNotNull(p_ByteOrder, "Byte order required.");
		/**
		 * Close previous streams
		 */
		if (m_InputStream != null)
		{
			close();
		}

		m_InputStream = new BufferedInputStream(
				Files.newInputStream(Paths.get(p_FilePath)));
		if (p_ByteOrder.equals(ByteOrder.BIG_ENDIAN))
		{
			final DataInputStream dIS = new DataInputStream(m_InputStream);
			m_DataInputStream = dIS;
			m_Closeable = dIS;
		}
		else if (p_ByteOrder.equals(ByteOrder.LITTLE_ENDIAN))
		{
			final LittleEndianDataInputStream dIS = new LittleEndianDataInputStream(
					m_InputStream);
			m_DataInputStream = dIS;
			m_Closeable = dIS;
		}
		else
		{
			throw new IllegalArgumentException("Invalid byte order.");
		}
		m_Position = 0;
	}

	@Override
	public boolean readBoolean() throws IOException
	{
		try
		{
			return m_DataInputStream.readBoolean();
		}
		finally
		{
			m_Position += 1;
		}
	}

	@Override
	public byte readByte() throws IOException
	{
		try
		{
			return m_DataInputStream.readByte();
		}
		finally
		{
			m_Position += 1;
		}
	}

	@Override
	public char readChar() throws IOException
	{
		try
		{
			return m_DataInputStream.readChar();
		}
		finally
		{
			m_Position += 2;
		}
	}

	@Override
	public char[] readCharsAsAscii(final int p_Count) throws IOException
	{
		checkArgument(p_Count >= 0, "Invalid count: %s", p_Count);

		final char[] chars = new char[p_Count];
		for (int charNum = 0; charNum < chars.length; charNum++)
		{
			chars[charNum] = (char) readByte();
		}
		return chars;
	}

	@Override
	public double readDouble() throws IOException
	{
		try
		{
			return m_DataInputStream.readDouble();
		}
		finally
		{
			m_Position += 8;
		}
	}

	@Override
	public float readFloat() throws IOException
	{
		try
		{
			return m_DataInputStream.readFloat();
		}
		finally
		{
			m_Position += 4;
		}
	}

	@Override
	public void readFully(final byte[] p_ByteBuffer) throws IOException
	{
		try
		{
			m_DataInputStream.readFully(p_ByteBuffer);
		}
		finally
		{
			m_Position += p_ByteBuffer.length;
		}
	}

	@Override
	public void readFully(final byte[] p_ByteBuffer, final int p_Offset,
			final int p_Length) throws IOException
	{
		try
		{
			m_DataInputStream.readFully(p_ByteBuffer, p_Offset, p_Length);
		}
		finally
		{
			m_Position += p_Length;
		}
	}

	@Override
	public int readInt() throws IOException
	{
		try
		{
			return m_DataInputStream.readInt();
		}
		finally
		{
			m_Position += 4;
		}
	}

	@Override
	public String readLine() throws IOException
	{
		final String line = m_DataInputStream.readLine();
		m_Position += line.getBytes().length;
		return line;
	}

	@Override
	public long readLong() throws IOException
	{
		try
		{
			return m_DataInputStream.readLong();
		}
		finally
		{
			m_Position += 8;
		}
	}

	@Override
	public short readShort() throws IOException
	{
		try
		{
			return m_DataInputStream.readShort();
		}
		finally
		{
			m_Position += 2;
		}
	}

	@Override
	public int readUInt32() throws IOException
	{
		/**
		 * Reads an unsigned integer (4-bytes) into a java signed integer
		 * (4-bytes) from the data input according to the byte order.
		 */
		final byte[] b = new byte[4];
		readFully(b, 0, 4);

		final ByteBuffer byteBuffer = ByteBuffer.wrap(b).order(m_ByteOrder);
		final int uint = byteBuffer.getInt();
		return uint;
	}

	@Override
	public int readUnsignedByte() throws IOException
	{
		try
		{
			return m_DataInputStream.readUnsignedByte();
		}
		finally
		{
			m_Position += 1;
		}
	}

	@Override
	public int readUnsignedShort() throws IOException
	{
		try
		{
			return m_DataInputStream.readUnsignedShort();
		}
		finally
		{
			m_Position += 2;
		}
	}

	@Override
	public String readUTF() throws IOException
	{
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public int seek(final int p_Position) throws IOException
	{
		initialize(m_FilePath, m_ByteOrder);
		final int skipBytes = skipBytes(p_Position);
		return skipBytes;
	}

	@Override
	public int skipBytes(final int p_NumBytes) throws IOException
	{
		final int skipBytes = m_DataInputStream.skipBytes(p_NumBytes);
		m_Position += skipBytes;
		return skipBytes;
	}

	@Override
	public int skipBytesAggressive(final int p_Count) throws IOException
	{
		int skippedBytes = 0;
		do
		{
			final int newlySkippedBytes = skipBytes(p_Count - skippedBytes);
			if (newlySkippedBytes == 0)
			{
				break;
			}
			skippedBytes += newlySkippedBytes;
		}
		while (skippedBytes != p_Count);
		return skippedBytes;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("file", m_FilePath)
				.add("endianness", m_ByteOrder).toString();
	}
}