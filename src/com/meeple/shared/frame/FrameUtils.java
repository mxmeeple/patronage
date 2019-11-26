package com.meeple.shared.frame;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLDebugMessageCallbackI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.meeple.shared.Delta;
import com.meeple.shared.Tickable;

public class FrameUtils {

	private static Logger logger = Logger.getLogger(FrameUtils.class);

	public static float TWOPI = (float) (Math.PI * 2f);

	public static final class SyncSetSupplier<T> implements Supplier<Set<T>> {

		@Override
		public Set<T> get() {
			return Collections.synchronizedSet(new HashSet<>());
		}

	}

	public static void iterateRunnable(Collection<Runnable> set, boolean remove) {

		if (set != null && !set.isEmpty()) {
			synchronized (set) {
				Iterator<Runnable> i = set.iterator();
				while (i.hasNext()) {
					Runnable r = i.next();
					if (r != null)
						r.run();
					if (r == null || remove) {
						i.remove();
					}
				}
			}
		}

	}

	public static <T> void iterateConsumer(Collection<Consumer<T>> set, T param, boolean remove) {

		if (set != null && !set.isEmpty()) {
			synchronized (set) {
				Iterator<Consumer<T>> i = set.iterator();
				while (i.hasNext()) {
					Consumer<T> r = i.next();
					if (r != null)
						r.accept(param);
					if (r == null || remove) {
						i.remove();
					}

				}
			}
		}

	}

	public static <T1, T2> void iterateBiConsumer(Collection<BiConsumer<T1, T2>> set, T1 param1, T2 param2, boolean remove) {

		if (set != null && !set.isEmpty()) {
			synchronized (set) {
				Iterator<BiConsumer<T1, T2>> i = set.iterator();
				while (i.hasNext()) {
					BiConsumer<T1, T2> r = i.next();
					if (r != null)
						r.accept(param1, param2);

					if (r == null || remove) {
						i.remove();
					}
				}

			}
		}
	}

	public static void iterateTickable(Collection<Tickable> set, Delta delta) {
		if (set != null && !set.isEmpty()) {

			synchronized (set) {
				Iterator<Tickable> i = set.iterator(); // Must be in the synchronized block
				while (i.hasNext()) {
					Tickable t = i.next();
					if (t == null) {
						i.remove();
					}
					Boolean ret = t.apply(delta);
					if (ret == null || ret.booleanValue()) {
						i.remove();
					}
				}
			}

		}
	}

	public static <K, V> void addToListMap(Map<K, List<V>> map, K key, V value, Supplier<List<V>> newList) {
		List<V> values = map.get(key);
		if (values == null) {
			values = newList.get();
			map.put(key, values);
		}
		values.add(value);
	}

	public static <K, V> void addToSetMap(Map<K, Set<V>> map, K key, V value, Supplier<Set<V>> newSet) {
		Set<V> values = map.get(key);
		if (values == null) {
			values = newSet.get();
			map.put(key, values);
		}
		values.add(value);
	}

	public static GLDebugMessageCallbackI defaultDebugMessage = new GLDebugMessageCallbackI() {

		@Override
		public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
			String messageString = MemoryUtil.memUTF8(MemoryUtil.memByteBuffer(message, length));
			String sourceString = "";
			String typeString = "";
			String severityString = "";
			switch (source) {
				case GL46.GL_DEBUG_SOURCE_API:
					sourceString = "API";
					break;

				case GL46.GL_DEBUG_SOURCE_WINDOW_SYSTEM:
					sourceString = "WINDOW SYSTEM";
					break;

				case GL46.GL_DEBUG_SOURCE_SHADER_COMPILER:
					sourceString = "SHADER COMPILER";
					break;

				case GL46.GL_DEBUG_SOURCE_THIRD_PARTY:
					sourceString = "THIRD PARTY";
					break;

				case GL46.GL_DEBUG_SOURCE_APPLICATION:
					sourceString = "APPLICATION";
					break;

				case GL46.GL_DEBUG_SOURCE_OTHER:
					sourceString = "UNKNOWN";
					break;

				default:
					sourceString = "UNKNOWN";
					break;
			}

			switch (severity) {
				case GL46.GL_DEBUG_SEVERITY_HIGH:
					severityString = "HIGH";
					break;

				case GL46.GL_DEBUG_SEVERITY_MEDIUM:
					severityString = "MEDIUM";
					break;

				case GL46.GL_DEBUG_SEVERITY_LOW:
					severityString = "LOW";
					break;

				case GL46.GL_DEBUG_SEVERITY_NOTIFICATION:
					severityString = "NOTIFICATION";
					break;

				default:
					severityString = "UNKNOWN";
					break;
			}

			switch (type) {
				case GL46.GL_DEBUG_TYPE_ERROR:
					typeString = "Error";
					break;
				case GL46.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
					typeString = "Depricated";
					break;
				case GL46.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
					typeString = "Undefined";
					break;
				case GL46.GL_DEBUG_TYPE_PORTABILITY:
					typeString = "Non-portable";
					break;
				case GL46.GL_DEBUG_TYPE_PERFORMANCE:
					typeString = "Performance";
					break;
				case GL46.GL_DEBUG_TYPE_MARKER:
					typeString = "Marker";
					break;
				case GL46.GL_DEBUG_TYPE_PUSH_GROUP:
					typeString = "Push";
					break;
				case GL46.GL_DEBUG_TYPE_POP_GROUP:
					typeString = "Pop";
					break;
				case GL46.GL_DEBUG_TYPE_OTHER:
					typeString = "Other";
					break;
			}

			//discard notifications
			if (severity == GL46.GL_DEBUG_SEVERITY_HIGH || severity == GL46.GL_DEBUG_SEVERITY_MEDIUM) {
				logger.warn("[Window: " + userParam + "][" + severityString + "][" + sourceString + "][" + typeString + "] " + messageString);
			} else if (severity == GL46.GL_DEBUG_SEVERITY_LOW) {
				logger.trace("[Window: " + userParam + "][" + severityString + "][" + sourceString + "][" + typeString + "] " + messageString);
			}
		}
	};

	public static long secondsToNanos(float seconds) {
		return (long) (seconds * (1000 * 1000 * 1000));
	}

	public static float nanosToSeconds(long ticks) {
		return ticks / (float) (1000 * 1000 * 1000);
	}

	public static float nanosToSecondsInacurate(long ticks) {
		return ((ticks >> 25) << 25) / (float) (1000 * 1000 * 1000);
	}

	public static Vector3f getCurrentForwardVector(Matrix4f matrix) {
		Vector3f ret = new Vector3f();
		matrix.getRow(2, ret);
		return new Vector3f(-ret.x, ret.z, -ret.y);
	}

	public static Vector3f getCurrentUpVector(Matrix4f matrix) {
		Vector3f ret = new Vector3f();
		matrix.getRow(1, ret);
		return new Vector3f(ret.x, -ret.z, ret.y);
	}

	public static Vector3f getCurrentRightVector(Matrix4f matrix) {
		Vector3f ret = new Vector3f();
		matrix.getRow(0, ret);
		ret.x = ret.x;
		ret.y = ret.y;
		ret.z = ret.z;
		return new Vector3f(ret.x, -ret.z, -ret.y);
	}

	public static void appendToArray(Number[] data, int offset, Matrix4f matrix) {
		data[offset + 0] = (matrix.m00());
		data[offset + 1] = (matrix.m01());
		data[offset + 2] = (matrix.m02());
		data[offset + 3] = (matrix.m03());
		data[offset + 4] = (matrix.m10());
		data[offset + 5] = (matrix.m11());
		data[offset + 6] = (matrix.m12());
		data[offset + 7] = (matrix.m13());
		data[offset + 8] = (matrix.m20());
		data[offset + 9] = (matrix.m21());
		data[offset + 10] = (matrix.m22());
		data[offset + 11] = (matrix.m23());
		data[offset + 12] = (matrix.m30());
		data[offset + 13] = (matrix.m31());
		data[offset + 14] = (matrix.m32());
		data[offset + 15] = (matrix.m33());
	}

	public static void appendToBuffer(Number[] array, int offset, Matrix3f matrix) {
		array[offset + 0] = (matrix.m00());
		array[offset + 1] = (matrix.m01());
		array[offset + 2] = (matrix.m02());
		array[offset + 3] = (matrix.m10());
		array[offset + 4] = (matrix.m11());
		array[offset + 5] = (matrix.m12());
		array[offset + 6] = (matrix.m20());
		array[offset + 7] = (matrix.m21());
		array[offset + 8] = (matrix.m22());
	}

	public static void appendToBuffer(Number[] array, int offset, Vector4f vector) {
		array[offset + 0] = (vector.x);
		array[offset + 1] = vector.y;
		array[offset + 2] = vector.z;
		array[offset + 3] = vector.w;

	}

	public static void appendToBuffer(Number[] array, int offset, Vector3f vector) {
		array[offset + 0] = vector.x;
		array[offset + 1] = vector.y;
		array[offset + 2] = vector.z;
	}

	public static void appendToList(List<Number> list, Matrix4f matrix) {
		float[] arr = matrix.get(new float[16]);
		for (float f : arr) {
			list.add(f);
		}
	}

	public static void appendToList(List<Number> list, Matrix3f matrix) {
		float[] arr = matrix.get(new float[9]);
		for (float f : arr) {
			list.add(f);
		}
	}

	public static void appendToList(List<Number> list, Vector4f vector) {
		list.add(vector.x);
		list.add(vector.y);
		list.add(vector.z);
		list.add(vector.w);
	}

	public static void appendToList(List<Number> list, Vector3f vector) {
		list.add(vector.x);
		list.add(vector.y);
		list.add(vector.z);
	}

	public static void appendToList(List<Number> list, Vector2f vector) {
		list.add(vector.x);
		list.add(vector.y);
	}

	public static void appendToBuffer(FloatBuffer buffer, Matrix4f matrix) {
		buffer.put(matrix.m00());
		buffer.put(matrix.m01());
		buffer.put(matrix.m02());
		buffer.put(matrix.m03());
		buffer.put(matrix.m10());
		buffer.put(matrix.m11());
		buffer.put(matrix.m12());
		buffer.put(matrix.m13());
		buffer.put(matrix.m20());
		buffer.put(matrix.m21());
		buffer.put(matrix.m22());
		buffer.put(matrix.m23());
		buffer.put(matrix.m30());
		buffer.put(matrix.m31());
		buffer.put(matrix.m32());
		buffer.put(matrix.m33());
	}

	public static void appendToBuffer(FloatBuffer buffer, Matrix3f matrix) {
		buffer.put(matrix.m00());
		buffer.put(matrix.m01());
		buffer.put(matrix.m02());
		buffer.put(matrix.m10());
		buffer.put(matrix.m11());
		buffer.put(matrix.m12());
		buffer.put(matrix.m20());
		buffer.put(matrix.m21());
		buffer.put(matrix.m22());
	}

	public static void appendToBuffer(FloatBuffer buffer, Vector4f vector) {
		buffer.put(vector.x);
		buffer.put(vector.y);
		buffer.put(vector.z);
		buffer.put(vector.w);
	}

	public static void appendToBuffer(FloatBuffer buffer, Vector3f vector) {
		buffer.put(vector.x);
		buffer.put(vector.y);
		buffer.put(vector.z);
	}

	public static void appendToBuffer(FloatBuffer buffer, Vector2f vector) {
		buffer.put(vector.x);
		buffer.put(vector.y);
	}

	public static FloatBuffer toBuffer(MemoryStack stack, Matrix4f matrix) {
		return stack
			.floats(
				matrix.m00(),
				matrix.m01(),
				matrix.m02(),
				matrix.m03(),
				matrix.m10(),
				matrix.m11(),
				matrix.m12(),
				matrix.m13(),
				matrix.m20(),
				matrix.m21(),
				matrix.m22(),
				matrix.m23(),
				matrix.m30(),
				matrix.m31(),
				matrix.m32(),
				matrix.m33());
	}

	public static FloatBuffer toBuffer(MemoryStack stack, Matrix3f matrix) {
		return stack
			.floats(
				matrix.m00(),
				matrix.m01(),
				matrix.m02(),
				matrix.m10(),
				matrix.m11(),
				matrix.m12(),
				matrix.m20(),
				matrix.m21(),
				matrix.m22());
	}

	public static FloatBuffer toBuffer(MemoryStack stack, Vector4f vector) {
		return stack
			.floats(
				vector.x,
				vector.y,
				vector.z,
				vector.w);
	}

	public static FloatBuffer toBuffer(MemoryStack stack, Vector3f vector) {
		return stack
			.floats(
				vector.x,
				vector.y,
				vector.z);
	}

	public static FloatBuffer toBuffer(MemoryStack stack, Vector2f vector) {
		return stack
			.floats(
				vector.x,
				vector.y);
	}

	public static <T> T getOrNull(T[][][] array, int x, int y, int z) {
		if (inArray(array, x, y, z))
			return array[x][y][z];
		return null;
	}

	public static <T> T getOrNull(T[][] array, int x, int y) {
		if (inArray(array, x, y))
			return array[x][y];
		return null;
	}

	public static <T> T getOrNull(T[] array, int x) {
		if (inArray(array, x))
			return array[x];
		return null;
	}

	public static <T> T getOr(T[][][] array, int x, int y, int z, T val) {
		if (inArray(array, x, y, z)) {
			T r = array[x][y][z];
			if (r != null) {
				return r;
			}
		}
		return val;
	}

	public static <T> T getOr(T[][] array, int x, int y, T val) {
		if (inArray(array, x, y)) {
			T r = array[x][y];
			if (r != null) {
				return r;
			}
		}
		return val;
	}

	public static <T> T getOr(T[] array, int x, T val) {
		if (inArray(array, x)) {
			T r = array[x];
			if (r != null) {
				return r;
			}
		}
		return val;
	}

	public static <T> boolean inArray(T[][][] array, int x, int y, int z) {
		int width = array.length;
		int height = array[0].length;
		int depth = array[0][0].length;
		return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
	}

	public static <T> boolean inArray(T[][] array, int x, int y) {
		int width = array.length;
		int height = array[0].length;
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	public static <T> boolean inArray(T[] array, int x) {
		return x >= 0 && x < array.length;
	}

	public static Vector2i toVec2i(Vector2f vec) {
		return new Vector2i((int) vec.x, (int) vec.y);
	}

	public static Vector3i toVec3i(Vector3f vec) {
		return new Vector3i((int) vec.x, (int) vec.y, (int) vec.z);
	}

	public static <T> boolean isNullOr(T value, T compare) {
		return (value != null && value.equals(compare));
	}

	public static <P extends Number, Mid extends Number> int rotationCompare2D(P x1, P y1, Mid midx, Mid midy, P x2, P y2, float radiOffset) {

		double angleA = Math.atan2(y1.floatValue() - midy.floatValue(), x1.floatValue() - midx.floatValue()) + Math.toRadians(180);
		double angleB = Math.atan2(y2.floatValue() - midy.floatValue(), x2.floatValue() - midx.floatValue()) + Math.toRadians(180);

		angleA = angleA + Math.toRadians(radiOffset);
		angleA = angleA % FrameUtils.TWOPI;

		angleB = angleB + Math.toRadians(radiOffset);
		angleB = angleB % FrameUtils.TWOPI;

		if (angleA > angleB) {
			return -1;
		} else if (angleA < angleB) {
			return 1;
		} else {
			return 0;
		}
	}

	public static <T> String writeArray(T[][] array) {
		return writeArray(array, new Function<T, String>() {

			@Override
			public String apply(T t) {
				if (t != null) {
					return (t.toString().charAt(0) + "").toUpperCase();
				} else {
					return " ";
				}
			}
		});
	}

	public static <T> String writeArray(T[][] array, Function<T, String> writeCommand) {

		String msg = "";
		String[] lines = new String[array[0].length];
		for (int i = 0; i < lines.length; i++) {
			lines[i] = "";

		}
		for (int x = 0; x < array.length; x++) {

			for (int y = 0; y < array[x].length; y++) {
				lines[y] += "[" + writeCommand.apply(array[x][y]) + "]";
			}
		}

		for (int i = lines.length - 1; i >= 0; i--) {
			String s = lines[i];
			msg += s + "\r\n";
		}
		return msg;
	}

	public static float getClamped(float min, float curr, float max) {
		return Math.min(Math.max(min, curr), max);
	}

	public static int getClamped(int min, int curr, int max) {
		return Math.min(Math.max(min, curr), max);
	}

	public static void rotateThis(Vector2f vec, float angle) {

		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cosFromSin(sin, angle);
		float x = vec.x * cos - vec.y * sin;
		float y = vec.x * sin + vec.y * cos;
		vec.set(x, y);
	}

	public static Vector2f rotateNew(Vector2f vec, float angle) {

		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cosFromSin(sin, angle);
		float x = vec.x * cos - vec.y * sin;
		float y = vec.x * sin + vec.y * cos;
		return new Vector2f(x, y);
	}

}
