package com.example.litera.utils;

public class GoogleDriveUtils {

    /**
     * Chuyển đổi URL Google Drive chia sẻ thành URL trực tiếp để tải xuống
     * @param driveUrl URL Google Drive (dạng https://drive.google.com/file/d/FILE_ID/view)
     * @return URL trực tiếp để tải xuống
     */
    public static String convertToDirect(String driveUrl) {
        if (driveUrl == null || driveUrl.isEmpty()) {
            return null;
        }

        // Nếu đã là URL trực tiếp, trả về ngay
        if (driveUrl.contains("export=download")) {
            return driveUrl;
        }

        // Trích xuất File ID từ URL Google Drive
        String fileId = extractFileId(driveUrl);
        if (fileId == null) {
            return driveUrl; // Không thể trích xuất ID, trả về URL gốc
        }

        // Tạo URL trực tiếp để tải xuống
        return "https://drive.google.com/uc?export=download&id=" + fileId;
    }

    /**
     * Trích xuất File ID từ Google Drive URL
     */
    private static String extractFileId(String driveUrl) {
        // Mẫu URL tiêu chuẩn: https://drive.google.com/file/d/FILE_ID/view
        if (driveUrl.contains("/file/d/")) {
            String[] parts = driveUrl.split("/file/d/");
            if (parts.length > 1) {
                String fileId = parts[1].split("/")[0];
                return fileId;
            }
        }

        // Mẫu URL thay thế: https://drive.google.com/open?id=FILE_ID
        if (driveUrl.contains("id=")) {
            String[] parts = driveUrl.split("id=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        }

        return null;
    }
}