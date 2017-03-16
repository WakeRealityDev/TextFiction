package de.onyxbits.textfiction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import android.os.Environment;
import android.util.Log;


public class FileUtil implements Comparator<File> {

	/**
	 * Datadir on the external storage
	 */
	public static final String HOMEDIR = "TextFiction";

	/**
	 * Where the games are stored (relative to the HOMEDIR or app data dir).
	 */
	public static final String GAMEDIR = "games";

	/**
	 * Where the meta-entries for games (Thunderword enhanced) are stored (relative to the HOMEDIR or app data dir).
	 */
	public static final String GAMEMETADIR = "gamesmeta";

	/**
	 * Where the save game files are stored (relative to the HOMEDIR or app data
	 * dir).
	 */
	public static final String SAVEDIR = "savegames";

	/**
	 * Where games store misc data
	 */
	public static final String DATADIR = "gamedata";

	private static final File library;
	private static final File librarymeta;
	private static final File saves;
	private static final File data;

	/**
	 * Just make sure we got all of our directories.
	 */
	static {
		File root = Environment.getExternalStorageDirectory();
		library = new File(new File(root, HOMEDIR), GAMEDIR);
		librarymeta = new File(new File(root, HOMEDIR), GAMEMETADIR);
		saves = new File(new File(root, HOMEDIR), SAVEDIR);
		data = new File(new File(root, HOMEDIR), DATADIR);
	}
	
	public static void ensureDirs(){
		library.mkdirs();
		librarymeta.mkdirs();
		saves.mkdirs();
		data.mkdirs();
	}

	/*
	Merge two arrays. Note: Untested if null passed in for either array.
	 */
	public static File[] concat(File[] a, File[] b) {
		int aLen = a.length;
		int bLen = b.length;
		File[] c = new File[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

	public static File[] listGames() {
		ensureDirs();

		File[] combinedArrays = null;

		File[] libraryFiles = library.listFiles();
		if (libraryFiles != null) {
			combinedArrays = libraryFiles;
		}

		File[] libraryMetaFiles = librarymeta.listFiles();
		if (libraryMetaFiles != null) {
			if (combinedArrays != null) {
				// Merge both arrays.
				combinedArrays = concat(libraryFiles, libraryMetaFiles);
			} else {
				// There is only one list, the librarymeta list.
				combinedArrays = libraryMetaFiles;
			}
		}

		if (combinedArrays == null) {
			// Both lists were empty.
			return new File[0];
		}
		Arrays.sort(combinedArrays);

		return combinedArrays;
	}


	/**
	 * List all the save files for a game
	 * 
	 * @param game
	 *          the game in question
	 * @return list of files in the savegamedir
	 */
	public static File[] listSaveGames(File game) {
		ensureDirs();
		File f = getSaveGameDir(game);
		File[] ret = f.listFiles();
		if (ret == null) {
			return new File[0];
		}
		Arrays.sort(ret, new FileUtil());
		return ret;
	}

	/**
	 * List all the saves for a game
	 * 
	 * @param game
	 *          the game in question
	 * @return the filenames of the save games.
	 */
	public static String[] listSaveName(File game) {
		ensureDirs();
		File[] f = listSaveGames(game);
		String ret[] = new String[f.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = f[i].getName();
		}
		return ret;
	}

	/**
	 * 
	 */
	public FileUtil() {
	}

	/**
	 * Copies files to the library
	 * 
	 * @param src
	 *          the file to copy
	 * @throws IOException
	 *           if something goes wrong
	 */
	public static void importGame(File src) throws IOException {
		ensureDirs();
		File dst = new File(library, src.getName());
		byte[] buf = new byte[1024];
		FileInputStream fin = new FileInputStream(src);
		FileOutputStream fout = new FileOutputStream(dst);

		int len;
		while ((len = fin.read(buf)) > 0) {
			fout.write(buf, 0, len);
		}
		getSaveGameDir(dst).mkdirs();
		fin.close();
		fout.close();
	}

	/**
	 * Fake creates files to the library. Creates a meta game entry as an alternate path to importGame method
	 *
	 * To reduce interference with the original Text Fiction app, this "For Thunderword" fork creates
	 *   in librarymeta instead of library. They will not be visible to the non-enhanced app
	 *
	 * @param src
 	 *          the filename to create (stuff into filesystem)
	 * @param sha256hash
	 *          SHA-256 hash of the file, used as an identifier with external engine providers (Thunderword)
	 * @param entryIFDB
	 *          IFDB key the IF story, cross-reference
	 */
	public static boolean stuffInGame(File src, String sha256hash, String entryIFDB) {
		ensureDirs();
		File dst = new File(librarymeta, src.getName());
		try {
			FileOutputStream outputStreamWriter = new FileOutputStream(dst);
			String outHash = sha256hash + "\n";
			outputStreamWriter.write(outHash.getBytes());
			if (entryIFDB != null) {
				String outEntryIFDB = entryIFDB + "\n";
				outputStreamWriter.write(outEntryIFDB.getBytes());
			}
			outputStreamWriter.close();
			if (dst.exists()) {
				Log.i("FileUtil", "[stuffIn] " + dst.getPath() + " SHA-256: " + sha256hash + " IFDB " + entryIFDB);
			} else {
				Log.w("FileUtil", "[stuffIn] failed " + dst.getPath() + " SHA-256: " + sha256hash + " IFDB: " + entryIFDB);
			}
		} catch (IOException e) {
			return false;
		}
		getSaveGameDir(dst).mkdirs();
		return true;
	}

	/**
	 * Delete a game and all other files belonging to it.
	 * 
	 * @param game
	 *          the game file
	 */
	public static void deleteGame(File game) {
		ensureDirs();
		File[] lst = getSaveGameDir(game).listFiles();
		for (File f : lst) {
			f.delete();
		}
		lst = getDataDir(game).listFiles();
		for (File f : lst) {
			f.delete();
		}
		getSaveGameDir(game).delete();
		getDataDir(game).delete();
		game.delete();
	}

	/**
	 * Get the directory where a game keeps its savegames
	 * 
	 * @param game
	 *          the game in question
	 * @return a directory for saving games.
	 */
	public static File getSaveGameDir(File game) {
		ensureDirs();
		File ret = new File(saves, game.getName());
		ret.mkdirs();
		return ret;
	}

	/**
	 * Get the directory where a game may keep various (config) data.
	 * 
	 * @param game
	 *          the game in question.
	 * @return a directory for keeping misc data.
	 */
	public static File getDataDir(File game) {
		ensureDirs();
		File ret = new File(data, game.getName());
		ret.mkdirs();
		return ret;
	}

	/**
	 * Strip filename extension from file name
	 * 
	 * @param file
	 *          the file in question
	 * @return basename
	 */
	public static String basename(File file) {
		String tmp = file.getName();
		int idx = tmp.lastIndexOf('.');
		if (idx > 0) {
			return tmp.substring(0, idx);
		}
		else {
			return tmp;
		}
	}

	/**
	 * Read a text file
	 * @param file the file to read
	 * @return its contents as a String
	 * @throws IOException if stuff goes wrong.
	 */
	public static String getContents(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}
		reader.close();
		return stringBuilder.toString();
	}

	@Override
	public int compare(File lhs, File rhs) {
		return Long.valueOf(rhs.lastModified()).compareTo(lhs.lastModified());
	}

}
