package gov.usgs.jem.sfwmm.grid;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;

/**
 * The SFWMM GridIO header structure. Use {@link #builder()} to construct a new
 * instance.
 *
 * @author mckelvym
 * @since Oct 25, 2016
 *
 */
public final class GIOHeader
{
	/**
	 * Builds new instances of {@link GIOHeader}
	 *
	 * @author mckelvym
	 * @since Oct 25, 2016
	 *
	 */
	public static class Builder
	{
		private int		m_bNumCols;
		private int		m_bNumNodes;
		private int		m_bNumRows;
		private float	m_bSizeX;
		private float	m_bSizeY;
		private String	m_bTitle;

		/**
		 * @since Oct 25, 2016
		 */
		private Builder()
		{
			/** Nothing for now */
		}

		/**
		 * @return {@link GIOHeader} instance
		 * @throws Exception
		 * @since Mar 9, 2018
		 */
		public GIOHeader build() throws Exception
		{
			return new GIOHeader(this);
		}

		/**
		 * Set the number of Cols
		 *
		 * @param p_NumCols
		 *            the number of Cols
		 * @return this builder
		 * @since Oct 28, 2016
		 */
		public Builder withNumCols(final int p_NumCols)
		{
			m_bNumCols = p_NumCols;
			return this;
		}

		/**
		 * Set the number of Nodes
		 *
		 * @param p_NumNodes
		 *            the number of Nodes.
		 * @return this builder
		 * @since Oct 25, 2016
		 */
		public Builder withNumNodes(final int p_NumNodes)
		{
			m_bNumNodes = p_NumNodes;
			return this;
		}

		/**
		 * Set the number of Rows
		 *
		 * @param p_NumRows
		 *            the number of Rows
		 * @return this builder
		 * @since Oct 25, 2016
		 */
		public Builder withNumRows(final int p_NumRows)
		{
			m_bNumRows = p_NumRows;
			return this;
		}

		/**
		 * Set the node size
		 *
		 * @param p_X
		 *            the node horizontal size
		 * @param p_Y
		 *            the node vertical size
		 * @return this builder
		 * @since Oct 25, 2016
		 */
		public Builder withSize(final float p_X, final float p_Y)
		{
			m_bSizeX = p_X;
			m_bSizeY = p_Y;
			return this;
		}

		/**
		 * Set the Title
		 *
		 * @param p_Title
		 *            the Title
		 * @return this builder
		 * @since Oct 25, 2016
		 */
		public Builder withTitle(final String p_Title)
		{
			m_bTitle = p_Title;
			return this;
		}
	}

	/**
	 * The number of bytes used for the grid title
	 *
	 * @since Oct 25, 2016
	 */
	public static final int GRID_TITLE_LENGTH = 80;

	/**
	 * Creates a new, empty builder
	 *
	 * @return a new builder instance.
	 * @since Oct 25, 2016
	 */
	public static Builder builder()
	{
		return new Builder();
	}

	/**
	 * The number of Cols in the file.
	 *
	 * @since Oct 28, 2016
	 */
	private final int		m_NumCols;

	/**
	 * The number of Nodes in the file.
	 *
	 * @since Oct 25, 2016
	 */
	private final int		m_NumNodes;

	/**
	 * The number of Rows in the file.
	 *
	 * @since Oct 25, 2016
	 */
	private final int		m_NumRows;

	/**
	 * @see #getSizeX()
	 * @since Oct 25, 2016
	 */
	private final float		m_SizeX;

	/**
	 * @see #getSizeY()
	 * @since Oct 25, 2016
	 */
	private final float		m_SizeY;

	/**
	 *
	 * /** The Title field.
	 *
	 * @since Oct 25, 2016
	 */
	private final String	m_Title;

	/**
	 * Create a new header instance from the provided builder.
	 *
	 * @param p_Builder
	 *            the {@link Builder} to take initialization from
	 * @since Oct 25, 2016
	 */
	private GIOHeader(final Builder p_Builder)
	{
		m_Title = p_Builder.m_bTitle;
		m_NumRows = p_Builder.m_bNumRows;
		m_NumCols = p_Builder.m_bNumCols;
		m_NumNodes = p_Builder.m_bNumNodes;
		m_SizeX = p_Builder.m_bSizeX;
		m_SizeY = p_Builder.m_bSizeY;

		boolean condition;

		checkNotNull(m_Title, "Title field cannot be null.");
		condition = m_Title.length() <= GRID_TITLE_LENGTH;
		checkArgument(condition,
				"Title field must be less than or equal to %s characters",
				GRID_TITLE_LENGTH);

		condition = m_NumRows > 0;
		checkArgument(condition,
				"Number of rows must be greater than 0, but is %s", m_NumRows);

		condition = m_NumCols > 0;
		checkArgument(condition,
				"Number of columns must be greater than 0, but is %s",
				m_NumCols);

		condition = m_NumNodes > 0;
		checkArgument(condition,
				"Number of nodes must be greater than 0, but is %s",
				m_NumNodes);

		condition = m_SizeX > 0;
		checkArgument(condition, "X size must be greater than 0");

		condition = m_SizeY > 0;
		checkArgument(condition, "Y size must be greater than 0");

	}

	/**
	 * Get the number of Cols
	 *
	 * @return the number of Cols
	 * @since Oct 28, 2016
	 */
	public int getColsSize()
	{
		return m_NumCols;
	}

	/**
	 * Get the number of Nodes
	 *
	 * @return the number of Nodes
	 * @since Oct 25, 2016
	 */
	public int getNodesSize()
	{
		return m_NumNodes;
	}

	/**
	 * Get the number of Rows
	 *
	 * @return the number of Rows
	 * @since Oct 25, 2016
	 */
	public int getRowsSize()
	{
		return m_NumRows;
	}

	/**
	 * @return node horizontal size
	 * @since Oct 26, 2016
	 */
	public float getSizeX()
	{
		return m_SizeX;
	}

	/**
	 * @return node vertical size
	 * @since Oct 26, 2016
	 */
	public float getSizeY()
	{
		return m_SizeY;
	}

	/**
	 * Get the Title field
	 *
	 * @return the Title field
	 * @since Oct 25, 2016
	 */
	public String getTitle()
	{
		return m_Title;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("title", m_Title)
				.add("numRows", m_NumRows).add("numCols", m_NumCols)
				.add("numNodes", m_NumNodes).add("size_x", m_SizeX)
				.add("size_y", m_SizeY).toString();
	}

}
