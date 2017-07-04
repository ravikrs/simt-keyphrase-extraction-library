package de.rwth.i9.simt.ke.lib.adt;

/**
 * Provides an immutable pair of two types.
 *
 * @param <T>
 * @param <S>
 */
public class Pair<T, S> {

	T x;
	S y;

	public Pair(T x, S y) {
		this.x = x;
		this.y = y;
	}

	public T getX() {
		return this.x;
	}

	public S getY() {
		return this.y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof Pair<?, ?>)) {
			return false;
		}

		Pair<?, ?> iobj = (Pair<?, ?>) obj;

		return ((x == null && iobj.x == null) || (x != null && x.equals(iobj.x)))
				&& ((y == null && iobj.y == null) || (y != null && y.equals(iobj.y)));
	}

	/**
	 * The implementation idea belongs to Joshua Bloch, Effective Java.
	 */
	public int hashCodeForObject(Object obj) {
		int result = 17;

		if (obj == null)
			;

		else if (obj instanceof Integer || obj instanceof Byte || obj instanceof Character || obj instanceof Short)
			result = 31 * result + (Integer) obj;

		else if (obj instanceof Long)
			result = 31 * result + (int) ((Long) obj ^ ((Long) obj >>> 32));

		else if (obj instanceof Float)
			result = 31 * result + Float.floatToIntBits((Float) obj);

		else if (obj instanceof Double)
			result = 31 * result + (int) Double.doubleToLongBits((Double) obj);

		else if (obj instanceof Boolean)
			result = 31 * result + ((Boolean) obj ? 0 : 1);

		else if (obj.getClass().isArray()) {
			Object[] array = (Object[]) obj;
			for (int i = 0; i < array.length; i++)
				result = 31 * result + array[i].hashCode();
		}

		else
			// some object
			result = 31 * result + obj.hashCode();

		return result;
	}

	@Override
	public int hashCode() {
		return hashCodeForObject(this.x) + hashCodeForObject(this.y);
	}

}
