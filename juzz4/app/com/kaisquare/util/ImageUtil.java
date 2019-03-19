/*
 * ImageUtil.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Utility methods for images.
 *
 * @author Tan Yee Fan
 */
public class ImageUtil {
	/**
	 * This class is not intended to be instantiated.
	 */
	private ImageUtil() {
	}

	/**
	 * Creates an image that is filled entirely with the specified color.
	 * This image is returned as a buffered image.
	 *
	 * @param width Width of the image, in pixels.
	 * @param height Height of the image, in pixels.
	 * @param color Color of the fill.
	 */
	public static BufferedImage createFilledImage(int width, int height, Color color) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = image.createGraphics();
		g.setColor(color);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return image;
	}

	/**
	 * Creates an image that is filled entirely with the specified color.
	 * This image is returned as a byte array in the specified image format,
	 * such as JPEG and PNG.
	 * <p>
	 * If an error occurred, then {@code null} is returned.
	 *
	 * @param width Width of the image, in pixels.
	 * @param height Height of the image, in pixels.
	 * @param color Color of the fill.
	 * @param format Format of the image, such as {@code jpeg} and
	 *        {@code png}.
	 */
	public static byte[] createFilledImage(int width, int height, Color color, String format) {
		BufferedImage image = createFilledImage(width, height, color);
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, format, outputStream);
			return outputStream.toByteArray();
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns a new image that is a resized version of an existing image.
	 *
	 * @param image Input image.
	 * @param width Width of the output image.
	 * @param height Height of the output image.
	 */
	public static BufferedImage resizeImage(Image image, int width, int height) {
		BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = newImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		return newImage;
	}

	/**
	 * Returns a new image that is a resized version of an existing image.
	 * <p>
	 * If an error occurred, then {@code null} is returned.
	 *
	 * @param bytes Input image.
	 * @param width Width of the output image.
	 * @param height Height of the output image.
	 * @param format Format of the output image, such as {@code jpeg} and
	 *        {@code png}.
	 */
	public static byte[] resizeImage(byte[] bytes, int width, int height, String format) {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
			BufferedImage image = ImageIO.read(inputStream);
			BufferedImage newImage = resizeImage(image, width, height);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(newImage, format, outputStream);
			return outputStream.toByteArray();
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Converts the given image to a buffered image.
	 *
	 * @param image Input image.
	 */
	public static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage)
			return (BufferedImage)image;
		else
			return resizeImage(image, image.getWidth(null), image.getHeight(null));
	}
}

