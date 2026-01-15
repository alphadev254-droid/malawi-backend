package mw.nwra.ewaterpermit.util;

import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
	public static final String MIME_APPLICATION_ZIP = "application/zip";
	public static final String MIME_APPLICATION_X_GZIP = "application/x-gzip";

	public static final String MIME_APPLICATION_MSWORD = "application/msword";
	public static final String MIME_APPLICATION_PDF = "application/pdf";
	public static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";

	public static final String MIME_APPLICATION_X_TAR = "application/x-tar";
	public static final String MIME_APPLICATION_X_RAR_COMPRESSED = "application/x-rar-compressed";

	public static final String MIME_IMAGE_BMP = "image/bmp";
	public static final String MIME_IMAGE_CGM = "image/cgm";
	public static final String MIME_IMAGE_GIF = "image/gif";
	public static final String MIME_IMAGE_IEF = "image/ief";
	public static final String MIME_IMAGE_JPEG = "image/jpeg";
	public static final String MIME_IMAGE_JPG = "image/jpg";
	public static final String MIME_IMAGE_JPE = "image/jpe";
	public static final String MIME_IMAGE_TIFF = "image/tiff";
	public static final String MIME_IMAGE_PNG = "image/png";

	public static final String MIME_IMAGE_X_PORTABLE_ANYMAP = "image/x-portable-anymap";
	public static final String MIME_IMAGE_X_PORTABLE_BITMAP = "image/x-portable-bitmap";
	public static final String MIME_IMAGE_X_PORTABLE_GRAYMAP = "image/x-portable-graymap";
	public static final String MIME_IMAGE_X_PORTABLE_PIXMAP = "image/x-portable-pixmap";

	public static final String MIME_TEXT_PLAIN = "text/plain";
	static final String MIME_TEXT_RTF = "text/rtf";
	public static final String MIME_TEXT_HTML = "text/html";
	public static final String MIME_VIDEO_MPEG = "video/mpeg";
	public static final String MIME_APPLICATION_VND_MSEXCEL = "application/vnd.ms-excel";

	private static Map<String, String> mimeTypeMapping = new HashMap<String, String>(30);

	static {
		mimeTypeMapping.put(MIME_IMAGE_JPEG, "jpeg");
		mimeTypeMapping.put(MIME_IMAGE_JPG, "jpg");
		mimeTypeMapping.put(MIME_IMAGE_JPE, "jpe");
		mimeTypeMapping.put(MIME_IMAGE_PNG, "png");
		mimeTypeMapping.put(MIME_IMAGE_GIF, "gif");
		mimeTypeMapping.put(MIME_VIDEO_MPEG, "mpeg");
		mimeTypeMapping.put(MIME_VIDEO_MPEG, "mpg");
		mimeTypeMapping.put(MIME_VIDEO_MPEG, "mpe");
		mimeTypeMapping.put(MIME_APPLICATION_MSWORD, "doc");
		mimeTypeMapping.put(MIME_APPLICATION_VND_MSEXCEL, "xls");
		mimeTypeMapping.put(MIME_APPLICATION_PDF, "pdf");
		mimeTypeMapping.put(MIME_IMAGE_TIFF, "tiff");
		mimeTypeMapping.put(MIME_IMAGE_TIFF, "tif");
		mimeTypeMapping.put(MIME_TEXT_PLAIN, "txt");
		mimeTypeMapping.put(MIME_TEXT_RTF, "rtf");

		mimeTypeMapping.put(MIME_APPLICATION_ZIP, "zip");
		mimeTypeMapping.put(MIME_APPLICATION_X_RAR_COMPRESSED, "rar");
		mimeTypeMapping.put(MIME_APPLICATION_X_GZIP, "gzip");
	}

	/**
	 * Returns the corresponding MIME type to the given extension. If no MIME type
	 * was found it returns 'application/octet-stream' type.
	 */
	public static String getFileExtension(String mimeType) {
		return lookupMimeType(mimeType);
	}

	/**
	 * Simply returns MIME type or <code>null</code> if no type is found.
	 */
	public static String lookupMimeType(String mimeType) {
		return mimeTypeMapping.get(mimeType.toLowerCase());
	}
}
