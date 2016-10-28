package gov.usgs.jem.sfwmm.grid.iosp;

import java.io.IOException;

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
	@Override
	public String getFileTypeDescription()
	{
		// TODO
		return null;
	}

	@Override
	public String getFileTypeId()
	{
		// TODO
		return null;
	}

	@Override
	public boolean isValidFile(final RandomAccessFile p_arg0) throws IOException
	{
		// TODO
		return false;
	}

	@Override
	public Array readData(final Variable p_arg0, final Section p_arg1)
			throws IOException, InvalidRangeException
	{
		// TODO
		return null;
	}

}
