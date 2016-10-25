package gov.usgs.jem.swfmm.grid.example;

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;

/**
 * Example using TODO
 *
 * @author mckelvym
 * @since Oct 25, 2016
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

		// try (BMDReader reader = BMDReader
		// .openDebug(AllTests.getTestFile().getAbsolutePath());)
		// {
		// final List<BMDVariable> variables = reader.getVariables();
		// log.info(String.format("Variables (%s): %s", variables.size(),
		// variables.stream().map(BMDVariable::getName)
		// .collect(Collectors.joining(", "))));
		// final List<BMDSegment> segments = reader.getSegments();
		// log.info(String.format("Segments (%s): %s", segments.size(),
		// segments.stream().map(BMDSegment::getName)
		// .collect(Collectors.joining(", "))));
		// final List<BMDTimeStep> timeSteps = reader.getTimeSteps();
		// log.info(String.format("Timesteps (%s): %s to %s", timeSteps.size(),
		// new java.util.Date(timeSteps.get(0).getTime()),
		// new java.util.Date(
		// Iterables.getLast(timeSteps).getTime())));
		//
		// final ConcentrationsQuery query = reader.newConcentrationsQuery()
		// .withAllTimeSteps()
		// .withSegments(Arrays.asList(segments.get(0)))
		// .withVariables(variables.stream()
		// .filter(x -> x.getName().equals("Hydraulic Depth"))
		// .collect(Collectors.toList()));
		//
		// for (final Concentration conc : query.execute())
		// {
		// log.info(String.format("%s, %s, %s: %s",
		// conc.getVariable().getName(),
		// conc.getSegment().getName(),
		// new java.util.Date(conc.getTimeStep().getTime()),
		// conc.getValue()));
		// }
		// }
		// catch (final Throwable t)
		// {
		// t.printStackTrace();
		// }
	}
}
