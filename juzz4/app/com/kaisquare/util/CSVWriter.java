/*
 * CSVWriter.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * A class for formatting and writing a CSV file to an output stream.
 *
 * @author Tan Yee Fan
 */
public class CSVWriter {
	private OutputStream outputStream;

	/**
	 * Creates a new CSV file writer.
	 *
	 * @param outputStream Output stream.
	 */
	public CSVWriter(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
	 * Writes a line to the CSV file. The input list of items will be
	 * written out comma-separated.
	 *
	 * @param list List of items to be written.
	 * @throws IOException If the CSV file could not be written.
	 */
	public void writeLine(List<Object> list) throws IOException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Object item: list) {
			if (first)
				first = false;
			else
				builder.append(',');
			String string = (item != null ? String.valueOf(item) : "");
			string = '"' + string.replaceAll("\"", "\"\"") + '"';
			builder.append(string);
		}
		builder.append("\r\n");
		String line = builder.toString();
		this.outputStream.write(line.getBytes("UTF-8"));
		this.outputStream.flush();
	}

	/**
	 * Closes the CSV file writer, thereby closing the underlying output
	 * stream as well.
	 */
	public void close() {
		try {
			this.outputStream.close();
		}
		catch (IOException e) {
		}
	}
}

