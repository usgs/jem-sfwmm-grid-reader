package gov.usgs.jem.netcdf.iosp.sfwmm.grid;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import gov.usgs.jem.sfwmm.grid.GIOReader;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

/**
 * The SFWMM GridIO IO service provider implementation to have this class be
 * read using the UCAR NetCDF CDM (Common Data Model)
 *
 * @author mckelvym
 * @since Oct 28, 2016
 *
 */
public final class SFWMMGridIOSP extends AbstractIOServiceProvider
{
	/**
	 * @since Nov 4, 2016
	 */
	static final String								AXIS					= "axis";

	/**
	 * Used in helping determine output date indices
	 *
	 * @since Nov 2, 2016
	 */
	private static final Calendar					CALENDAR;

	/**
	 * Grid cell size, horizontal or vertical
	 *
	 * @since Oct 31, 2016
	 */
	private static float							CELL_SIZE_M				= 3218.69f;

	/**
	 * @since Nov 4, 2016
	 */
	static final String								CHUNK_SIZES				= "_ChunkSizes";

	/**
	 * @since Nov 4, 2016
	 */
	static final String								COORDINATE_AXIS_TYPE	= "_CoordinateAxisType";

	/**
	 * The type of data provided by this reader.
	 *
	 * @since Nov 4, 2016
	 */
	private static final DataType					DATA_TYPE;

	/**
	 * Used to format reference date string
	 *
	 * @since Nov 2, 2016
	 */
	private static final SimpleDateFormat			DATE_FORMATTER;

	/**
	 * Class logger
	 */
	private static final org.apache.log4j.Logger	log						= org.apache.log4j.Logger
			.getLogger(SFWMMGridIOSP.class);

	/**
	 * @since Nov 4, 2016
	 */
	static final String								LONG_NAME				= "long_name";

	/**
	 * @since Nov 4, 2016
	 */
	static final String								STANDARD_NAME			= "standard_name";

	/**
	 * The time variable name
	 *
	 * @since Oct 28, 2016
	 */
	static final String								TIME_VAR_NAME			= "time";

	/**
	 * @since Nov 4, 2016
	 */
	static final String								UNITS					= "units";

	/**
	 * The horizontal coordinate variable name
	 *
	 * @since Nov 2, 2016
	 */
	static final String								X_VAR_NAME				= "x";

	/**
	 * The vertical coordinate variable name
	 *
	 * @since Nov 2, 2016
	 */
	static final String								Y_VAR_NAME				= "y";

	static
	{
		final TimeZone timeZone = TimeZone.getTimeZone("UTC");
		DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");
		DATE_FORMATTER.setTimeZone(timeZone);
		CALENDAR = Calendar.getInstance();
		CALENDAR.setTimeZone(timeZone);
		DATA_TYPE = DataType.FLOAT;
	}

	/**
	 * Takes in a Date List and a timestep and returns an int array that can be
	 * written to a NetCDF file as time variable data.
	 *
	 * @param p_Dates
	 *            List of Date objects
	 * @param p_ChronoUnit
	 *            The {@link ChronoUnit} to use when parsing date indices to
	 *            dates
	 * @return Array of int containing offsets from first date in p_Dates
	 */
	private static int[] getDateIndexes(final List<Date> p_Dates,
			final ChronoUnit p_ChronoUnit)
	{
		checkNotNull(p_Dates, "Dates cannot be null.");

		final int[] indexes = new int[p_Dates.size()];
		indexes[0] = 0;
		final Instant refInstant = p_Dates.get(0).toInstant();
		try
		{
			for (int i = 1; i < indexes.length; i++)
			{
				final Instant toInstant = p_Dates.get(i).toInstant();
				final Long between = p_ChronoUnit.between(refInstant,
						toInstant);
				indexes[i] = between.intValue();
			}
		}
		catch (final UnsupportedTemporalTypeException e)
		{
			final Calendar cal = CALENDAR;
			cal.setTime(p_Dates.get(0));
			switch (p_ChronoUnit)
			{
				case MONTHS:
				{
					final int ref = cal.get(Calendar.YEAR) * 12
							+ cal.get(Calendar.MONTH);
					for (int i = 1; i < indexes.length; i++)
					{
						cal.setTime(p_Dates.get(i));
						final int now = cal.get(Calendar.YEAR) * 12
								+ cal.get(Calendar.MONTH);
						indexes[i] = now - ref;
					}
				}
					break;
				case YEARS:
				{
					final int ref = cal.get(Calendar.YEAR);
					for (int i = 1; i < indexes.length; i++)
					{
						cal.setTime(p_Dates.get(i));
						final int now = cal.get(Calendar.YEAR);
						indexes[i] = now - ref;
					}
				}
					break;
				default:
					throw e;
			}
		}
		return indexes;
	}

	/**
	 * Determine the preferred {@link ChronoUnit} to use based on the minimum
	 * span of seconds between sorted date entries
	 *
	 * @param p_Dates
	 *            a collection of dates
	 * @return the {@link ChronoUnit} to use
	 * @since Oct 31, 2016
	 */
	private static ChronoUnit getTimeStep(final Collection<Date> p_Dates)
	{
		final SortedSet<Date> dates = Sets.newTreeSet(p_Dates);

		long numSeconds = Long.MAX_VALUE;
		Date ref = dates.first();
		final Instant refInstant = ref.toInstant();
		/**
		 * Determine the minimum number of seconds between consecutive
		 * timestamps
		 */
		boolean isFirst = true;
		for (final Date date : dates)
		{
			if (isFirst)
			{
				isFirst = false;
				continue;
			}
			numSeconds = Math.min(Math.abs(
					ChronoUnit.SECONDS.between(refInstant, date.toInstant())),
					numSeconds);
			ref = date;
		}

		final int second = 1;
		final int minute = 60 * second;
		final int hour = 60 * minute;
		final int day = 24 * hour;
		final int month = 28 * day;
		final int year = 12 * month;

		if (numSeconds < minute)
		{
			return ChronoUnit.SECONDS;
		}
		else if (numSeconds < hour)
		{
			return ChronoUnit.MINUTES;
		}
		else if (numSeconds < day - hour)
		{
			return ChronoUnit.HOURS;
		}
		else if (numSeconds < month)
		{
			return ChronoUnit.DAYS;
		}
		else if (numSeconds < year)
		{
			return ChronoUnit.MONTHS;
		}
		else
		{
			return ChronoUnit.YEARS;
		}
	}

	/**
	 * The data variable name.
	 *
	 * @since Oct 28, 2016
	 */
	private String					m_DataVariableName;

	/**
	 * The file that was supplied to the constructor.
	 */
	private File					m_File;

	/**
	 * The fill value
	 */
	private Number					m_NoDataValue;

	/**
	 * @see GIOReader
	 * @since Oct 28, 2016
	 */
	private GIOReader				m_Reader;

	/**
	 * Num dates
	 *
	 * @since Oct 31, 2016
	 */
	private int						m_SizeT;

	/**
	 * Grid width
	 *
	 * @since Oct 31, 2016
	 */
	private int						m_SizeX;

	/**
	 * Grid height
	 *
	 * @since Oct 31, 2016
	 */
	private int						m_SizeY;

	/**
	 * 't', 'y', and 'x'
	 *
	 * @since Oct 28, 2016
	 */
	private final List<Variable>	m_SupportingVariables;

	/**
	 * Time step used
	 *
	 * @since Nov 2, 2016
	 */
	private ChronoUnit				m_TimeStep;

	{
		m_SupportingVariables = Lists.newArrayList();
		m_DataVariableName = null;
	}

	/**
	 * @since Oct 28, 2016
	 */
	public SFWMMGridIOSP()
	{
		/** Nothing for now */
	}

	@Override
	public void close() throws IOException
	{
		if (raf != null)
		{
			raf.close();
		}
		raf = null;

		if (m_Reader != null)
		{
			m_Reader.close();
		}
		m_Reader = null;

		m_SupportingVariables.clear();
		super.close();
	}

	/**
	 * Create an array for the t coordinate values
	 *
	 * @return the new array
	 * @throws IOException
	 *             if problem reading from file
	 * @throws ParseException
	 *             if unable to parse date
	 * @since Oct 31, 2016
	 */
	private Array createTArray() throws ParseException, IOException
	{
		final int[] dateIndexes = getDateIndexes(m_Reader.getDates(),
				m_TimeStep);
		final Array cacheData = Array.factory(DataType.INT,
				new int[] { m_SizeT }, dateIndexes);
		return cacheData;
	}

	/**
	 * Create an array for the x coordinate values
	 *
	 * @return the new array
	 * @since Oct 31, 2016
	 */
	private Array createXArray()
	{
		/**
		 * south and west bounding coordinates (25.133890, -81.330940 lat-lon)
		 *
		 * 466641.10, 2779814.25
		 */
		Array cacheData;
		/**
		 * Build coords list - x
		 */
		cacheData = Array.factory(DataType.DOUBLE, new int[] { m_SizeX });
		final double refX = 466641.10;
		IntStream.range(0, m_SizeX)
				.forEach(i -> cacheData.setObject(i, refX + i * CELL_SIZE_M));

		return cacheData;
	}

	/**
	 * Create an array for the y coordinate values
	 *
	 * @return the new array
	 * @since Oct 31, 2016
	 */
	private Array createYArray()
	{
		/**
		 * south and west bounding coordinates (25.133890, -81.330940 lat-lon)
		 *
		 * 466641.10, 2779814.25
		 */
		Array cacheData;
		/**
		 * Build coords list - y, reverse order
		 */
		cacheData = Array.factory(DataType.DOUBLE, new int[] { m_SizeY });
		final double refY = 2779814.25 + (m_SizeY - 1) * CELL_SIZE_M
				+ 0.5 * CELL_SIZE_M;
		IntStream.range(0, m_SizeY)
				.forEach(i -> cacheData.setObject(i, refY - i * CELL_SIZE_M));

		return cacheData;
	}

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

	@SuppressWarnings("deprecation")
	@Override
	public void open(final RandomAccessFile p_RAF,
			final NetcdfFile p_NetcdfFile, final CancelTask p_CancelTask)
			throws IOException
	{
		super.open(p_RAF, p_NetcdfFile, p_CancelTask);

		m_File = new File(p_RAF.getLocation());
		m_Reader = GIOReader.open(m_File.getAbsolutePath());
		if (m_Reader == null)
		{
			final String message = String
					.format("Unable to retrieve reader for file %s", m_File);
			log.error(message);
			close();
			throw new IOException(message);
		}

		/**
		 * Find no data value, determine class type.
		 */
		m_NoDataValue = m_Reader.getNoDataValue();
		List<Date> dates = null;
		try
		{
			dates = m_Reader.getDates();
		}
		catch (final ParseException e)
		{
			final String message = String
					.format("Unable to parse dates from file %s", m_File);
			log.error(message, e);
			close();
			throw new IOException(message);
		}
		m_SizeT = dates.size();
		m_TimeStep = getTimeStep(dates);
		if (m_TimeStep.equals(ChronoUnit.MONTHS)
				|| m_TimeStep.equals(ChronoUnit.YEARS))
		{
			m_TimeStep = ChronoUnit.DAYS;
		}

		Variable tVariable = null;
		try
		{
			Dimension tDimension = new Dimension(TIME_VAR_NAME, dates.size());
			tDimension = ncfile.addDimension(null, tDimension);
			tVariable = ncfile.addVariable(null, tDimension.getShortName(),
					DataType.INT, tDimension.getShortName());
			ncfile.addVariableAttribute(tVariable,
					new Attribute(LONG_NAME, "time step"));
			ncfile.addVariableAttribute(tVariable,
					new Attribute(COORDINATE_AXIS_TYPE, "Time"));
			ncfile.addVariableAttribute(tVariable, new Attribute(AXIS, "t"));
			final Date refDate = dates.get(0);
			ncfile.addVariableAttribute(tVariable,
					new Attribute(UNITS, String.format("%s since %s",
							m_TimeStep.toString().toLowerCase(), DATE_FORMATTER
									.format(refDate.getTime()).toString())));
			ncfile.addVariableAttribute(tVariable, new Attribute(CHUNK_SIZES,
					Lists.newArrayList(tDimension.getLength())));
		}
		catch (final Exception e)
		{
			final String message = "Unable to create t coordinate variable.";
			log.error(message, e);
			close();
			throw new IOException(message, e);
		}
		m_SupportingVariables.add(tVariable);

		final String coordinateUnitString = "m";

		m_SizeY = m_Reader.getHeader().getRowsSize();
		Variable yVariable = null;
		try
		{
			final Dimension yDimension = ncfile.addDimension(null,
					new Dimension(Y_VAR_NAME, m_SizeY));
			yVariable = ncfile.addVariable(null, yDimension.getShortName(),
					DataType.DOUBLE, yDimension.getShortName());
			ncfile.addVariableAttribute(yVariable,
					new Attribute(LONG_NAME, "y coordinate of projection"));
			ncfile.addVariableAttribute(yVariable,
					new Attribute(STANDARD_NAME, "projection_y_coordinate"));
			ncfile.addVariableAttribute(yVariable,
					new Attribute(COORDINATE_AXIS_TYPE, "GeoY"));
			ncfile.addVariableAttribute(yVariable,
					new Attribute(AXIS, Y_VAR_NAME));
			ncfile.addVariableAttribute(yVariable,
					new Attribute(UNITS, coordinateUnitString));
			ncfile.addVariableAttribute(yVariable, new Attribute(CHUNK_SIZES,
					Lists.newArrayList(yDimension.getLength())));
		}
		catch (final Exception e)
		{
			final String message = "Unable to create y coordinate variable.";
			log.error(message, e);
			close();
			throw new IOException(message, e);
		}
		m_SupportingVariables.add(yVariable);

		m_SizeX = m_Reader.getHeader().getColsSize();
		Variable xVariable = null;
		try
		{
			final Dimension xDimension = ncfile.addDimension(null,
					new Dimension(X_VAR_NAME, m_SizeX));
			xVariable = ncfile.addVariable(null, xDimension.getShortName(),
					DataType.DOUBLE, xDimension.getShortName());
			ncfile.addVariableAttribute(xVariable,
					new Attribute(LONG_NAME, "x coordinate of projection"));
			ncfile.addVariableAttribute(xVariable,
					new Attribute(STANDARD_NAME, "projection_x_coordinate"));
			ncfile.addVariableAttribute(xVariable,
					new Attribute(COORDINATE_AXIS_TYPE, "GeoX"));
			ncfile.addVariableAttribute(xVariable,
					new Attribute(AXIS, X_VAR_NAME));
			ncfile.addVariableAttribute(xVariable,
					new Attribute(UNITS, coordinateUnitString));
			ncfile.addVariableAttribute(xVariable, new Attribute(CHUNK_SIZES,
					Lists.newArrayList(xDimension.getLength())));
		}
		catch (final Exception e)
		{
			final String message = "Unable to create x coordinate variable.";
			log.error(message, e);
			close();
			throw new IOException(message, e);
		}
		m_SupportingVariables.add(xVariable);

		m_DataVariableName = Files.getNameWithoutExtension(m_File.getName())
				.replace(".", "");

		/**
		 * A CRS variable is created with respect to the coordinate reference
		 * system and uses the units provided.
		 */
		Variable crsVar;
		try
		{
			final String projVariableName = "transverse_mercator";
			crsVar = ncfile.addVariable(null, projVariableName, DataType.INT,
					"");
			ncfile.addVariableAttribute(crsVar,
					new Attribute("_CoordinateAxisTypes", "GeoY GeoX"));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("grid_mapping_name", projVariableName));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("longitude_of_central_meridian", -81.0));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("latitude_of_projection_origin", 0.0));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("scale_factor_at_central_meridian", 0.9996));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("earth_radius", 6371229.0));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("false_easting", 500000.0));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("false_northing", 0.0));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("semi_major_axis", 6378137.0));
			ncfile.addVariableAttribute(crsVar,
					new Attribute("semi_minor_axis", 6356752.314140356));
			crsVar.setCachedData(
					Array.factory(crsVar.getDataType(), new int[0]));
		}
		catch (final Exception e)
		{
			final String message = String
					.format("Unable to create CRS variable.");
			log.error(message, e);
			close();
			throw new IOException(message, e);
		}

		final String dataVarLongName = m_DataVariableName;
		final String dataVarUnits = "";
		Variable dataVariable = null;
		try
		{
			final String dimNames = m_SupportingVariables.stream()
					.map(Variable::getDimensionsString)
					.collect(Collectors.joining(" "));

			dataVariable = ncfile.addVariable(null, m_DataVariableName,
					DATA_TYPE, dimNames);
			ncfile.addVariableAttribute(dataVariable,
					new Attribute(LONG_NAME, dataVarLongName));
			ncfile.addVariableAttribute(dataVariable,
					new Attribute(UNITS, dataVarUnits));
			ncfile.addVariableAttribute(dataVariable,
					new Attribute("coordinates", dimNames));
			ncfile.addVariableAttribute(dataVariable,
					new Attribute("_FillValue", m_NoDataValue));

			/**
			 * assignCRSToDataVariable
			 */
			final String wkt = "PROJCS[\"NAD83 / UTM zone 17N\",   GEOGCS[\"NAD83\",     DATUM[\"North American Datum 1983\",       SPHEROID[\"GRS 1980\", 6378137.0, 298.257222101, AUTHORITY[\"EPSG\",\"7019\"]],       TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],       AUTHORITY[\"EPSG\",\"6269\"]],     PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]],     UNIT[\"degree\", 0.017453292519943295],     AXIS[\"Geodetic longitude\", EAST],     AXIS[\"Geodetic latitude\", NORTH],     AUTHORITY[\"EPSG\",\"4269\"]],   PROJECTION[\"Transverse_Mercator\", AUTHORITY[\"EPSG\",\"9807\"]],   PARAMETER[\"central_meridian\", -81.0],   PARAMETER[\"latitude_of_origin\", 0.0],   PARAMETER[\"scale_factor\", 0.9996],   PARAMETER[\"false_easting\", 500000.0],   PARAMETER[\"false_northing\", 0.0],   UNIT[\"m\", 1.0],   AXIS[\"Easting\", EAST],   AXIS[\"Northing\", NORTH],   AUTHORITY[\"EPSG\",\"26917\"]]";
			dataVariable.addAttribute(new Attribute("esri_pe_string", wkt));
			dataVariable.addAttribute(
					new Attribute("grid_mapping", crsVar.getShortName()));
		}
		catch (final Exception e)
		{
			final String message = "Unable to create data variable.";
			log.error(message, e);
			close();
			throw new IOException(message, e);
		}

		/**
		 * Add common global attributes such as the conventions used, time that
		 * the file was created, the source and application name, the
		 * institution creating the file, and the author.
		 */
		ncfile.addAttribute(null, new Attribute("Metadata_Conventions",
				"Unidata Dataset Discovery v1.0"));
		ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.6"));
		ncfile.addAttribute(null, new Attribute("cerp_version", "1.2"));
		final Date now = new Date();
		final Date lastModified = new Date(new File(location).lastModified());
		ncfile.addAttribute(null,
				new Attribute("history", String.format("Created %s; %s %s", now,
						m_File.getName(), lastModified)));
		ncfile.addAttribute(null,
				new Attribute("source", getClass().getCanonicalName()));
		ncfile.addAttribute(null, new Attribute("comment", String
				.format("JEM NetCDF %s v1.0", getClass().getSimpleName())));
		final String user = MoreObjects
				.firstNonNull(System.getProperty("user.name"), "Unknown User");
		String computerName = "Unknown Computer";
		try
		{
			computerName = MoreObjects.firstNonNull(
					InetAddress.getLocalHost().getHostName(), computerName);
		}
		catch (final UnknownHostException e)
		{
			/**
			 * Ignore
			 */
			checkNotNull(e);
		}
		ncfile.addAttribute(null, new Attribute("author",
				String.format("%s on %s", user, computerName)));

		ncfile.finish();

	}

	@SuppressWarnings("deprecation")
	@Override
	public Array readData(final Variable p_Variable, final Section p_Section)
			throws IOException, InvalidRangeException
	{
		final boolean hasCachedData = p_Variable.hasCachedData();
		if (!hasCachedData)
		{
			final String shortName = p_Variable.getShortName();
			if (m_SupportingVariables.contains(p_Variable))
			{
				Array cacheData = null;
				if (shortName.equals(Y_VAR_NAME))
				{
					cacheData = createYArray();
				}
				else if (shortName.equals(X_VAR_NAME))
				{
					cacheData = createXArray();
				}
				else if (shortName.equals(TIME_VAR_NAME))
				{
					try
					{
						cacheData = createTArray();
					}
					catch (final ParseException e)
					{
						log.error(
								String.format("%s: %s", p_Variable, p_Section),
								e);
						close();
						throw new IOException(e);
					}
				}
				else
				{
					final String message = String.format("Unknown variable! %s",
							shortName);
					log.error(message);
					close();
					throw new IOException(message);
				}
				p_Variable.setCachedData(cacheData);
			}
			/**
			 * Data variable
			 */
			else if (m_DataVariableName.equals(shortName))
			{
				Section section = new Section(
						new int[] { m_SizeT, m_SizeY, m_SizeX });
				if (p_Section != null)
				{
					final int expectedRank = 3;
					checkArgument(p_Section.getRank() == expectedRank,
							"Invalid section rank. Expected %s but found %s.",
							expectedRank, p_Section.getRank());
					section = p_Section;
				}

				final int tOrigin = section.getOrigin(0);
				final int yOrigin = section.getOrigin(1);
				final int xOrigin = section.getOrigin(2);
				final int tSize = section.getShape(0);
				final int ySize = section.getShape(1);
				final int xSize = section.getShape(2);

				try
				{
					final Range<Integer> tRange = Range.closedOpen(tOrigin,
							tOrigin + tSize);
					final Range<Integer> yRange = Range.closedOpen(yOrigin,
							yOrigin + ySize);
					final Range<Integer> xRange = Range.closedOpen(xOrigin,
							xOrigin + xSize);
					final float[] readData = m_Reader.readData(tRange, yRange,
							xRange);
					final Array data = Array.factory(DATA_TYPE,
							section.getShape(), readData);
					return data.flip(1);
				}
				catch (final ParseException e)
				{
					log.error(String.format("%s: %s", p_Variable, p_Section),
							e);
					close();
					throw new IOException(e);
				}
			}
		}

		try
		{
			return p_Variable.read(p_Section);
		}
		catch (final Throwable t)
		{
			log.error(String.format("%s: %s", p_Variable, p_Section), t);
			close();
			throw t;
		}
	}

}
