package project3;

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
import graphicslib3D.light.AmbientLight;
import graphicslib3D.light.PositionalLight;
import graphicslib3D.shape.Sphere;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Scanner;

import static com.jogamp.opengl.GL4.*;

/**
 * Project 3: Lights, Materials, Textures, Shadows, and Skyboxes
 * <p>
 * Based on Program 4.3 - Multiple Models and Program 6.1 - Sphere from Gordon & Clevenger.
 *
 * @author Eric Peterson
 */
public class Project3 extends JFrame implements GLEventListener, KeyListener
{
	/* ********* *
	 * Constants *
	 * ********* */
	private static final int SPHERE_PRECISION = 24;
	private static final float TRANSLATE_FACTOR = 0.5f;
	private static final float YAW_FACTOR = 0.1f;
	private static final float PITCH_FACTOR = 0.1f;
	private static final float LIGHT_MOVEMENT_FACTOR = 0.5f;
	private static final String EARTH_TEXTURE_FILE = "textures/earth.jpg";
	private static final String SUN_TEXTURE_FILE = "textures/sun.jpg";
	private static final String SKYBOX_TEXTURE_FILE = "textures/interstellar.jpg";
	private static final String SHUTTLE_TEXTURE_FILE = "textures/shuttle.jpg";
	private static final String LIGHT_TEXTURE_FILE = "textures/light.jpg";
	private static final String SHUTTLE_OBJ_FILE = "shuttle.obj";
	private static final float[] POSITIONAL_LIGHT_ON = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
	private static final float[] POSITIONAL_LIGHT_OFF = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
	
	/* **************** *
	 * Member Variables *
	 * **************** */
	private GLCanvas m_myCanvas;
	private int m_renderingProgram1, m_renderingProgram2, m_renderingProgram3;
	private int[] m_vao;
	private int[] m_vbo;
	private Matrix3D m_modelMatrix, m_viewMatrix, m_modelViewMatrix;
	private float m_cameraX, m_cameraY, m_cameraZ, m_cameraPitch, m_cameraYaw;
	private Vector3D m_forwardVector;
	private float m_sunLocX, m_sunLocY, m_sunLocZ;
	private FPSAnimator m_animator;
	private Sphere m_sun, m_earth;
	private int m_sunTexture, m_earthTexture, m_skyboxTexture, m_shuttleTexture, m_lightTexture;
	private boolean m_usePositionalLight;
	private ImportedModel m_shuttle;
	private PositionalLight m_positionalLight;
	private Point3D m_lightLocation;
	private AmbientLight m_globalAmbient;
	private int m_screenSizeX, m_screenSizeY;
	private int[] m_shadowTex;
	private int[] m_shadowBuffer;
	private Matrix3D m_lightVMatrix;
	private Matrix3D m_lightPMatrix;
	private Matrix3D m_shadowMVP;
	private Matrix3D m_shadowMVP2;
	private Matrix3D m_b;
	
	public Project3()
	{
		// Initialize default member variable values.
		m_vao = new int[1];
		m_vbo = new int[27];
		m_modelMatrix = new Matrix3D();
		m_viewMatrix = new Matrix3D();
		m_modelViewMatrix = new Matrix3D();
		m_sun = new Sphere(SPHERE_PRECISION);
		m_earth = new Sphere(SPHERE_PRECISION);
		m_usePositionalLight = true;
		m_positionalLight = new PositionalLight();
		m_lightLocation = new Point3D(0.0f, 5.0f, 0.0f);
		m_globalAmbient = AmbientLight.getAmbientLight();
		m_globalAmbient.setValues(new float[] {0.7f, 0.7f, 0.7f, 1.0f});
		m_shadowTex = new int[1];
		m_shadowBuffer = new int[1];
		m_lightVMatrix = new Matrix3D();
		m_lightPMatrix = new Matrix3D();
		m_shadowMVP = new Matrix3D();
		m_shadowMVP2 = new Matrix3D();
		m_b = new Matrix3D();
		
		// Set up JFrame properties.
		setTitle("Project 3 - Lights, Materials, Textures, Shadows, and Skyboxes");
		setSize(800, 800);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
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
		
		m_positionalLight.setPosition(m_lightLocation);
		
		updateForward();
		
		// Clear the depth buffer so no trails are left behind.
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		float[] bkg = {0.0f, 0.0f, 0.0f, 1.0f};
		FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		// Make the custom frame buffer current, and associate it with the shadow texture.
		gl.glBindFramebuffer(GL_FRAMEBUFFER, m_shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, m_shadowTex[0], 0);
		
		// Disable drawing colors, but enable the depth computation.
		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(2.0f, 4.0f);
		
		passOne();
		
		gl.glDisable(GL_POLYGON_OFFSET_FILL);
		
		// Restore the default display buffer, and re-enable drawing.
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, m_shadowTex[0]);
		// Drawing only front faces allows back face culling.
		gl.glDrawBuffer(GL_FRONT);
		
		passTwo();
	}
	
	public void passOne()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		// m_renderingProgram1 contains only the pass one vertex shader.
		gl.glUseProgram(m_renderingProgram1);
		
		// Build the light's P and V matrices to look-at the origin.
		Point3D origin = new Point3D(0.0, 0.0, 0.0);
		Vector3D up = new Vector3D(0.0, 1.0, 0.0);
		m_lightVMatrix.setToIdentity();
		m_lightPMatrix.setToIdentity();
		// Vector from light to origin.
		m_lightVMatrix = lookAt(m_positionalLight.getPosition(), origin, up);
		float aspect = (float) m_myCanvas.getWidth() / (float) m_myCanvas.getHeight();
		m_lightPMatrix = perspective(60.0f, aspect, 0.1f, 1000.0f);
		
		int shadowLoc = gl.glGetUniformLocation(m_renderingProgram1, "shadowMVP");
		
		/* *** *
		 * Sun *
		 * *** */
		
		m_modelMatrix.setToIdentity();
		m_modelMatrix.translate(m_sunLocX, m_sunLocY, m_sunLocZ);
		m_modelMatrix.rotateY(((System.currentTimeMillis()) / 100.0) % 360);
		
		// We are drawing from the light's point of view, so we use the light's P and V matrices.
		m_shadowMVP.setToIdentity();
		m_shadowMVP.concatenate(m_lightPMatrix);
		m_shadowMVP.concatenate(m_lightVMatrix);
		m_shadowMVP.concatenate(m_modelMatrix);
		gl.glUniformMatrix4fv(shadowLoc, 1, false, m_shadowMVP.getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Enable depth test and face-culling.
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);
		
		// Draw the object.
		int numVerts = m_sun.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		
		/* ******* *
		 * Shuttle *
		 * ******* */
		
		m_modelMatrix.setToIdentity();
		m_modelMatrix.translate(-5.0, 0.0, 0.0);
		m_modelMatrix.rotateX((System.currentTimeMillis()) % 360);
		m_modelMatrix.rotateY((System.currentTimeMillis()) % 360);
		m_modelMatrix.rotateZ((System.currentTimeMillis()) % 360);
		
		// We are drawing from the light's point of view, so we use the light's P and V matrices.
		m_shadowMVP.setToIdentity();
		m_shadowMVP.concatenate(m_lightPMatrix);
		m_shadowMVP.concatenate(m_lightVMatrix);
		m_shadowMVP.concatenate(m_modelMatrix);
		gl.glUniformMatrix4fv(shadowLoc, 1, false, m_shadowMVP.getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[21]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);
		
		// Draw the object.
		numVerts = m_shuttle.getVertices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		
		/* ***** *
		 * Earth *
		 * ***** */
		
		m_modelMatrix.setToIdentity();
		m_modelMatrix.translate(5.0, 0.0, 0.0);
		m_modelMatrix.rotateY(((System.currentTimeMillis()) / 50.0) % 360);
		m_modelMatrix.scale(0.75, 0.75, 0.75);
		
		// We are drawing from the light's point of view, so we use the light's P and V matrices.
		m_shadowMVP.setToIdentity();
		m_shadowMVP.concatenate(m_lightPMatrix);
		m_shadowMVP.concatenate(m_lightVMatrix);
		m_shadowMVP.concatenate(m_modelMatrix);
		gl.glUniformMatrix4fv(shadowLoc, 1, false, m_shadowMVP.getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);
		
		// Draw the object.
		numVerts = m_earth.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
	}
	
	public void passTwo()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		// m_renderingProgram2 includes the vertex and fragment shader which ignore lighting and shadows.
		gl.glUseProgram(m_renderingProgram2);
		
		// Get the memory locations of the uniforms in the shaders.
		int mvLoc2 = gl.glGetUniformLocation(m_renderingProgram2, "mv_matrix");
		int projLoc2 = gl.glGetUniformLocation(m_renderingProgram2, "proj_matrix");
		
		// Construct perspective projection matrix.
		float aspect = (float) m_myCanvas.getWidth() / (float) m_myCanvas.getHeight();
		Matrix3D pMat = perspective(60.0f, aspect, 0.1f, 1000.0f);
		
		// Set up view matrix.
		m_viewMatrix.setToIdentity();
		m_viewMatrix =
				lookAt(new Point3D(m_cameraX, m_cameraY, m_cameraZ), new Point3D(m_cameraX + m_forwardVector.getX(), m_cameraY + m_forwardVector.getY(), m_cameraZ + m_forwardVector.getZ()),
						new Vector3D(0.0f, 1.0f, 0.0f));
		
		// Pass the projection matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(projLoc2, 1, false, pMat.getFloatValues(), 0);
		
		/* ****** *
		 * Skybox *
		 * ****** */
		
		// Pass the model-view matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(mvLoc2, 1, false, m_viewMatrix.getFloatValues(), 0);
		
		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[18]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[19]);
		gl.glVertexAttribPointer(1, 2, GL_DOUBLE, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Activate the skybox texture.
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, m_skyboxTexture);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW); // Cube is CW, but we are viewing its interior.
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36); // Draw skybox without depth testing.
		gl.glEnable(GL_DEPTH_TEST);
		
		/* **************** *
		 * Positional Light *
		 * **************** */
		
		if(m_usePositionalLight)
		{
			m_modelMatrix.setToIdentity();
			m_modelViewMatrix.setToIdentity();
			
			// Apply transformations to the model-view matrix.
			m_modelMatrix.translate(m_lightLocation.getX(), m_lightLocation.getY(), m_lightLocation.getZ());
			m_modelViewMatrix.concatenate(m_viewMatrix);
			m_modelViewMatrix.concatenate(m_modelMatrix);
			
			// Pass the model-view matrix to a uniform in the shader.
			gl.glUniformMatrix4fv(mvLoc2, 1, false, m_modelViewMatrix.getFloatValues(), 0);
			
			// Bind the vertex buffer to a vertex attribute.
			gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[24]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);
			
			// Bind the texture buffer to a vertex attribute.
			gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[25]);
			gl.glVertexAttribPointer(1, 2, GL_DOUBLE, false, 0, 0);
			gl.glEnableVertexAttribArray(1);
			
			// Set up texture.
			gl.glActiveTexture(GL_TEXTURE1);
			gl.glBindTexture(GL_TEXTURE_2D, m_lightTexture);
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			gl.glGenerateMipmap(GL_TEXTURE_2D);
			if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
			{
				float[] max = new float[1];
				gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
				gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
			}
			
			// Enable depth test and face-culling.
			gl.glEnable(GL_DEPTH_TEST);
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CW);
			
			// Draw the object.
			gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		}
		
		// Switch rendering programs.
		gl.glUseProgram(m_renderingProgram3);
		
		// Set up lights based on the light's current position.
		installLights(m_viewMatrix);
		
		// Get the locations of the uniforms in the shader.
		int mvLoc = gl.glGetUniformLocation(m_renderingProgram3, "mv_matrix");
		int projLoc = gl.glGetUniformLocation(m_renderingProgram3, "proj_matrix");
		int nLoc = gl.glGetUniformLocation(m_renderingProgram3, "norm_matrix");
		int shadowLoc = gl.glGetUniformLocation(m_renderingProgram3, "shadowMVP2");
		
		// Pass the projection matrix to a uniform in the shader.
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);

		/* *** *
		 * Sun *
		 * *** */
		
		m_modelMatrix.setToIdentity();
		m_modelViewMatrix.setToIdentity();
		
		// Apply transformations to the model-view matrix.
		m_modelMatrix.translate(m_sunLocX, m_sunLocY, m_sunLocZ);
		m_modelMatrix.rotateY((System.currentTimeMillis()) / 100.0 % 360);
		m_modelViewMatrix.concatenate(m_viewMatrix);
		m_modelViewMatrix.concatenate(m_modelMatrix);
		
		// Build the MVP matrix from the light's point of view.
		m_shadowMVP2.setToIdentity();
		m_shadowMVP2.concatenate(m_b);
		m_shadowMVP2.concatenate(m_lightPMatrix);
		m_shadowMVP2.concatenate(m_lightVMatrix);
		m_shadowMVP2.concatenate(m_modelMatrix);

		// Pass the model-view and normal matrices to uniforms in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_modelViewMatrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(nLoc, 1, false, m_modelViewMatrix.inverse().transpose().getFloatValues(), 0);
		gl.glUniformMatrix4fv(shadowLoc, 1, false, m_shadowMVP2.getFloatValues(), 0);

		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Bind the normal buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, m_sunTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float[] max = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}

		// Enable depth test and face-culling.
		//gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);

		// Draw the object.
		int numVerts = m_sun.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);

		/* ******* *
		 * Shuttle *
		 * ******* */
		
		m_modelMatrix.setToIdentity();
		m_modelViewMatrix.setToIdentity();
		
		// Apply transformations to the model-view matrix.
		m_modelMatrix.translate(-5.0, 0.0, 0.0);
		m_modelMatrix.rotateX((System.currentTimeMillis() / 10.0) % 360);
		m_modelMatrix.rotateY((System.currentTimeMillis() / 10.0) % 360);
		m_modelMatrix.rotateZ((System.currentTimeMillis() / 10.0) % 360);
		m_modelViewMatrix.concatenate(m_viewMatrix);
		m_modelViewMatrix.concatenate(m_modelMatrix);
		
		// Build the MVP matrix from the light's point of view.
		m_shadowMVP2.setToIdentity();
		m_shadowMVP2.concatenate(m_b);
		m_shadowMVP2.concatenate(m_lightPMatrix);
		m_shadowMVP2.concatenate(m_lightVMatrix);
		m_shadowMVP2.concatenate(m_modelMatrix);
		
		// Pass the model-view and normal matrices to uniforms in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_modelViewMatrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(nLoc, 1, false, m_modelViewMatrix.inverse().transpose().getFloatValues(), 0);
		gl.glUniformMatrix4fv(shadowLoc, 1, false, m_shadowMVP2.getFloatValues(), 0);

		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[21]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[22]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Bind the normal buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[23]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, m_shuttleTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float[] max = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);

		// Draw the object.
		numVerts = m_shuttle.getVertices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		
		/* ***** *
		 * Earth *
		 * ***** */
		
		m_modelMatrix.setToIdentity();
		m_modelViewMatrix.setToIdentity();
		
		// Apply transformations to the model-view matrix.
		m_modelMatrix.translate(5.0, 0.0, 0.0);
		m_modelMatrix.rotateY(((System.currentTimeMillis()) / 50.0) % 360);
		m_modelMatrix.scale(0.75, 0.75, 0.75);
		m_modelViewMatrix.concatenate(m_viewMatrix);
		m_modelViewMatrix.concatenate(m_modelMatrix);
		
		// Build the MVP matrix from the light's point of view.
		m_shadowMVP2.setToIdentity();
		m_shadowMVP2.concatenate(m_b);
		m_shadowMVP2.concatenate(m_lightPMatrix);
		m_shadowMVP2.concatenate(m_lightVMatrix);
		m_shadowMVP2.concatenate(m_modelMatrix);
		
		// Pass the model-view and normal matrices to uniforms in the shader.
		gl.glUniformMatrix4fv(mvLoc, 1, false, m_modelViewMatrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(nLoc, 1, false, m_modelViewMatrix.inverse().transpose().getFloatValues(), 0);
		gl.glUniformMatrix4fv(shadowLoc, 1, false, m_shadowMVP2.getFloatValues(), 0);

		// Bind the vertex buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		// Bind the texture buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[4]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		// Bind the normal buffer to a vertex attribute.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[5]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
		
		// Set up texture.
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, m_earthTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);
		if(gl.isExtensionAvailable("GL_EXT_texture_filer_anisotropic"))
		{
			float[] max = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
		}
		
		// Enable depth test and face-culling.
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);

		// Draw the object.
		numVerts = m_earth.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
	}
	
	private void installLights(Matrix3D viewMatrix)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		Point3D lightP = m_positionalLight.getPosition();
		Point3D lightPv = lightP.mult(viewMatrix);
		float[] viewspaceLightPos = new float[] {(float) lightPv.getX(), (float) lightPv.getY(), (float) lightPv.getZ()};
		
		// Set the current globalAmbient settings.
		int globalAmbLoc = gl.glGetUniformLocation(m_renderingProgram3, "globalAmbient");
		gl.glProgramUniform4fv(m_renderingProgram3, globalAmbLoc, 1, m_globalAmbient.getValues(), 0);
		
		// Get the locations of the light fields in the shader.
		int ambLoc = gl.glGetUniformLocation(m_renderingProgram3, "light.ambient");
		int diffLoc = gl.glGetUniformLocation(m_renderingProgram3, "light.diffuse");
		int specLoc = gl.glGetUniformLocation(m_renderingProgram3, "light.specular");
		int posLoc = gl.glGetUniformLocation(m_renderingProgram3, "light.position");
		
		// Set the uniform light values in the shader.
		gl.glProgramUniform4fv(m_renderingProgram3, ambLoc, 1, m_positionalLight.getAmbient(), 0);
		gl.glProgramUniform4fv(m_renderingProgram3, diffLoc, 1, m_positionalLight.getDiffuse(), 0);
		gl.glProgramUniform4fv(m_renderingProgram3, specLoc, 1, m_positionalLight.getSpecular(), 0);
		gl.glProgramUniform3fv(m_renderingProgram3, posLoc, 1, viewspaceLightPos, 0);
		
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
		m_renderingProgram1 = createVertexProgram("shaders/pass1vert.glsl");
		//m_renderingProgram1 = createShaderProgram("shaders/pass1vert.glsl", "shaders/pass1frag.glsl");
		m_renderingProgram2 = createShaderProgram("shaders/oldvert.glsl", "shaders/oldfrag.glsl");
		m_renderingProgram3 = createShaderProgram("shaders/vert.glsl", "shaders/frag.glsl");
		
		m_shuttle = new ImportedModel(SHUTTLE_OBJ_FILE);
		
		setupVertices();
		setupShadowBuffers();
		
		m_b.setElementAt(0, 0, 0.5);
		m_b.setElementAt(0, 1, 0.0);
		m_b.setElementAt(0, 2, 0.0);
		m_b.setElementAt(0, 3, 0.5f);
		m_b.setElementAt(1, 0, 0.0);
		m_b.setElementAt(1, 1, 0.5);
		m_b.setElementAt(1, 2, 0.0);
		m_b.setElementAt(1, 3, 0.5f);
		m_b.setElementAt(2, 0, 0.0);
		m_b.setElementAt(2, 1, 0.0);
		m_b.setElementAt(2, 2, 0.5);
		m_b.setElementAt(2, 3, 0.5f);
		m_b.setElementAt(3, 0, 0.0);
		m_b.setElementAt(3, 1, 0.0);
		m_b.setElementAt(3, 2, 0.0);
		m_b.setElementAt(3, 3, 1.0f);
		
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
		m_skyboxTexture = loadTexture(SKYBOX_TEXTURE_FILE).getTextureObject();
		m_shuttleTexture = loadTexture(SHUTTLE_TEXTURE_FILE).getTextureObject();
		m_lightTexture = loadTexture(LIGHT_TEXTURE_FILE).getTextureObject();
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
	}
	
	private void setupShadowBuffers()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		m_screenSizeX = m_myCanvas.getWidth();
		m_screenSizeY = m_myCanvas.getHeight();
		
		// Create the custom frame buffer.
		gl.glGenFramebuffers(1, m_shadowBuffer, 0);
		
		// Create the shadow texture and configure it to hold depth information.
		gl.glGenTextures(1, m_shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, m_shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, m_screenSizeX, m_screenSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
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
		
		// Skybox
		setupSkyboxVertices(18);
		
		// Shuttle
		setupObjectVertices(m_shuttle, 21);
		
		// Positional Light Cube
		setupCubeVertices(24);
	}
	
	private void setupCubeVertices(int startingVBOIndex)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		float[] cubeVertices = {-1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, // Back Face Triangle 1
				1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, // Back Face Triangle 2
				1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, // Right Face Triangle 1
				1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, // Right Face Triangle 2
				1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, // Front Face Triangle 1
				-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, // Front Face Triangle 2
				-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, // Left Face Triangle 1
				-1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, // Left Face Triangle 2
				-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, // Bottom Face Triangle 1
				1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, // Bottom Face Triangle 2
				-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, // Top Face Triangle 1
				1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f // Top Face Triangle 2
		};
		for(int i = 0; i < cubeVertices.length; ++i)
		{
			cubeVertices[i] *= 0.1f;
		}
		
		double[] cubeTextureCoord = {.25, (double) 2 / 3, .25, (double) 1 / 3, .50, (double) 1 / 3, // front triangles
				.50, (double) 1 / 3, .50, (double) 2 / 3, .25, (double) 2 / 3, //
				.50, (double) 1 / 3, .75, (double) 1 / 3, .50, (double) 2 / 3, // right triangles
				.75, (double) 1 / 3, .75, (double) 2 / 3, .50, (double) 2 / 3, //
				.75, (double) 1 / 3, 1.0, (double) 1 / 3, .75, (double) 2 / 3, // back triangles
				1.0, (double) 1 / 3, 1.0, (double) 2 / 3, .75, (double) 2 / 3, //
				0.0, (double) 1 / 3, .25, (double) 1 / 3, 0.0, (double) 2 / 3, // left triangles
				.25, (double) 1 / 3, .25, (double) 2 / 3, 0.0, (double) 2 / 3, //
				.25, 0.0, .50, 0.0, .50, (double) 1 / 3, // bottom triangles
				.50, (double) 1 / 3, .25, (double) 1 / 3, .25, 0.0, //
				.25, (double) 2 / 3, .50, (double) 2 / 3, .50, 1.0, // top triangles
				.50, 1.0, .25, 1.0, .25, (double) 2 / 3  //
		};
		
		float[] cubeNormals = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, // Back Face Triangle 1
				0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, // Back Face Triangle 2
				-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, // Right Face Triangle 1
				-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, // Right Face Triangle 2
				0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, // Front Face Triangle 1
				0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, // Front Face Triangle 2
				1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, // Left Face Triangle 1
				1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, // Left Face Triangle 1
				0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, // Bottom Face Triangle 1
				0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, // Bottom Face Triangle 1
				0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, // Top Face Triangle 1
				0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f // Top Face Triangle 2
		};
		
		// Bind vertex buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(cubeVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		
		// Bind texture buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 1]);
		DoubleBuffer texBuf = Buffers.newDirectDoubleBuffer(cubeTextureCoord);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 8, texBuf, GL_STATIC_DRAW);
		
		// Bind normal buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 2]);
		FloatBuffer normalBuf = Buffers.newDirectFloatBuffer(cubeNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, normalBuf.limit() * 4, normalBuf, GL_STATIC_DRAW);
	}
	
	private void setupObjectVertices(ImportedModel model, int startingVBOIndex)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		// Get vertices and number of vertices.
		Vertex3D[] vertices = model.getVertices();
		int numVertices = model.getNumVertices();
		
		// Create vertex, texture, and normal buffers.
		float[] pValues = new float[numVertices * 3];
		float[] tValues = new float[numVertices * 2];
		float[] nValues = new float[numVertices * 3];
		
		// Populate the buffers with the proper values.
		for(int i = 0; i < numVertices; i++)
		{
			pValues[i * 3] = (float) (vertices[i]).getX();
			pValues[i * 3 + 1] = (float) (vertices[i]).getY();
			pValues[i * 3 + 2] = (float) (vertices[i]).getZ();
			tValues[i * 2] = (float) (vertices[i]).getS();
			tValues[i * 2 + 1] = (float) (vertices[i]).getT();
			nValues[i * 3] = (float) (vertices[i]).getNormalX();
			nValues[i * 3 + 1] = (float) (vertices[i]).getNormalY();
			nValues[i * 3 + 2] = (float) (vertices[i]).getNormalZ();
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
	
	private void setupSkyboxVertices(int startingVBOIndex)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		float[] cubeVertices =
				{-1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
						-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f,
						1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						-1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f};
		for(int i = 0; i < cubeVertices.length; ++i)
		{
			cubeVertices[i] *= 250.0f;
		}
		
		double[] cubeTextureCoord = {.25, (double) 2 / 3, .25, (double) 1 / 3, .50, (double) 1 / 3, // front triangles
				.50, (double) 1 / 3, .50, (double) 2 / 3, .25, (double) 2 / 3, //
				.50, (double) 1 / 3, .75, (double) 1 / 3, .50, (double) 2 / 3, // right triangles
				.75, (double) 1 / 3, .75, (double) 2 / 3, .50, (double) 2 / 3, //
				.75, (double) 1 / 3, 1.0, (double) 1 / 3, .75, (double) 2 / 3, // back triangles
				1.0, (double) 1 / 3, 1.0, (double) 2 / 3, .75, (double) 2 / 3, //
				0.0, (double) 1 / 3, .25, (double) 1 / 3, 0.0, (double) 2 / 3, // left triangles
				.25, (double) 1 / 3, .25, (double) 2 / 3, 0.0, (double) 2 / 3, //
				.25, 0.0, .50, 0.0, .50, (double) 1 / 3, // bottom triangles
				.50, (double) 1 / 3, .25, (double) 1 / 3, .25, 0.0, //
				.25, (double) 2 / 3, .50, (double) 2 / 3, .50, 1.0, // top triangles
				.50, 1.0, .25, 1.0, .25, (double) 2 / 3  //
		};
		
		float[] cubeNormals = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, // Back Face Triangle 1
				0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, // Back Face Triangle 2
				-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, // Right Face Triangle 1
				-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, // Right Face Triangle 2
				0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, // Front Face Triangle 1
				0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, // Front Face Triangle 2
				1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, // Left Face Triangle 1
				1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, // Left Face Triangle 1
				0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, // Bottom Face Triangle 1
				0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, // Bottom Face Triangle 1
				0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, // Top Face Triangle 1
				0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f // Top Face Triangle 2
		};
		
		// Bind vertex buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(cubeVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		
		// Bind texture buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 1]);
		DoubleBuffer texBuf = Buffers.newDirectDoubleBuffer(cubeTextureCoord);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 8, texBuf, GL_STATIC_DRAW);
		
		// Bind normal buffer with a vbo entry.
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[startingVBOIndex + 2]);
		FloatBuffer normalBuf = Buffers.newDirectFloatBuffer(cubeNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, normalBuf.limit() * 4, normalBuf, GL_STATIC_DRAW);
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
		
		GLSLUtils.checkOpenGLError();
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
		Scanner pause = new Scanner(System.in);
		pause.next();
		new Project3();
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
		
		String[] vshaderSource = GLSLUtils.readShaderSource(vertLoc);
		String[] fshaderSource = GLSLUtils.readShaderSource(fragLoc);
		
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
	
	private int createVertexProgram(String vertLoc)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		String[] vshaderSource = GLSLUtils.readShaderSource(vertLoc);
		
		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		
		gl.glCompileShader(vShader);
		
		int vprogram = gl.glCreateProgram();
		gl.glAttachShader(vprogram, vShader);
		gl.glLinkProgram(vprogram);
		return vprogram;
	}
	
	private Texture loadTexture(String textureFileName)
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
			// Camera Movement
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
			// Positional Light Movement
			case KeyEvent.VK_I:
				m_lightLocation.setZ(m_lightLocation.getZ() - LIGHT_MOVEMENT_FACTOR);
				break;
			case KeyEvent.VK_K:
				m_lightLocation.setZ(m_lightLocation.getZ() + LIGHT_MOVEMENT_FACTOR);
				break;
			case KeyEvent.VK_J:
				m_lightLocation.setX(m_lightLocation.getX() - LIGHT_MOVEMENT_FACTOR);
				break;
			case KeyEvent.VK_L:
				m_lightLocation.setX(m_lightLocation.getX() + LIGHT_MOVEMENT_FACTOR);
				break;
			case KeyEvent.VK_O:
				m_lightLocation.setY(m_lightLocation.getY() - LIGHT_MOVEMENT_FACTOR);
				break;
			case KeyEvent.VK_U:
				m_lightLocation.setY(m_lightLocation.getY() + LIGHT_MOVEMENT_FACTOR);
				break;
			// Positional Light Toggle
			case KeyEvent.VK_P:
				m_usePositionalLight = !m_usePositionalLight;
				// Turn light on or off based on boolean.
				if(m_usePositionalLight)
				{
					m_positionalLight.setDiffuse(POSITIONAL_LIGHT_ON);
					m_positionalLight.setSpecular(POSITIONAL_LIGHT_ON);
				}
				else
				{
					m_positionalLight.setDiffuse(POSITIONAL_LIGHT_OFF);
					m_positionalLight.setSpecular(POSITIONAL_LIGHT_OFF);
				}
				break;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e)
	{
	
	}
}