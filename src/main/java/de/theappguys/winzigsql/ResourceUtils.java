/**
 *
 */
package de.theappguys.winzigsql;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import android.content.Context;

/**
 * Util methods to read raw assets and resources
 */
public class ResourceUtils {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static final int BUFFER_SIZE = 2 * 1024;//read in 4K chunks (1 char == 2 byte)


	private ResourceUtils(){}

	/**
	 * Attempts to read the resource with the given id as a utf-8 string.
	 * @throws IOException reading the resource failed (most likely the
	 * resource does not exist or cannot be read as a raw resource)
	 */
	public static String readRawResourceAsString(
			final Context ctx, final int id)
	throws IOException {
		final Reader r =
				new InputStreamReader(new BufferedInputStream(
				ctx.getResources().openRawResource(id)),
				UTF_8);
		try {
		    return readString(r);
		} finally {
			r.close();
		}
	}

	/**
	 * Attempts to read the asset with the given name as a utf-8 string.
	 * @throws IOException reading the asset failed (most likely the given name
	 * was wrong)
	 */
	public static String readAssetAsString(final Context ctx, final String name)
	throws IOException {
		final Reader r = new BufferedReader(new InputStreamReader(
				ctx.getAssets().open(name), UTF_8));
		try {
			return readString(r);
		} finally {
			r.close();
		}
	}

	/**
	 * Reads the contents from the given reader. DOES NOT CLOSE THE STREAM!
	 * @param in reader to read from, not null
	 * @return the content from the reader
	 * @throws IOException reading failed
	 */
	public static String readString(final Reader in) throws IOException {
	    final StringBuilder result = new StringBuilder();
        final char[] buffer = new char[BUFFER_SIZE];
        int read = 0;
        while ((read = in.read(buffer)) != -1) {
            result.append(buffer, 0, read);
        }

        return result.toString();
	}

}
