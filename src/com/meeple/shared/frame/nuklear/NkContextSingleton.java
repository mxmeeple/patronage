package com.meeple.shared.frame.nuklear;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.nuklear.NkAllocator;
import org.lwjgl.nuklear.NkBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkConvertConfig;
import org.lwjgl.nuklear.NkDrawCommand;
import org.lwjgl.nuklear.NkDrawNullTexture;
import org.lwjgl.nuklear.NkDrawVertexLayoutElement;
import org.lwjgl.nuklear.NkMouse;
import org.lwjgl.nuklear.NkUserFont;
import org.lwjgl.nuklear.NkUserFontGlyph;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import com.meeple.shared.frame.window.MirroredWindowCallbacks;
import com.meeple.shared.frame.window.UserInput;

public class NkContextSingleton implements Closeable {

	public static final String fontFolder = "resources/fonts/";
	public static final String fontExtension = ".ttf";
	private static NkUserFont default_font;

	private static NkDrawNullTexture null_texture = NkDrawNullTexture.create();
	private static final int BUFFER_INITIAL_SIZE = 4 * 1024;
	private static final int MAX_VERTEX_BUFFER = 512 * 1024;
	private static final int MAX_ELEMENT_BUFFER = 128 * 1024;

	private static final NkAllocator ALLOCATOR;

	private static final NkDrawVertexLayoutElement.Buffer VERTEX_LAYOUT;

	static {
		ALLOCATOR = NkAllocator
			.create()
			.alloc((handle, old, size) -> nmemAllocChecked(size))
			.mfree((handle, ptr) -> nmemFree(ptr));

		VERTEX_LAYOUT = NkDrawVertexLayoutElement
			.create(4)
			.position(0)
			.attribute(NK_VERTEX_POSITION)
			.format(NK_FORMAT_FLOAT)
			.offset(0)
			.position(1)
			.attribute(NK_VERTEX_TEXCOORD)
			.format(NK_FORMAT_FLOAT)
			.offset(8)
			.position(2)
			.attribute(NK_VERTEX_COLOR)
			.format(NK_FORMAT_R8G8B8A8)
			.offset(16)
			.position(3)
			.attribute(NK_VERTEX_ATTRIBUTE_COUNT)
			.format(NK_FORMAT_COUNT)
			.offset(0)
			.flip();
	}

	public NkContext context = NkContext.create();

	/**
	 * It is important to store the TTF buffer externally for the STB library to use
	 */
	ByteBuffer ttf;

	private NkBuffer cmds = NkBuffer.create();
	private int vbo, vao, ebo;
	private int prog;
	private int vert_shdr;
	private int frag_shdr;
	private int uniform_tex;
	private int uniform_proj;
	public boolean hasClosed = false;

	public void render(float frameBufferX, float frameBufferY, float windowWidth, float windowHeight) {
		/* IMPORTANT: `nk_glfw_render` modifies some global OpenGL state
		* with blending, scissor, face culling, depth test and viewport and
		* defaults everything back into a default state.
		* Make sure to either a.) save and restore or b.) reset your own state after
		* rendering the UI.*/
		boolean blend = glGetBoolean(GL_BLEND);
		boolean cull = glGetBoolean(GL_CULL_FACE);
		boolean depth = glGetBoolean(GL_DEPTH_TEST);
		boolean scissor = glGetBoolean(GL_SCISSOR_TEST);
		float[] vals = new float[4];
		glGetFloatv(GL46.GL_VIEWPORT, vals);

		GL46.glPushAttrib(GL46.GL_VIEWPORT_BIT);
		//NOTE includes blend
		GL46.glPushAttrib(GL46.GL_COLOR_BUFFER_BIT);
		//NOTE all depth 
		GL46.glPushAttrib(GL46.GL_DEPTH_BUFFER_BIT);
		//NOTE cull faces
		GL46.glPushAttrib(GL46.GL_POLYGON_BIT);
		//NOTE scissor test and cull face
		GL46.glPushAttrib(GL46.GL_ENABLE_BIT);

		internalRender(frameBufferX, frameBufferY, windowWidth, windowHeight, NK_ANTI_ALIASING_ON, NkContextSingleton.MAX_VERTEX_BUFFER, NkContextSingleton.MAX_ELEMENT_BUFFER);
		GL46.glPopAttrib();
		GL46.glPopAttrib();
		GL46.glPopAttrib();
		GL46.glPopAttrib();
		GL46.glPopAttrib();/*
							if (blend) {
							glEnable(GL_BLEND);
							} else {
							glDisable(GL_BLEND);
							}
							if (cull) {
							glEnable(GL_CULL_FACE);
							} else {
							glDisable(GL_CULL_FACE);
							}
							if (depth) {
							glEnable(GL_DEPTH_TEST);
							} else {
							glDisable(GL_DEPTH_TEST);
							}
							
							if (scissor) {
							glEnable(GL_SCISSOR_TEST);
							} else {
							glDisable(GL_SCISSOR_TEST);
							}*/
		//		glViewport((int) vals[0], (int) vals[1], (int) vals[2], (int) vals[3]);
	}

	public void handleInput(long window) {

		nk_input_begin(context);
		glfwPollEvents();

		NkMouse mouse = context.input().mouse();
		if (mouse.grab()) {
			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
		} else if (mouse.grabbed()) {
			float prevX = mouse.prev().x();
			float prevY = mouse.prev().y();
			glfwSetCursorPos(window, prevX, prevY);
			mouse.pos().x(prevX);
			mouse.pos().y(prevY);
		} else if (mouse.ungrab()) {
			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
		}

		nk_input_end(context);
	}

	public void setup(long windowID, UserInput callbacks) {

		setupContext(windowID);
		addWindowCallbacks(callbacks);
		setupFont(NkContextSingleton.fontFolder + "FiraSans" + NkContextSingleton.fontExtension);
	}

	private void addWindowCallbacks(UserInput cb) {

		cb.scrollCallbackSet.add(new GLFWScrollCallbackI() {

			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				try (MemoryStack stack = stackPush()) {
					NkVec2 scroll = NkVec2
						.mallocStack(stack)
						.x((float) xoffset)
						.y((float) yoffset);
					nk_input_scroll(context, scroll);

				}
			}
		});
		cb.charCallbackSet.add(new GLFWCharCallbackI() {

			@Override
			public void invoke(long window, int codepoint) {
				nk_input_unicode(context, codepoint);

			}
		});
		cb.keyCallbackSet.add((windowID, key, scancode, action, mods) -> {
			boolean press = action == GLFW_PRESS;
			switch (key) {

				case GLFW_KEY_DELETE:
					nk_input_key(context, NK_KEY_DEL, press);
					break;
				case GLFW_KEY_ENTER:
					nk_input_key(context, NK_KEY_ENTER, press);
					break;
				case GLFW_KEY_TAB:
					nk_input_key(context, NK_KEY_TAB, press);
					break;
				case GLFW_KEY_BACKSPACE:
					nk_input_key(context, NK_KEY_BACKSPACE, press);
					break;
				case GLFW_KEY_UP:
					nk_input_key(context, NK_KEY_UP, press);
					break;
				case GLFW_KEY_DOWN:
					nk_input_key(context, NK_KEY_DOWN, press);
					break;
				case GLFW_KEY_HOME:
					nk_input_key(context, NK_KEY_TEXT_START, press);
					nk_input_key(context, NK_KEY_SCROLL_START, press);
					break;
				case GLFW_KEY_END:
					nk_input_key(context, NK_KEY_TEXT_END, press);
					nk_input_key(context, NK_KEY_SCROLL_END, press);
					break;
				case GLFW_KEY_PAGE_DOWN:
					nk_input_key(context, NK_KEY_SCROLL_DOWN, press);
					break;
				case GLFW_KEY_PAGE_UP:
					nk_input_key(context, NK_KEY_SCROLL_UP, press);
					break;
				case GLFW_KEY_LEFT_SHIFT:
				case GLFW_KEY_RIGHT_SHIFT:
					nk_input_key(context, NK_KEY_SHIFT, press);
					break;
				case GLFW_KEY_LEFT_CONTROL:
				case GLFW_KEY_RIGHT_CONTROL:
					if (press) {
						nk_input_key(context, NK_KEY_COPY, glfwGetKey(windowID, GLFW_KEY_C) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_PASTE, glfwGetKey(windowID, GLFW_KEY_P) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_CUT, glfwGetKey(windowID, GLFW_KEY_X) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_TEXT_UNDO, glfwGetKey(windowID, GLFW_KEY_Z) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_TEXT_REDO, glfwGetKey(windowID, GLFW_KEY_R) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_TEXT_WORD_LEFT, glfwGetKey(windowID, GLFW_KEY_LEFT) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_TEXT_WORD_RIGHT, glfwGetKey(windowID, GLFW_KEY_RIGHT) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_TEXT_LINE_START, glfwGetKey(windowID, GLFW_KEY_B) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_TEXT_LINE_END, glfwGetKey(windowID, GLFW_KEY_E) == GLFW_PRESS);
					} else {
						nk_input_key(context, NK_KEY_LEFT, glfwGetKey(windowID, GLFW_KEY_LEFT) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_RIGHT, glfwGetKey(windowID, GLFW_KEY_RIGHT) == GLFW_PRESS);
						nk_input_key(context, NK_KEY_COPY, false);
						nk_input_key(context, NK_KEY_PASTE, false);
						nk_input_key(context, NK_KEY_CUT, false);
						nk_input_key(context, NK_KEY_SHIFT, false);
					}
					break;
			}
		});
		cb.cursorPosCallbackSet.add((windowID, xpos, ypos) -> nk_input_motion(context, (int) xpos, (int) ypos));

		cb.mouseButtonCallbackSet.add((windowID, button, action, mods) -> {
			try (MemoryStack stack = stackPush()) {
				DoubleBuffer cx = stack.mallocDouble(1);
				DoubleBuffer cy = stack.mallocDouble(1);

				glfwGetCursorPos(windowID, cx, cy);

				int x = (int) cx.get(0);
				int y = (int) cy.get(0);

				int nkButton;
				switch (button) {
					case GLFW_MOUSE_BUTTON_RIGHT:
						nkButton = NK_BUTTON_RIGHT;
						break;
					case GLFW_MOUSE_BUTTON_MIDDLE:
						nkButton = NK_BUTTON_MIDDLE;
						break;
					default:
						nkButton = NK_BUTTON_LEFT;
				}
				nk_input_button(context, nkButton, x, y, action == GLFW_PRESS);

			}
		});

	}

	private void setupFont(String fontLocation) {

		int BITMAP_W = 1024;
		int BITMAP_H = 1024;

		int FONT_HEIGHT = 18;
		int fontTexID = glGenTextures();

		STBTTFontinfo fontInfo = STBTTFontinfo.create();
		STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(150);

		float scale;
		float descent;
		if (ttf == null) {
			try (MemoryStack stack = stackPush()) {

				try {
					ttf = IOUtil.ioResourceToByteBuffer(fontLocation, 512 * 1024);
				} catch (IOException err) {
					err.printStackTrace();
				}
			}
		}
		try (MemoryStack stack = stackPush()) {

			stbtt_InitFont(fontInfo, ttf);
			scale = stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT);

			IntBuffer d = stack.mallocInt(1);
			stbtt_GetFontVMetrics(fontInfo, null, d, null);
			descent = d.get(0) * scale;

			ByteBuffer bitmap = memAlloc(BITMAP_W * BITMAP_H);

			STBTTPackContext pc = STBTTPackContext.mallocStack(stack);
			stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL);
			stbtt_PackSetOversampling(pc, 4, 4);
			stbtt_PackFontRange(pc, ttf, 0, FONT_HEIGHT, 32, cdata);
			stbtt_PackEnd(pc);

			// Convert R8 to RGBA8
			ByteBuffer texture = memAlloc(BITMAP_W * BITMAP_H * 4);
			for (int i = 0; i < bitmap.capacity(); i++) {
				texture.putInt((bitmap.get(i) << 24) | 0x00FFFFFF);
			}
			texture.flip();

			glBindTexture(GL_TEXTURE_2D, fontTexID);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, BITMAP_W, BITMAP_H, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, texture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			memFree(texture);
			memFree(bitmap);
		}
		if (NkContextSingleton.default_font == null) {
			NkContextSingleton.default_font = NkUserFont.create();

			NkContextSingleton.default_font
				.width((handle, h, text, len) -> {
					float text_width = 0;
					try (MemoryStack stack = stackPush()) {
						IntBuffer unicode = stack.mallocInt(1);

						int glyph_len = nnk_utf_decode(text, memAddress(unicode), len);
						int text_len = glyph_len;

						if (glyph_len == 0) {
							return 0;
						}

						IntBuffer advance = stack.mallocInt(1);
						while (text_len <= len && glyph_len != 0) {
							if (unicode.get(0) == NK_UTF_INVALID) {
								break;
							}

							/* query currently drawn glyph information */
							stbtt_GetCodepointHMetrics(fontInfo, unicode.get(0), advance, null);
							text_width += advance.get(0) * scale;

							/* offset next glyph */
							glyph_len = nnk_utf_decode(text + text_len, memAddress(unicode), len - text_len);
							text_len += glyph_len;
						}
					}
					return text_width;
				})
				.height(FONT_HEIGHT)
				.query((handle, font_height, glyph, codepoint, next_codepoint) -> {
					try (MemoryStack stack = stackPush()) {
						FloatBuffer x = stack.floats(0.0f);
						FloatBuffer y = stack.floats(0.0f);

						STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);
						IntBuffer advance = stack.mallocInt(1);

						stbtt_GetPackedQuad(cdata, BITMAP_W, BITMAP_H, codepoint - 32, x, y, q, false);
						stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null);

						NkUserFontGlyph ufg = NkUserFontGlyph.create(glyph);

						ufg.width(q.x1() - q.x0());
						ufg.height(q.y1() - q.y0());
						ufg.offset().set(q.x0(), q.y0() + (FONT_HEIGHT + descent));
						ufg.xadvance(advance.get(0) * scale);
						ufg.uv(0).set(q.s0(), q.t0());
						ufg.uv(1).set(q.s1(), q.t1());
					}
				})
				.texture(
					it -> it
						.id(fontTexID));
		}

		nk_style_set_font(context, NkContextSingleton.default_font);
	}

	private void setupContext(long winID) {

		String NK_SHADER_VERSION = Platform.get() == Platform.MACOSX ? "#version 150\n" : "#version 300 es\n";
		String vertex_shader =
			NK_SHADER_VERSION +
				"uniform mat4 ProjMtx;\n" +
				"in vec2 Position;\n" +
				"in vec2 TexCoord;\n" +
				"in vec4 Color;\n" +
				"out vec2 Frag_UV;\n" +
				"out vec4 Frag_Color;\n" +
				"void main() {\n" +
				"   Frag_UV = TexCoord;\n" +
				"   Frag_Color = Color;\n" +
				"   gl_Position = ProjMtx * vec4(Position.xy, 0, 1);\n" +
				"}\n";
		String fragment_shader =
			NK_SHADER_VERSION +
				"precision mediump float;\n" +
				"uniform sampler2D Texture;\n" +
				"in vec2 Frag_UV;\n" +
				"in vec4 Frag_Color;\n" +
				"out vec4 Out_Color;\n" +
				"void main(){\n" +
				"   Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
				"}\n";

		nk_buffer_init(cmds, NkContextSingleton.ALLOCATOR, NkContextSingleton.BUFFER_INITIAL_SIZE);
		prog = glCreateProgram();
		vert_shdr = glCreateShader(GL_VERTEX_SHADER);
		frag_shdr = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(vert_shdr, vertex_shader);
		glShaderSource(frag_shdr, fragment_shader);
		glCompileShader(vert_shdr);
		glCompileShader(frag_shdr);
		if (glGetShaderi(vert_shdr, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new IllegalStateException();
		}
		if (glGetShaderi(frag_shdr, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new IllegalStateException();
		}
		glAttachShader(prog, vert_shdr);
		glAttachShader(prog, frag_shdr);
		glLinkProgram(prog);
		if (glGetProgrami(prog, GL_LINK_STATUS) != GL_TRUE) {
			throw new IllegalStateException();
		}

		uniform_tex = glGetUniformLocation(prog, "Texture");
		uniform_proj = glGetUniformLocation(prog, "ProjMtx");
		int attrib_pos = glGetAttribLocation(prog, "Position");
		int attrib_uv = glGetAttribLocation(prog, "TexCoord");
		int attrib_col = glGetAttribLocation(prog, "Color");

		{
			// buffer setup
			vbo = glGenBuffers();
			ebo = glGenBuffers();
			vao = glGenVertexArrays();

			glBindVertexArray(vao);
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

			glEnableVertexAttribArray(attrib_pos);
			glEnableVertexAttribArray(attrib_uv);
			glEnableVertexAttribArray(attrib_col);

			glVertexAttribPointer(attrib_pos, 2, GL_FLOAT, false, 20, 0);
			glVertexAttribPointer(attrib_uv, 2, GL_FLOAT, false, 20, 8);
			glVertexAttribPointer(attrib_col, 4, GL_UNSIGNED_BYTE, true, 20, 16);
		}

		{
			// null texture setup
			int nullTexID = glGenTextures();

			NkContextSingleton.null_texture.texture().id(nullTexID);
			NkContextSingleton.null_texture.uv().set(0.5f, 0.5f);

			glBindTexture(GL_TEXTURE_2D, nullTexID);
			try (MemoryStack stack = stackPush()) {
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, stack.ints(0xFFFFFFFF));
			}
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		}

		glBindTexture(GL_TEXTURE_2D, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);

		nk_init(context, NkContextSingleton.ALLOCATOR, null);

		context.clip()
        .copy((handle, text, len) -> {
            if (len == 0) {
                return;
            }

            try (MemoryStack stack = stackPush()) {
                ByteBuffer str = stack.malloc(len + 1);
                memCopy(text, memAddress(str), len);
                str.put(len, (byte)0);

                glfwSetClipboardString(winID, str);
            }
        })
        .paste((handle, edit) -> {
            long text = nglfwGetClipboardString(winID);
            if (text != NULL) {
                nnk_textedit_paste(edit, text, nnk_strlen(text));
            }
        });
	}

	private void internalRender(float frameBufferX, float frameBufferY, float windowWidth, float windowHeight, int AA, int max_vertex_buffer, int max_element_buffer) {

		try (MemoryStack stack = stackPush()) {

			// setup global state
			glEnable(GL_BLEND);
			glBlendEquation(GL_FUNC_ADD);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glDisable(GL_CULL_FACE);
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_SCISSOR_TEST);
			glActiveTexture(GL_TEXTURE0);

			// setup program
			glUseProgram(prog);
			glUniform1i(uniform_tex, 0);
			glUniformMatrix4fv(
				uniform_proj,
				false,
				stack
					.floats(
						2.0f / windowWidth,
						0.0f,
						0.0f,
						0.0f,
						0.0f,
						-2.0f / windowHeight,
						0.0f,
						0.0f,
						0.0f,
						0.0f,
						-1.0f,
						0.0f,
						-1.0f,
						1.0f,
						0.0f,
						1.0f));

			// convert from command queue into draw list and draw to screen

			// allocate vertex and element buffer
			glBindVertexArray(vao);
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
			glBufferData(GL_ARRAY_BUFFER, max_vertex_buffer, GL_STREAM_DRAW);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, max_element_buffer, GL_STREAM_DRAW);
			// load draw vertices & elements directly into vertex + element buffer

			ByteBuffer vertices = Objects.requireNonNull(glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, max_vertex_buffer, null));
			ByteBuffer elements = Objects.requireNonNull(glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY, max_element_buffer, null));

			// fill convert configuration
			NkConvertConfig config = NkConvertConfig
				.callocStack(stack)
				.vertex_layout(NkContextSingleton.VERTEX_LAYOUT)
				.vertex_size(20)
				.vertex_alignment(4)
				.null_texture(NkContextSingleton.null_texture)
				.circle_segment_count(22)
				.curve_segment_count(22)
				.arc_segment_count(22)
				.global_alpha(1.0f)
				.shape_AA(AA)
				.line_AA(AA);

			// setup buffers to load vertices and elements
			NkBuffer vbuf = NkBuffer.mallocStack(stack);
			NkBuffer ebuf = NkBuffer.mallocStack(stack);

			nk_buffer_init_fixed(vbuf, vertices/*, max_vertex_buffer*/);
			nk_buffer_init_fixed(ebuf, elements/*, max_element_buffer*/);
			nk_convert(context, cmds, vbuf, ebuf, config);
		}
		glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
		glUnmapBuffer(GL_ARRAY_BUFFER);

		// iterate over and execute each draw command
		float fb_scale_x = frameBufferX / windowWidth;
		float fb_scale_y = frameBufferY / windowHeight;

		long offset = NULL;
		for (NkDrawCommand cmd = nk__draw_begin(context, cmds); cmd != null; cmd = nk__draw_next(cmd, cmds, context)) {
			if (cmd.elem_count() == 0) {
				continue;
			}
			glBindTexture(GL_TEXTURE_2D, cmd.texture().id());
			glScissor(
				(int) (cmd.clip_rect().x() * fb_scale_x),
				(int) ((windowHeight - (int) (cmd.clip_rect().y() + cmd.clip_rect().h())) * fb_scale_y),
				(int) (cmd.clip_rect().w() * fb_scale_x),
				(int) (cmd.clip_rect().h() * fb_scale_y));
			glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset);
			offset += cmd.elem_count() * 2;
		}
		nk_clear(context);

	}

	@Override
	public void close() {
		hasClosed = true;
		Objects.requireNonNull(context.clip().copy()).free();
		Objects.requireNonNull(context.clip().paste()).free();
		nk_free(context);

		glDetachShader(prog, vert_shdr);
		glDetachShader(prog, frag_shdr);
		glDeleteShader(vert_shdr);
		glDeleteShader(frag_shdr);
		glDeleteProgram(prog);
		glDeleteTextures(NkContextSingleton.default_font.texture().id());
		glDeleteTextures(NkContextSingleton.null_texture.texture().id());
		glDeleteBuffers(vbo);
		glDeleteBuffers(ebo);
		nk_buffer_free(cmds);

		Objects.requireNonNull(NkContextSingleton.default_font.query()).free();
		Objects.requireNonNull(NkContextSingleton.default_font.width()).free();

		Objects.requireNonNull(NkContextSingleton.ALLOCATOR.alloc()).free();
		Objects.requireNonNull(NkContextSingleton.ALLOCATOR.mfree()).free();

	}

}
