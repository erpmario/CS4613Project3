package project3;

import graphicslib3D.Vertex3D;

import java.io.IOException;

public class ImportedModel
{
	private Vertex3D[] m_vertices;
	private int m_numVertices;
	
	public ImportedModel(String filename)
	{
		ModelImporter modelImporter = new ModelImporter();
		try
		{
			modelImporter.parseOBJ(filename);
			m_numVertices = modelImporter.getNumVertices();
			float[] verts = modelImporter.getVertices();
			float[] tcs = modelImporter.getTextureCoordinates();
			float[] normals = modelImporter.getNormals();
			
			m_vertices = new Vertex3D[m_numVertices];
			for(int i = 0; i < m_vertices.length; i++)
			{
				m_vertices[i] = new Vertex3D();
				m_vertices[i].setLocation(verts[i * 3], verts[i * 3 + 1], verts[i * 3 + 2]);
				m_vertices[i].setST(tcs[i * 2], tcs[i * 2 + 1]);
				m_vertices[i].setNormal(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public Vertex3D[] getVertices()
	{
		return m_vertices;
	}
	
	public int getNumVertices()
	{
		return m_numVertices;
	}
}
