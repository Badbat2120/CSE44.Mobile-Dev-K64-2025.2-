package com.example.litera.utils;

import android.util.Log;

/**
 * Utility class for handling Google Drive URLs
 */
public class GoogleDriveUtils {
    private static final String TAG = "GoogleDriveUtils";

    /**
     * Convert Google Drive URL to direct download URL that can be used with Glide
     *
     * @param driveUrl Google Drive URL (can be sharing URL or view URL)
     * @return Direct download URL or original URL if conversion failed
     */
    public static String convertToDirect(String driveUrl) {
        if (driveUrl == null || driveUrl.isEmpty()) {
            Log.d(TAG, "Empty URL provided");
            return null;
        }

        try {
            Log.d(TAG, "Original Google Drive URL: " + driveUrl);

            // Case 1: URL format - https://drive.google.com/file/d/{fileId}/view
            if (driveUrl.contains("drive.google.com/file/d/")) {
                String fileId = extractFileId(driveUrl);
                if (fileId != null) {
                    String directUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
                    Log.d(TAG, "Converted to direct URL: " + directUrl);
                    return directUrl;
                }
            }

            // Case 2: URL format - https://drive.google.com/open?id={fileId}
            else if (driveUrl.contains("drive.google.com/open?id=")) {
                String fileId = driveUrl.substring(driveUrl.indexOf("id=") + 3);
                if (fileId.contains("&")) {
                    fileId = fileId.substring(0, fileId.indexOf("&"));
                }
                String directUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
                Log.d(TAG, "Converted to direct URL: " + directUrl);
                return directUrl;
            }

            // Case 3: URL format - https://docs.google.com/document/d/{fileId}/edit
            else if (driveUrl.contains("docs.google.com/")) {
                String fileId = extractFileId(driveUrl);
                if (fileId != null) {
                    String directUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
                    Log.d(TAG, "Converted to direct URL: " + directUrl);
                    return directUrl;
                }
            }

            // Case 4: Already a direct URL
            else if (driveUrl.contains("drive.google.com/uc?")) {
                Log.d(TAG, "Already a direct URL: " + driveUrl);
                return driveUrl;
            }

            // Case 5: Google Drive thumbnail URL
            else if (driveUrl.contains("drive.google.com/thumbnail?")) {
                Log.d(TAG, "Already a thumbnail URL: " + driveUrl);
                return driveUrl;
            }

            // Case 6: Sharing URL format - https://drive.google.com/drive/folders/{folderId}?usp=sharing
            else if (driveUrl.contains("drive.google.com/drive/folders/")) {
                // We can't directly access a folder, only files
                Log.w(TAG, "Folder URL can't be converted to direct download: " + driveUrl);
                return null;
            }

            // Case 7: URL is already a web content URL
            else if (driveUrl.startsWith("http") && (
                    driveUrl.endsWith(".jpg") || driveUrl.endsWith(".jpeg") ||
                            driveUrl.endsWith(".png") || driveUrl.endsWith(".gif") ||
                            driveUrl.contains("/image/") || driveUrl.contains("/photo/"))) {
                Log.d(TAG, "Already appears to be a web image URL: " + driveUrl);
                return driveUrl;
            }

            // Could not convert URL
            Log.w(TAG, "Could not convert Google Drive URL: " + driveUrl);
            return driveUrl;

        } catch (Exception e) {
            Log.e(TAG, "Error converting Google Drive URL: " + driveUrl, e);
            return driveUrl;
        }
    }

    /**
     * Extract file ID from various Google Drive URL formats
     */
    private static String extractFileId(String url) {
        try {
            String fileId = null;

            // Format: /file/d/{fileId}/
            if (url.contains("/file/d/")) {
                fileId = url.substring(url.indexOf("/file/d/") + 8);
                if (fileId.contains("/")) {
                    fileId = fileId.substring(0, fileId.indexOf("/"));
                }
            }
            // Format: /document/d/{fileId}/
            else if (url.contains("/document/d/")) {
                fileId = url.substring(url.indexOf("/document/d/") + 12);
                if (fileId.contains("/")) {
                    fileId = fileId.substring(0, fileId.indexOf("/"));
                }
            }
            // Format: /presentation/d/{fileId}/
            else if (url.contains("/presentation/d/")) {
                fileId = url.substring(url.indexOf("/presentation/d/") + 16);
                if (fileId.contains("/")) {
                    fileId = fileId.substring(0, fileId.indexOf("/"));
                }
            }
            // Format: /spreadsheets/d/{fileId}/
            else if (url.contains("/spreadsheets/d/")) {
                fileId = url.substring(url.indexOf("/spreadsheets/d/") + 16);
                if (fileId.contains("/")) {
                    fileId = fileId.substring(0, fileId.indexOf("/"));
                }
            }

            if (fileId != null && fileId.contains("?")) {
                fileId = fileId.substring(0, fileId.indexOf("?"));
            }

            return fileId;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting file ID from URL: " + url, e);
            return null;
        }
    }
}