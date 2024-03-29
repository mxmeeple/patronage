package com.meeple.citybuild.client.gui;

import org.joml.Math;

import com.meeple.citybuild.client.render.Screen;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.utils.FrameUtils;

public class LoadingScreen extends Screen {

	@Override
	public void render( ClientWindow window, GLContext glContext, Delta delta) {

		double menuSeconds = FrameUtils.nanosToSecondsInacurate(delta.totalNanos);

		float r = (float) (Math.sin(menuSeconds * 0.03f + 0.1f)) * 0.5f;
		float g = (float) (Math.sin(menuSeconds * 0.02f + 0.2f)) * 0.5f;
		float b = (float) (Math.sin(menuSeconds * 0.01f + 0.3f)) * 0.5f;
		window.clearColour.set(r, g, b, 1f);
	}

}
