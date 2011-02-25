package com.rj.processing.plasmasound.visuals;

import msafluid.MSAFluidSolver2D;
import processing.core.PApplet;
import processing.core.PImage;
import android.view.MotionEvent;

import com.rj.processing.plasmasound.PlasmaSound;

public class PlasmaFluid extends Visual {

	
	final float FLUID_WIDTH = 40;

	public MSAFluidSolver2D fluidSolver;

	PImage imgFluid;


	public PlasmaFluid(PlasmaSound p) {
		super(p);
		initVis();
	}
	
	private void setupFluid() {
	    // create fluid and set options
	    fluidSolver = new MSAFluidSolver2D((int)(FLUID_WIDTH), (int)(FLUID_WIDTH * height/width));
	    
	    //options
		fluidSolver.enableRGB(true).setFadeSpeed(0.01f).setDeltaT(0.5f).setVisc(0.0001f).setSolverIterations(3);
	    
	    // create image to hold fluid picture
	    imgFluid = p.createImage(fluidSolver.getWidth(), fluidSolver.getHeight(), p.RGB);
	}

	public void initVis() {
		setupFluid();
		
		// textMode(SHAPE);
		p.textMode(p.MODEL);
		p.rectMode(p.CENTER);
		p.ellipseMode(p.CENTER);
	}

	
	public void drawVis() {
		p.pushStyle();
		
		drawFluid();
		
		
		p.popStyle();
		
		
	}

	private void drawFluid() {
	    fluidSolver.update();
	    
	    imgFluid.loadPixels();
	    int cellcount = fluidSolver.getNumCells();
	    for(int i=0; i<cellcount; i++) { //optimize here.
	        imgFluid.pixels[i] = p.color(fluidSolver.r[i], fluidSolver.g[i], fluidSolver.b[i]);
	    }  
	    imgFluid.updatePixels();//  fastblur(imgFluid, 2);
	    
	    p.image(imgFluid, 0, 0, width, height);
	}
	
	
	
	public void touchEvent(MotionEvent me, int i, float x, float y, float vx,
			float vy, float size) {
		fluidTouchEvent(me,i,x,y,vx,vy,size);
	}

	private void fluidTouchEvent(MotionEvent me, int i, float x, float y, float vx,
			float vy, float size) {
		
		float velocityScale = 30f;
		
		//PApplet.println(""+vx+","+vy);
		vx = vx * velocityScale;
		vy = vy * velocityScale;
		
	    addForce(x/width, y/height, vx/width, vy/height);
	}
	
	// add force and dye to fluid, and create particles
	public void addForce(float x, float y, float dx, float dy) {
	        float colorMult = 5;
	        colorMult=colorMult*y;
	        float velocityMult = 30.0f;

	        if (dx > 1) dx = 1;
	        if (dy > 5) dy = 1;

	        int drawColor;

	        p.colorMode(p.HSB, 360, 1, 1);
	        float hue = ((x + y) * 180 + p.frameCount) % 360;
	        drawColor = p.color(hue, 1, 1);
	        p.colorMode(PApplet.RGB, 255);  
	        for (int i=0; i<3; i++) {
	        	for (int j=0; j<1; j++) {
		        	int index = fluidSolver.getIndexForNormalizedPosition(x+.01f*i, y+.01f*j);
			        fluidSolver.rOld[index]  += p.red(drawColor) * colorMult;
			        fluidSolver.gOld[index]  += p.green(drawColor) * colorMult;
			        fluidSolver.bOld[index]  += p.blue(drawColor) * colorMult;
			
			        fluidSolver.uOld[index] += dx * velocityMult;
			        fluidSolver.vOld[index] += dy * velocityMult;
	        	}
	        }
	}




}