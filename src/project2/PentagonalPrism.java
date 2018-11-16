package project2;

import graphicslib3D.Shape3D;
import graphicslib3D.Vertex3D;

/**
 * Auxiliary class for quickly generating a pentagonal prism.
 * <p>
 * Based on Program 6.1.1 - Sphere (Coded Version) from Gordon & Clevenger.
 *
 * @author Eric Peterson
 */
public class PentagonalPrism extends Shape3D
{
	/* ********* *
	 * Constants *
	 * ********* */
	private static final int NUM_VERTICES = 12;
	//private static final int NUM_INDICES = 60;
	//private static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
	private static final double C1 = Math.cos((2 * Math.PI) / 5);
	private static final double C2 = Math.cos(Math.PI / 5);
	private static final double S1 = Math.sin((2 * Math.PI) / 5);
	private static final double S2 = Math.sin((4 * Math.PI) / 5);
	
	/* **************** *
	 * Member Variables *
	 * **************** */
	private int[] m_indices;
	private Vertex3D[] m_vertices;
	
	public PentagonalPrism(double height)
	{
		m_vertices = new Vertex3D[NUM_VERTICES];
		//m_indices = new int[NUM_INDICES];
		
		m_vertices[0] = new Vertex3D(0, 0, height / 2);
		m_vertices[0].setST(0.5, 0.5);
		m_vertices[1] = new Vertex3D(0, 1, height / 2);
		m_vertices[1].setST(0.5, 1);
		m_vertices[2] = new Vertex3D(S1, C1, height / 2);
		m_vertices[2].setST(1, 0.6);
		m_vertices[3] = new Vertex3D(S2, -C2, height / 2);
		m_vertices[3].setST(0.8, 0);
		m_vertices[4] = new Vertex3D(-S2, -C2, height / 2);
		m_vertices[4].setST(0.2, 0);
		m_vertices[5] = new Vertex3D(-S1, C1, height / 2);
		m_vertices[5].setST(0, 0.6);
		m_vertices[6] = new Vertex3D(0, 0, -height / 2);
		m_vertices[6].setST(0.5, 0.5);
		m_vertices[7] = new Vertex3D(0, 1, -height / 2);
		m_vertices[7].setST(0.5, 1);
		m_vertices[8] = new Vertex3D(S1, C1, -height / 2);
		m_vertices[8].setST(1, 0.6);
		m_vertices[9] = new Vertex3D(S2, -C2, -height / 2);
		m_vertices[9].setST(0.8, 0);
		m_vertices[10] = new Vertex3D(-S2, -C2, -height / 2);
		m_vertices[10].setST(0.2, 0);
		m_vertices[11] = new Vertex3D(-S1, C1, -height / 2);
		m_vertices[11].setST(0, 0.6);
		
		m_indices = new int[] {
				// Top Face (5 Triangles)
				0, 1, 2, 0, 2, 3, 0, 3, 4, 0, 4, 5, 0, 5, 1,
				// Bottom Face (5 Triangles)
				6, 7, 11, 6, 11, 10, 6, 10, 9, 6, 9, 8, 6, 8, 7,
				// Rectangles (2 Triangles Each)
				// Top Right Rectangle
				1, 7, 2, 7, 8, 2,
				// Bottom Right Rectangle
				2, 8, 3, 8, 9, 3,
				// Bottom Rectangle
				3, 9, 4, 9, 10, 4,
				// Bottom Left Rectangle
				4, 10, 5, 10, 11, 5,
				// Top Left Rectangle
				5, 11, 1, 11, 7, 1};
		
		/*m_vertices[0] = new Vertex3D(Math.sqrt((10 + (2 * Math.sqrt(5))) / 5), 0, 1); // (0.76, 0, 1)
		m_vertices[1] = new Vertex3D(Math.sqrt((10 + (2 * Math.sqrt(5))) / 5), 0, -1); // (0.76, 0, -1)
		m_vertices[2] = new Vertex3D(Math.sqrt((5 - Math.sqrt(5)) / 10), GOLDEN_RATIO, 1); // (0.166, 1.62, 1)
		m_vertices[3] = new Vertex3D(Math.sqrt((5 - Math.sqrt(5)) / 10), GOLDEN_RATIO, -1); // (0.166, 1.62, -1)
		m_vertices[4] = new Vertex3D(Math.sqrt((5 - Math.sqrt(5)) / 10), -GOLDEN_RATIO, 1); // (0.166, -1.62, 1)
		m_vertices[5] = new Vertex3D(Math.sqrt((5 - Math.sqrt(5)) / 10), -GOLDEN_RATIO, -1); // (0.166, -1.62, -1)
		m_vertices[6] = new Vertex3D(-Math.sqrt((5 + (2 * Math.sqrt(5))) / 5), 1, 1); // (-0.616, 1, 1)
		m_vertices[7] = new Vertex3D(-Math.sqrt((5 + (2 * Math.sqrt(5))) / 5), 1, -1); // (-0.616, 1, -1)
		m_vertices[8] = new Vertex3D(-Math.sqrt((5 + (2 * Math.sqrt(5))) / 5), -1, 1); // (-0.616, -1, 1)
		m_vertices[9] = new Vertex3D(-Math.sqrt((5 + (2 * Math.sqrt(5))) / 5), -1, -1); // (-0.616, -1, -1)
		*/
		/*for(int i = 0; i < m_indices.length; ++i)
		{
			m_indices[i] = i;
		}*/
	}
	
	public int[] getIndices()
	{
		return m_indices;
	}
	
	public Vertex3D[] getVertices()
	{
		return m_vertices;
	}
}