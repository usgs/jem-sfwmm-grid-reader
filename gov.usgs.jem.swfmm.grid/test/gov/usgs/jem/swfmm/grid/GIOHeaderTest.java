package gov.usgs.jem.swfmm.grid;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Tests {@link GIOHeader}
 *
 * @author mckelvym
 * @since Oct 25, 2016
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GIOHeaderTest
{

	private static final Random r;

	static
	{
		r = new Random(System.currentTimeMillis());
	}

	private static void expectFailure(final GIOHeader.Builder p_Builder)
	{
		try
		{
			p_Builder.build();
			Assert.fail("Not trapping bad input.");
		}
		catch (final Throwable t)
		{
			/**
			 * Expected
			 */
		}
	}

	/**
	 * @return the next random unsigned int value
	 * @since Oct 25, 2016
	 */
	private static int nextUInt()
	{
		int v = r.nextInt();
		while (v < 0)
		{
			v = r.nextInt();
		}
		return v;
	}

	/**
	 * @return the next random unsigned long value
	 * @since Oct 25, 2016
	 */
	private static long nextULong()
	{
		long v = r.nextLong();
		while (v < 0)
		{
			v = r.nextLong();
		}
		return v;
	}

	/**
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 25, 2016
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		final Class<?> classToTest = GIOHeader.class;
		final Class<?> testingClass = GIOHeaderTest.class;
		AllTests.assertHasRequiredMethods(classToTest, testingClass);
	}

	private GIOHeader.Builder m_Builder;

	/**
	 * @throws java.lang.Exception
	 *             if unexpected condition causing test failure
	 * @since Oct 25, 2016
	 */
	@Before
	public void setUp() throws Exception
	{
		m_Builder = GIOHeader.builder().withTitle("TITLE").withNumRows(1)
				.withNumNodes(1).withSize(2.0f, 2.0f);
	}

	/**
	 * Test method for {@link gov.usgs.jem.swfmm.grid.GIOHeader#builder()}.
	 *
	 * @throws Exception
	 */
	@Test
	public final void testBuilder() throws Exception
	{
		Assert.assertNotNull(GIOHeader.builder());
		Assert.assertNotNull(m_Builder.build());
	}

	/**
	 * Test method for {@link gov.usgs.jem.swfmm.grid.GIOHeader#getNodesSize()}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetNodesSize() throws Exception
	{
		final int v = Math.abs(r.nextInt()) + 1;
		Assert.assertEquals(v,
				m_Builder.withNumNodes(v).build().getNodesSize());

		expectFailure(m_Builder.withNumNodes(0));
	}

	/**
	 * Test method for {@link gov.usgs.jem.swfmm.grid.GIOHeader#getRowsSize()}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetRowsSize() throws Exception
	{
		final int v = Math.abs(r.nextInt()) + 1;
		Assert.assertEquals(v, m_Builder.withNumRows(v).build().getRowsSize());

		expectFailure(m_Builder.withNumRows(0));
	}

	/**
	 * Test method for {@link gov.usgs.jem.swfmm.grid.GIOHeader#getSizeX()}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetSizeX() throws Exception
	{
		final float v = r.nextFloat() + 0.1f;
		Assert.assertEquals(v,
				m_Builder.withSize(v, v + 1.0f).build().getSizeX(),
				Float.MIN_NORMAL);

		expectFailure(m_Builder.withSize(0.0f, v + 1.0f));
	}

	/**
	 * Test method for {@link gov.usgs.jem.swfmm.grid.GIOHeader#getSizeY()}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetSizeY() throws Exception
	{
		final float v = r.nextFloat() + 0.1f;
		Assert.assertEquals(v,
				m_Builder.withSize(v + 1.0f, v).build().getSizeY(),
				Float.MIN_NORMAL);

		expectFailure(m_Builder.withSize(v + 1.0f, 0.0f));
	}

	/**
	 * Test method for {@link gov.usgs.jem.swfmm.grid.GIOHeader#getTitle()}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetTitle() throws Exception
	{
		final String v = "A TITLE";
		Assert.assertEquals(v, m_Builder.withTitle(v).build().getTitle());

		expectFailure(m_Builder.withTitle(null));
	}
}
