package com.meeple.citybuild.client.render;

import org.joml.Vector4f;

import com.meeple.shared.Delta;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;

public abstract class Screen {
	public String name;
	public final Vector4f colour = new Vector4f();

	Screen parent;
	Screen child;

	public final boolean isTransparent() {
		return colour.w < 1f;
	}

	public final Screen getParent() {
		return parent;
	}

	public final void setParent(Screen parent) {
		this.parent = parent;
		this.child = parent;
	}

	public final Screen getChild() {
		return child;
	}

	public final void setChild(Screen child) {
		this.child = child;
		child.parent = this;
	}

	public final Screen getRootParent() {
		Screen r = getParent();
		if (r == null) {
			return this;
		}
		while (r != null) {
			Screen next = r.getParent();
			if (next == null) {
				break;
			} else {
				r = next;
			}
		}
		return r;
	}

	public final Screen getRootChild() {
		Screen r = getChild();
		if (r == null) {
			return this;
		}
		while (r != null) {
			Screen next = r.getChild();
			if (next == null) {
				break;
			} else {
				r = next;
			}
		}
		return r;
	}

	/**
	 * Renders the entire tree using {@link #renderUp()}.<br>
	 * This can be called from any Renderable that is part of this rendering tree
	 */
	public void renderTree(ClientWindow window, GLContext glContext, Delta delta) {
		Screen child = getRootChild();
		child.renderUp(window, glContext, delta);
	}

	/**
	 * recursive render call for whole tree
	 */
	private void renderUp(ClientWindow window, GLContext glContext, Delta delta) {
		if (this.isTransparent()) {
			Screen parent = getParent();
			if (parent != null) {
				parent.renderUp(window, glContext, delta);
			}
		}
		this.render(window, glContext, delta);
	}

	public void clearParent() {
		if (this.parent != null)
			this.parent.child = null;
		this.parent = null;
	}

	public void clearChild() {
		if (this.child != null)
			this.child.parent = null;

		this.child = null;
	}
	public abstract void render(ClientWindow window, GLContext glContext, Delta delta);
	//public abstract void render(ClientWindow window, Delta delta);

}
