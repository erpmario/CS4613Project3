package project2;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import graphicslib3D.*;
import graphicslib3D.shape.Sphere;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL4.*;

/**
 * Project 2: 3D Modeling and Camera Manipulation
 * <p>
 * Based on Program 4.3 - Multiple Models and Program 6.1 - Sphere from Gordon & Clevenger.
 *
 * @author Eric Peterson
 */
public class Project2 extends JFrame implements GLEventListener, KeyListener
{
	/* ********* *
	 * Constants *
	 * ********* */
	private static final int SPHERE_PRECISION = 24;
	private static final float TRANSLATE_FACTOR = 0.5f;
	private static final float YAW_FACTOR = 0.1f;
	private static final float PITCH_FACTOR = 0.1f;
	private static final String EARTH_TEXTURE_FILE = "textures/earth.jpg";
	private static final String SUN_TEXTURE_FILE = "textures/sun.jpg";
	private static final String EARTH_MOON_TEXTURE_FILE = "textures/moon.jpg";
	private static final String MARS_TEXTURE_FILE = "textures/mars.jpg";
	private static final String PHOBOS_TEXTURE_FILE = "textures/phobos.jpg";
	private static final String ME_TEXTURE_FILE = "textures/me.jpg";
	private static final String RED_TEXTURE_FILE = "textures/red.jpg";
	private static final String GREEN_TEXTURE_FILE = "textures/green.jpg";
	private static final String BLUE_TEXTURE_FILE = "textures/blue.jpg";
	
	/* **************** *
	 * Member Variables *
	 * **************** */
	private GLCanvas m_myCanvas;
	private int m_renderingProgram;
	private int[] m_vao;
	private int[] m_vbo;
	private MatrixStack m_mvStack;
	private float m_cameraX, m_cameraY, m_cameraZ, m_cameraPitch, m_cameraYaw;
	private Vector3D m_forwardVector;
	private float m_sunLocX, m_sunLocY, m_sunLocZ;
	private FPSAnimator m_animator;
	private Sphere m_sun, m_earth, m_earthMoon, m_mars, m_phobos;
	private PentagonalPrism m_pentagonalPrism;
	private int m_sunTexture, m_earthTexture, m_earthMoonTexture, m_marsTexture, m_phobosTexture, m_meTexture, m_redTexture, m_greenTexture, m_blueTexture;
	private boolean m_drawWorldAxes;
	
	public Project2()
	{
		// Initialize default member variable values.
		m_vao = new int[1];
		m_vbo = new int[21];
		m_mvStack = new MatrixStack(20);
		m_sun = new Sphere(SPHERE_PRECISION);
		m_earth = new Sphere(SPHERE_PRECISION);
		m_earthMoon = new Sphere(SPHERE_PRECISION);
		m_mars = new Sphere(SPHERE_PRECISION);
		m_phobos = new Sphere(SPHERE_PRECISION);
		m_pentagonalPrism = new PentagonalPrism(1);
		m_drawWorldAxes = true;
		
		// Set up JFrame properties.
		setTitle("Project 2 - 3D Modeling and Camera Manipulation");
		setSize(800, 800);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		m_myCanvas = new GLCanvas();
		m_myCanvas.addGLEventListener(this);
		m_myCanvas.addKeyListener(this);
		getContentPane().add(m_myCanvas);
		this.setVisible(true);
		m_animator = new FPSAnimator(m_myCanvas, 60);
		m_animator.start();
	}
	
	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		updateForward();
		
		m_mvStack = new MatrixStack(20);
		
		// Clear the depth buffer so no trails are left behind.
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		float bkg[] = {0.0f, 0.0f, 0.0f, 1.0f};
		FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(m_renderingProgram);
		
		// Get the memory locations of the uniforms in the shaders.
		int mvLoc = gl.glGetUniformLocation(m_renderingProgram, "mv_matrix");
		int projLoc = gl.glGetUniformLocation(m_renderingProgram, "proj_matrix");
		
		// Construct perspective projection matrix.
		float aspect = (float) m_myCanvas.getWidth() / (float) m_myCanvas.getHeight();
		Matrix3D pMat = perspective(60.0f, aspect, 0.1f, 1000.0f);
		
		// Set up view matrix.
		m_mvStack.pushMatrix();
		m_mvStack.multMatrix(
				lookAt(new Point3D(m_cameraX, m_cameraY, m_cameraZ), new Point3D(m_cameraX + m_forwardVector.getX(), m_cameraY + m_forwardVector.getY(), m_cameraZ + m_forwardVector.getZ()),
						new Vector3D(0.0f, 1.0f, 0.0f)));
		//m_mvStack.loadMatrix(m_viewMatrix);
		//m_mvStack.translate(-m_cameraX, -m_cameraY, -m_cameraZ);
		
		double amt = (System.currentTimeMillis()) / 1000.0;
		
		// Pass the projection matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
		
		/* *** *
		 * Sun *
		 * *** */
		
		// Apply transformations to the model-view matrix.
		m_mvStack.pushMatrix();
		m_mvStack.translate(m_sunLocX, m_sunLocY, m_sunLocZ);
		m_mvStack.pushMatrix();
		m_mvStack.rotate(((System.currentTimeMillis()) / 100.0) % 360, 0.0, 1.0, 0.0);
		
		// Pass the model-view matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_mvStack.peek().getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, m_sunTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float max[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		
		// Draw the object.
		int numVerts = m_sun.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		m_mvStack.popMatrix();
		
		/* ***** *
		 * Earth *
		 * ***** */
		
		// Apply transformations to the model-view matrix.
		m_mvStack.pushMatrix();
		m_mvStack.translate(Math.sin(amt) * 4.0f, 0.0f, Math.cos(amt) * 4.0f);
		m_mvStack.pushMatrix();
		m_mvStack.rotate(((System.currentTimeMillis()) / 50.0) % 360, 0.0, 1.0, 0.0);
		m_mvStack.scale(0.75, 0.75, 0.75);
		
		// Pass the model-view matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_mvStack.peek().getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[4]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, m_earthTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float max[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		
		// Draw the object.
		numVerts = m_earth.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		m_mvStack.popMatrix();
		
		/* ************ *
		 * Earth's Moon *
		 * ************ */
		
		// Apply transformations to the model-view matrix.
		m_mvStack.pushMatrix();
		m_mvStack.translate(0.0f, Math.sin(amt) * 2.0f, Math.cos(amt) * 2.0f);
		m_mvStack.rotate(((System.currentTimeMillis()) / 10.0) % 360, 0.0, 0.0, 1.0);
		m_mvStack.scale(0.25, 0.25, 0.25);
		
		// Pass the model-view matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_mvStack.peek().getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[7]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, m_earthMoonTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float max[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		
		// Draw the object.
		numVerts = m_earthMoon.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		m_mvStack.popMatrix();
		
		// Go back to sun reference.
		m_mvStack.popMatrix();
		
		/* **** *
		 * Mars *
		 * **** */
		
		// Apply transformations to the model-view matrix.
		m_mvStack.pushMatrix();
		m_mvStack.translate(Math.sin(amt * 1.5) * 7.0f, Math.sin(amt * 1.5) * 7.0f, Math.cos(amt * 1.5) * 7.0f);
		m_mvStack.pushMatrix();
		m_mvStack.rotate(((System.currentTimeMillis()) / 40.0) % 360, 0.0, 1.0, 0.0);
		m_mvStack.scale(0.60, 0.60, 0.60);
		
		// Pass the model-view matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_mvStack.peek().getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[9]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[10]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, m_marsTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float max[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		
		// Draw the object.
		numVerts = m_mars.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		m_mvStack.popMatrix();
		
		/* ****** *
		 * Phobos *
		 * ****** */
		
		// Apply transformations to the model-view matrix.
		m_mvStack.pushMatrix();
		m_mvStack.translate(Math.cos(amt * 2.0) * 1.5f, Math.sin(amt * 2.0) * 1.5f, Math.cos(amt * 2.0) * 1.5f);
		m_mvStack.rotate(((System.currentTimeMillis()) / 25.0) % 360, 0.0, 1.0, 1.0);
		m_mvStack.scale(0.20, 0.20, 0.20);
		
		// Pass the model-view matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_mvStack.peek().getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[12]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[13]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, m_phobosTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float max[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		
		// Draw the object.
		numVerts = m_phobos.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		m_mvStack.popMatrix();
		
		m_mvStack.popMatrix();
		
		
		/* **************** *
		 * Pentagonal Prism *
		 * **************** */
		
		// Apply transformations to the model-view matrix.
		m_mvStack.pushMatrix();
		m_mvStack.translate(0.0f, Math.sin(amt * 2.0) * 8.0f, Math.cos(amt * 2.0) * 8.0f);
		m_mvStack.pushMatrix();
		m_mvStack.rotate(((System.currentTimeMillis()) / 40.0) % 360, 0.0, 1.0, 0.0);
		m_mvStack.scale(0.50, 0.50, 0.50);
		
		// Pass the model-view matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_mvStack.peek().getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[18]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[19]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, m_meTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float max[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);
		
		// Draw the object.
		numVerts = m_pentagonalPrism.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		m_mvStack.popMatrix();
		m_mvStack.popMatrix();
		
		m_mvStack.popMatrix();
		
		/* ********** *
		 * World Axes *
		 * ********** */
		
		if(m_drawWorldAxes)
		{
			m_mvStack.pushMatrix();
			
			// Pass the model-view matrix to a uniform in the shader.
			gl.glUniformMatrix4fv(mvLoc, 1, false, m_mvStack.peek().getFloatValues(), 0);
			
			/* ****** *
			 * X Axis *
			 * ****** */
			
			// Bind the vertex buffer to a vertex attribute.
			gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[15]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);
			
			// Texture
			gl.glActiveTexture(GL_TEXTURE0);
			gl.glBindTexture(GL_TEXTURE_2D, m_redTexture);
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			gl.glGenerateMipmap(GL_TEXTURE_2D);
			if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
			{
				float max[] = new float[1];
				gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
				gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
			}
			
			// Enable depth test.
			gl.glEnable(GL_DEPTH_TEST);
			
			gl.glDrawArrays(GL_LINES, 0, 2);
			
			/* ****** *
			 * Y Axis *
			 * ****** */
			
			// Bind the vertex buffer to a vertex attribute.
			gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[16]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);
			
			// Texture
			gl.glActiveTexture(GL_TEXTURE0);
			gl.glBindTexture(GL_TEXTURE_2D, m_greenTexture);
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			gl.glGenerateMipmap(GL_TEXTURE_2D);
			if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
			{
				float max[] = new float[1];
				gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
				gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
			}
			
			// Enable depth test.
			gl.glEnable(GL_DEPTH_TEST);
			
			gl.glDrawArrays(GL_LINES, 0, 2);
			
			/* ****** *
			 * Z Axis *
			 * ****** */
			
			// Bind the vertex buffer to a vertex attribute.
			gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[17]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);
			
			// Texture
			gl.glActiveTexture(GL_TEXTURE0);
			gl.glBindTexture(GL_TEXTURE_2D, m_blueTexture);
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			gl.glGenerateMipmap(GL_TEXTURE_2D);
			if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
			{
				float max[] = new float[1];
				gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
				gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
			}
			
			// Enable depth test.
			gl.glEnable(GL_DEPTH_TEST);
			
			gl.glDrawArrays(GL_LINES, 0, 2);
			
			m_mvStack.popMatrix();
		}
		
		m_mvStack.popMatrix();
	}
	
	private void updateForward()
	{
		m_forwardVector.setX(Math.cos(m_cameraPitch) * Math.sin(m_cameraYaw));
		m_forwardVector.setY(Math.sin(m_cameraPitch));
		m_forwardVector.setZ(Math.cos(m_cameraPitch) * -Math.cos(m_cameraYaw));
		m_forwardVector.normalize();
	}
	
	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		m_renderingProgram = createShaderProgram("shaders/vert.shader", "shaders/frag.shader");
		setupVertices();
		
		// Camera Position
		m_cameraX = 0.0f;
		m_cameraY = 0.0f;
		m_cameraZ = 15.0f;
		m_cameraPitch = 0.0f;
		m_cameraYaw = 0.0f;
		
		// Forward vector is looking down negative z-axis.
		m_forwardVector = new Vector3D(0.0f, 0.0f, -1.0f);
		
		// Sun Position
		m_sunLocX = 0.0f;
		m_sunLocY = 0.0f;
		m_sunLocZ = 0.0f;
		
		// Load textures.
		m_sunTexture = loadTexture(SUN_TEXTURE_FILE).getTextureObject();
		m_earthTexture = loadTexture(EARTH_TEXTURE_FILE).getTextureObject();
		m_earthMoonTexture = loadTexture(EARTH_MOON_TEXTURE_FILE).getTextureObject();
		m_marsTexture = loadTexture(MARS_TEXTURE_FILE).getTextureObject();
		m_phobosTexture = loadTexture(PHOBOS_TEXTURE_FILE).getTextureObject();
		m_meTexture = loadTexture(ME_TEXTURE_FILE).getTextureObject();
		m_redTexture = loadTexture(RED_TEXTURE_FILE).getTextureObject();
		m_greenTexture = loadTexture(GREEN_TEXTURE_FILE).getTextureObject();
		m_blueTexture = loadTexture(BLUE_TEXTURE_FILE).getTextureObject();
	}
	
	private void setupVertices()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		// Set up vertex array.
		gl.glGenVertexArrays(m_vao.length, m_vao, 0);
		gl.glBindVertexArray(m_vao[0]);
		gl.glGenBuffers(m_vbo.length, m_vbo, 0);
		
		// Planets and Moons
		setupSphereVertices(m_sun, 0);
		setupSphereVertices(m_earth, 3);
		setupSphereVertices(m_earthMoon, 6);
		setupSphereVertices(m_mars, 9);
		setupSphereVertices(m_phobos, 12);
		
		// World Axes
		float[] xAxisVertices = {0.0f, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f};
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[15]);
		FloatBuffer xAxisVertBuf = Buffers.newDirectFloatBuffer(xAxisVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, xAxisVertBuf.limit() * 4, xAxisVertBuf, GL_STATIC_DRAW);
		
		float[] yAxisVertices = {0.0f, 0.0f, 0.0f, 0.0f, 5.0f, 0.0f};
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[16]);
		FloatBuffer yAxisVertBuf = Buffers.newDirectFloatBuffer(yAxisVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, yAxisVertBuf.limit() * 4, yAxisVertBuf, GL_STATIC_DRAW);
		
		float[] zAxisVertices = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 5.0f};
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[17]);
		FloatBuffer zAxisVertBuf = Buffers.newDirectFloatBuffer(zAxisVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, zAxisVertBuf.limit() * 4, zAxisVertBuf, GL_STATIC_DRAW);
		
		// Pentagonal Prism
		setupPentagonalPrismVertices(m_pentagonalPrism, 18);
	}
	
	private void setupPentagonalPrismVertices(PentagonalPrism prism, int startingVBOIndex)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		// Get vertices and indices.
		Vertex3D[] vertices = prism.getVertices();
		int[] indices = prism.getIndices();
		
		// Create vertex, texture, and normal buffers.
		float[] pValues = new float[indices.length * 3];
		float[] tValues = new float[indices.length * 2];
		//float[] nValues = new float[indices.length * 3];
		
		// Populate the buffers with the proper values.
		for(int i = 0; i < indices.length; i++)
		{
			pValues[i * 3] = (float) (vertices[indices[i]]).getX();
			pValues[i * 3 + 1] = (float) (vertices[indices[i]]).getY();
			pValues[i * 3 + 2] = (float) (vertices[indices[i]]).getZ();
			tValues[i * 2] = (float) (vertices[indices[i]]).getS();
			tValues[i * 2 + 1] = (float) (vertices[indices[i]]).getT();
			//nValues[i * 3] = (float) (vertices[indices[i]]).getNormalX();
			//nValues[i * 3 + 1] = (float) (vertices[indices[i]]).getNormalY();
			//nValues[i * 3 + 2] = (float) (vertices[indices[i]]).getNormalZ();
		}
		
		// Bind vertex buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pValues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		
		// Bind texture buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 1]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tValues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);
		
		// Bind normal buffer with a vbo entry.
		//gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 2]);
		//FloatBuffer normalBuf = Buffers.newDirectFloatBuffer(nValues);
		//gl.glBufferData(GL_ARRAY_BUFFER, normalBuf.limit() * 4, normalBuf, GL_STATIC_DRAW);
	}
	
	private void setupSphereVertices(Sphere sphere, int startingVBOIndex)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		// Get vertices and indices.
		Vertex3D[] vertices = sphere.getVertices();
		int[] indices = sphere.getIndices();
		
		// Create vertex, texture, and normal buffers.
		float[] pValues = new float[indices.length * 3];
		float[] tValues = new float[indices.length * 2];
		float[] nValues = new float[indices.length * 3];
		
		// Populate the buffers with the proper values.
		for(int i = 0; i < indices.length; i++)
		{
			pValues[i * 3] = (float) (vertices[indices[i]]).getX();
			pValues[i * 3 + 1] = (float) (vertices[indices[i]]).getY();
			pValues[i * 3 + 2] = (float) (vertices[indices[i]]).getZ();
			tValues[i * 2] = (float) (vertices[indices[i]]).getS();
			tValues[i * 2 + 1] = (float) (vertices[indices[i]]).getT();
			nValues[i * 3] = (float) (vertices[indices[i]]).getNormalX();
			nValues[i * 3 + 1] = (float) (vertices[indices[i]]).getNormalY();
			nValues[i * 3 + 2] = (float) (vertices[indices[i]]).getNormalZ();
		}
		
		// Bind vertex buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pValues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		
		// Bind texture buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 1]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tValues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);
		
		// Bind normal buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 2]);
		FloatBuffer normalBuf = Buffers.newDirectFloatBuffer(nValues);
		gl.glBufferData(GL_ARRAY_BUFFER, normalBuf.limit() * 4, normalBuf, GL_STATIC_DRAW);
	}
	
	private Matrix3D perspective(float fovy, float aspect, float n, float f)
	{
		float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0, 0, A);
		r.setElementAt(1, 1, q);
		r.setElementAt(2, 2, B);
		r.setElementAt(3, 2, -1.0f);
		r.setElementAt(2, 3, C);
		r.setElementAt(3, 3, 0.0f);
		return r;
	}
	
	public static void main(String[] args)
	{
		new Project2();
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
	}
	
	public void dispose(GLAutoDrawable drawable)
	{
	}
	
	private int createShaderProgram(String vertLoc, String fragLoc)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		String vshaderSource[] = GLSLUtils.readShaderSource(vertLoc);
		String fshaderSource[] = GLSLUtils.readShaderSource(fragLoc);
		
		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);
		
		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);
		
		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		gl.glLinkProgram(vfprogram);
		return vfprogram;
	}
	
	public Texture loadTexture(String textureFileName)
	{
		Texture tex = null;
		try
		{
			tex = TextureIO.newTexture(new File(textureFileName), false);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return tex;
	}
	
	private Matrix3D lookAt(Point3D eye, Point3D target, Vector3D y)
	{
		Vector3D eyeV = new Vector3D(eye);
		Vector3D targetV = new Vector3D(target);
		Vector3D fwd = (targetV.minus(eyeV)).normalize();
		Vector3D side = (fwd.cross(y)).normalize();
		Vector3D up = (side.cross(fwd)).normalize();
		
		Matrix3D look = new Matrix3D();
		look.setElementAt(0, 0, side.getX());
		look.setElementAt(1, 0, up.getX());
		look.setElementAt(2, 0, -fwd.getX());
		look.setElementAt(3, 0, 0.0f);
		look.setElementAt(0, 1, side.getY());
		look.setElementAt(1, 1, up.getY());
		look.setElementAt(2, 1, -fwd.getY());
		look.setElementAt(3, 1, 0.0f);
		look.setElementAt(0, 2, side.getZ());
		look.setElementAt(1, 2, up.getZ());
		look.setElementAt(2, 2, -fwd.getZ());
		look.setElementAt(3, 2, 0.0f);
		look.setElementAt(0, 3, side.dot(eyeV.mult(-1)));
		look.setElementAt(1, 3, up.dot(eyeV.mult(-1)));
		look.setElementAt(2, 3, (fwd.mult(-1)).dot(eyeV.mult(-1)));
		look.setElementAt(3, 3, 1.0f);
		return look;
	}
	
	@Override
	public void keyTyped(KeyEvent e)
	{
	
	}
	
	@Override
	public void keyPressed(KeyEvent e)
	{
		int keyCode = e.getExtendedKeyCode();
		switch(keyCode)
		{
			case KeyEvent.VK_W:
				m_cameraX += m_forwardVector.getX() * TRANSLATE_FACTOR;
				m_cameraY += m_forwardVector.getY() * TRANSLATE_FACTOR;
				m_cameraZ += m_forwardVector.getZ() * TRANSLATE_FACTOR;
				break;
			case KeyEvent.VK_S:
				m_cameraX -= m_forwardVector.getX() * TRANSLATE_FACTOR;
				m_cameraY -= m_forwardVector.getY() * TRANSLATE_FACTOR;
				m_cameraZ -= m_forwardVector.getZ() * TRANSLATE_FACTOR;
				break;
			case KeyEvent.VK_A:
				Vector3D sideVector = m_forwardVector.cross(new Vector3D(0.0f, 1.0f, 0.0f));
				m_cameraX -= sideVector.getX() * TRANSLATE_FACTOR;
				m_cameraY -= sideVector.getY() * TRANSLATE_FACTOR;
				m_cameraZ -= sideVector.getZ() * TRANSLATE_FACTOR;
				break;
			case KeyEvent.VK_D:
				sideVector = m_forwardVector.cross(new Vector3D(0.0f, 1.0f, 0.0f));
				m_cameraX += sideVector.getX() * TRANSLATE_FACTOR;
				m_cameraY += sideVector.getY() * TRANSLATE_FACTOR;
				m_cameraZ += sideVector.getZ() * TRANSLATE_FACTOR;
				break;
			case KeyEvent.VK_E:
				sideVector = m_forwardVector.cross(new Vector3D(0.0f, 1.0f, 0.0f));
				Vector3D topVector = m_forwardVector.cross(sideVector);
				m_cameraX += topVector.getX() * TRANSLATE_FACTOR;
				m_cameraY += topVector.getY() * TRANSLATE_FACTOR;
				m_cameraZ += topVector.getZ() * TRANSLATE_FACTOR;
				break;
			case KeyEvent.VK_Q:
				sideVector = m_forwardVector.cross(new Vector3D(0.0f, 1.0f, 0.0f));
				topVector = m_forwardVector.cross(sideVector);
				m_cameraX -= topVector.getX() * TRANSLATE_FACTOR;
				m_cameraY -= topVector.getY() * TRANSLATE_FACTOR;
				m_cameraZ -= topVector.getZ() * TRANSLATE_FACTOR;
				break;
			case KeyEvent.VK_LEFT:
				m_cameraYaw -= YAW_FACTOR;
				break;
			case KeyEvent.VK_RIGHT:
				m_cameraYaw += YAW_FACTOR;
				break;
			case KeyEvent.VK_UP:
				m_cameraPitch += PITCH_FACTOR;
				break;
			case KeyEvent.VK_DOWN:
				m_cameraPitch -= PITCH_FACTOR;
				break;
			case KeyEvent.VK_SPACE:
				m_drawWorldAxes = !m_drawWorldAxes;
				break;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e)
	{
	
	}
}