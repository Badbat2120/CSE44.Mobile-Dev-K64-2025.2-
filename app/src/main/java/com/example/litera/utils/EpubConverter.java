package com.example.litera.utils;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EpubConverter {

    private static final String TAG = "EpubConverter";

    public static String convertEpubToHtml(File epubFile) {
        StringBuilder htmlContent = new StringBuilder();

        try {
            ZipFile zipFile = new ZipFile(epubFile);

            // Step 1: Find content.opf file
            ZipEntry opfEntry = findOpfFile(zipFile);
            if (opfEntry == null) {
                Log.e(TAG, "OPF file not found in EPUB");
                return "";
            }

            // Step 2: Parse OPF to get spine and manifest items
            InputStream opfStream = zipFile.getInputStream(opfEntry);
            List<SpineItem> spineItems = parseOpf(opfStream, zipFile, opfEntry.getName());
            opfStream.close();

            // Step 3: Extract and combine HTML content in spine order
            htmlContent.append("<html><head><meta charset=\"utf-8\"><style>body { margin: 5%; line-height: 1.6; font-size: 18px; }</style></head><body>");

            for (SpineItem item : spineItems) {
                ZipEntry htmlEntry = zipFile.getEntry(item.href);
                if (htmlEntry != null) {
                    InputStream htmlStream = zipFile.getInputStream(htmlEntry);
                    String content = streamToString(htmlStream);
                    htmlStream.close();

                    // Extract body content from HTML
                    content = extractBodyContent(content);
                    htmlContent.append(content);
                }
            }

            htmlContent.append("</body></html>");
            zipFile.close();

        } catch (Exception e) {
            Log.e(TAG, "Error converting EPUB to HTML", e);
            return "";
        }

        return htmlContent.toString();
    }

    private static ZipEntry findOpfFile(ZipFile zipFile) {
        // First, try to find container.xml
        try {
            ZipEntry containerEntry = zipFile.getEntry("META-INF/container.xml");
            if (containerEntry != null) {
                InputStream containerStream = zipFile.getInputStream(containerEntry);
                String opfPath = parseContainerXml(containerStream);
                containerStream.close();

                if (opfPath != null) {
                    return zipFile.getEntry(opfPath);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding OPF file via container.xml", e);
        }

        // If container.xml approach fails, search for .opf files
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".opf")) {
                return entry;
            }
        }

        return null;
    }

    private static String parseContainerXml(InputStream inputStream) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.getName().equals("rootfile")) {
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        if (parser.getAttributeName(i).equals("full-path")) {
                            return parser.getAttributeValue(i);
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing container.xml", e);
        }

        return null;
    }

    private static List<SpineItem> parseOpf(InputStream inputStream, ZipFile zipFile, String opfPath) throws XmlPullParserException, IOException {
        List<SpineItem> spineItems = new ArrayList<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(inputStream, null);

        // First pass: build manifest map
        int eventType = parser.getEventType();

        String baseDir = "";
        if (opfPath.contains("/")) {
            baseDir = opfPath.substring(0, opfPath.lastIndexOf('/') + 1);
        }

        // Identify manifest items
        List<ManifestItem> manifestItems = new ArrayList<>();
        List<String> spineItemRefs = new ArrayList<>();

        boolean inSpine = false;
        boolean inManifest = false;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                if (tagName.equals("manifest")) {
                    inManifest = true;
                } else if (tagName.equals("spine")) {
                    inSpine = true;
                } else if (inManifest && tagName.equals("item")) {
                    // Process manifest item
                    String id = null;
                    String href = null;
                    String mediaType = null;

                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attrName = parser.getAttributeName(i);
                        if (attrName.equals("id")) {
                            id = parser.getAttributeValue(i);
                        } else if (attrName.equals("href")) {
                            href = parser.getAttributeValue(i);
                        } else if (attrName.equals("media-type")) {
                            mediaType = parser.getAttributeValue(i);
                        }
                    }

                    if (id != null && href != null) {
                        ManifestItem item = new ManifestItem();
                        item.id = id;
                        item.href = baseDir + href;
                        item.mediaType = mediaType;
                        manifestItems.add(item);
                    }
                } else if (inSpine && tagName.equals("itemref")) {
                    // Process spine itemref
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        if (parser.getAttributeName(i).equals("idref")) {
                            spineItemRefs.add(parser.getAttributeValue(i));
                            break;
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String tagName = parser.getName();
                if (tagName.equals("manifest")) {
                    inManifest = false;
                } else if (tagName.equals("spine")) {
                    inSpine = false;
                }
            }

            eventType = parser.next();
        }

        // Build spine items using manifest and spine references
        for (String idref : spineItemRefs) {
            for (ManifestItem item : manifestItems) {
                if (item.id.equals(idref) && (item.mediaType == null ||
                        item.mediaType.contains("html") ||
                        item.mediaType.contains("xhtml"))) {
                    SpineItem spineItem = new SpineItem();
                    spineItem.id = item.id;
                    spineItem.href = item.href;
                    spineItems.add(spineItem);
                    break;
                }
            }
        }

        return spineItems;
    }

    private static String streamToString(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, bytesRead, "UTF-8"));
        }

        return builder.toString();
    }

    private static String extractBodyContent(String html) {
        // Simple body content extraction
        int bodyStart = html.indexOf("<body");
        int contentStart = html.indexOf(">", bodyStart);
        int bodyEnd = html.lastIndexOf("</body>");

        if (bodyStart != -1 && contentStart != -1 && bodyEnd != -1) {
            return html.substring(contentStart + 1, bodyEnd);
        }

        return html;
    }

    // Helper classes for EPUB parsing
    private static class ManifestItem {
        String id;
        String href;
        String mediaType;
    }

    private static class SpineItem {
        String id;
        String href;
    }
}