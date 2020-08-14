package gov.usgs.jem.netcdf.iosp.sfwmm.grid.example;

import gov.usgs.jem.netcdf.iosp.sfwmm.grid.AllTests;
import gov.usgs.jem.netcdf.iosp.sfwmm.grid.SFWMMGridIOSP;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.log4j.BasicConfigurator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

/**
 * Example showing how to convert from the 2x2's .bin format to a NetCDF file
 *
 * @author mckelvym
 * @since Nov 4, 2016
 *
 */
public final class Main
{
	/**
	 * Class logger
	 */
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(Main.class);

	/**
	 * @param args
	 *            unused
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @since Mar 9, 2018
	 */
	@SuppressWarnings({ "deprecation", "resource" })
	public static void main(final String[] args)
			throws IOException, IllegalAccessException, InstantiationException
	{
		BasicConfigurator.configure();

		final String inputFilePath = AllTests.getTestFile().getAbsolutePath();
		final String outputFilePath = inputFilePath + ".nc";

		/**
		 * Register the service provider with the NetCDF library
		 */
		NetcdfFiles.registerIOProvider(SFWMMGridIOSP.class);

		/**
		 * Proceed to open file through CDM as any other NetCDF file
		 */
		try (NetcdfFile nc = NetcdfFiles.open(inputFilePath))
		{
			/**
			 * Copy to 'real' NetCDF
			 */
			final ucar.nc2.FileWriter2 writer = new ucar.nc2.FileWriter2(nc,
					outputFilePath, ucar.nc2.NetcdfFileWriter.Version.netcdf3,
					null);
			writer.write();
		}

		/**
		 * Log CDL
		 */
		try (NetcdfFile nc = NetcdfFiles.open(outputFilePath))
		{
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			nc.writeCDL(out, false);
			log.info(out);
		}
	}
}
