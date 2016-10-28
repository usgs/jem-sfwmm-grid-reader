package gov.usgs.jem.sfwmm.grid.iosp;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.unidata.io.RandomAccessFile;

/**
 * The SFWMM GridIO header structure. Use {@link #builder()} to construct a new
 * instance.
 *
 * @author mckelvym
 * @since Oct 28, 2016
 *
 */
public final class SFWMMGridIOSP extends AbstractIOServiceProvider
{
	/**
	 * Class logger
	 */
	private static final org.apache.log4j.Logger	log	= org.apache.log4j.Logger
			.getLogger(SFWMMGridIOSP.class);

	/**
	 * The file that was supplied to the constructor.
	 */
	private File									m_File;

	@Override
	public String getFileTypeDescription()
	{
		return SFWMMGridIOSP.class.getSimpleName();
	}

	@Override
	public String getFileTypeId()
	{
		return SFWMMGridIOSP.class.getCanonicalName();
	}

	@Override
	public boolean isValidFile(final RandomAccessFile p_RAF) throws IOException
	{
		if (p_RAF == null)
		{
			return false;
		}

		m_File = new File(p_RAF.getLocation());
		if (!m_File.exists())
		{
			log.error("File does not exist: " + m_File.getAbsolutePath());
			return false;
		}
		if (!p_RAF.getLocation().endsWith(".bin"))
		{
			log.error("Does not end with .bin");
			return false;
		}

		p_RAF.order(ByteOrder.BIG_ENDIAN);
		p_RAF.seek(80);

		final int numRows = p_RAF.readInt();
		if (numRows <= 0 || numRows > 1000)
		{
			log.error(String.format(
					"Does not look like .bin file...num rows read is %s",
					numRows));
		}

		final int numNodes = p_RAF.readInt();
		if (numNodes <= 0 || numNodes > 100000)
		{
			log.error(String.format(
					"Does not look like .bin file...num nodes read is %s",
					numNodes));
		}

		return true;
	}

	@Override
	public Array readData(final Variable p_Variable, final Section p_Section)
			throws IOException, InvalidRangeException
	{
		// TODO
		return null;
	}

}
