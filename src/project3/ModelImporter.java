package project3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ModelImporter
{
	private ArrayList<Float> m_vertVals = new ArrayList<Float>();
	private ArrayList<Float> m_triangleVerts = new ArrayList<Float>();
	private ArrayList<Float> m_textureCoords = new ArrayList<Float>();
	private ArrayList<Float> m_stVals = new ArrayList<Float>();
	private ArrayList<Float> m_normals = new ArrayList<Float>();
	private ArrayList<Float> m_normVals = new ArrayList<Float>();
	
	public void parseOBJ(String filename) throws IOException
	{
		InputStream input = ModelImporter.class.getResourceAsStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(input));
		String line;
		while((line = br.readLine()) != null)
		{
			if(line.startsWith("v "))            // vertex position ("v" case)
			{
				for(String s : (line.substring(2)).split(" "))
				{
					m_vertVals.add(Float.valueOf(s));
				}
			}
			else if(line.startsWith("vt"))            // texture coordinates ("vt" case)
			{
				for(String s : (line.substring(3)).split(" "))
				{
					m_stVals.add(Float.valueOf(s));
				}
			}
			else if(line.startsWith("vn"))            // vertex normals ("vn" case)
			{
				for(String s : (line.substring(3)).split(" "))
				{
					m_normVals.add(Float.valueOf(s));
				}
			}
			else if(line.startsWith("f"))            // triangle faces ("f" case)
			{
				for(String s : (line.substring(2)).split(" "))
				{
					String v = s.split("/")[0];
					String vt = s.split("/")[1];
					String vn = s.split("/")[2];
					
					int vertRef = (Integer.valueOf(v) - 1) * 3;
					int tcRef = (Integer.valueOf(vt) - 1) * 2;
					int normRef = (Integer.valueOf(vn) - 1) * 3;
					
					m_triangleVerts.add(m_vertVals.get(vertRef));
					m_triangleVerts.add(m_vertVals.get((vertRef) + 1));
					m_triangleVerts.add(m_vertVals.get((vertRef) + 2));
					
					m_textureCoords.add(m_stVals.get(tcRef));
					m_textureCoords.add(m_stVals.get(tcRef + 1));
					
					m_normals.add(m_normVals.get(normRef));
					m_normals.add(m_normVals.get(normRef + 1));
					m_normals.add(m_normVals.get(normRef + 2));
				}
			}
		}
		input.close();
	}
	
	public int getNumVertices()
	{
		return (m_triangleVerts.size() / 3);
	}
	
	public float[] getVertices()
	{
		float[] p = new float[m_triangleVerts.size()];
		for(int i = 0; i < m_triangleVerts.size(); i++)
		{
			p[i] = m_triangleVerts.get(i);
		}
		return p;
	}
	
	public float[] getTextureCoordinates()
	{
		float[] t = new float[(m_textureCoords.size())];
		for(int i = 0; i < m_textureCoords.size(); i++)
		{
			t[i] = m_textureCoords.get(i);
		}
		return t;
	}
	
	public float[] getNormals()
	{
		float[] n = new float[(m_normals.size())];
		for(int i = 0; i < m_normals.size(); i++)
		{
			n[i] = m_normals.get(i);
		}
		return n;
	}
}
