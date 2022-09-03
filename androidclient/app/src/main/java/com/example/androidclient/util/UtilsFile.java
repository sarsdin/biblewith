package com.example.androidclient.util;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;

public class UtilsFile {


    private final static String PUBLIC_DOWNLOAD_PATH = "content://downloads/public_downloads";


    private final static String EXTERNAL_STORAGE_DOCUMENTS_PATH = "com.android.externalstorage.documents";


    private final static String DOWNLOAD_DOCUMENTS_PATH = "com.android.providers.downloads.documents";


    private final static String MEDIA_DOCUMENTS_PATH = "com.android.providers.media.documents";


    private final static String PHOTO_CONTENTS_PATH = "com.google.android.apps.photos.content";


    private Boolean isExternalStorageDocument(Uri uri) {
        return EXTERNAL_STORAGE_DOCUMENTS_PATH.equals(uri.getAuthority());

    }
    private Boolean isPublicDocument(Uri uri) {
        return PUBLIC_DOWNLOAD_PATH.equals(uri.getAuthority());

    }


    private Boolean isDownloadsDocument(Uri uri) {
        return DOWNLOAD_DOCUMENTS_PATH.equals(uri.getAuthority());

    }

    private Boolean isMediaDocument(Uri uri) {
        return MEDIA_DOCUMENTS_PATH.equals(uri.getAuthority());
    }


    private Boolean isGooglePhotosUri(Uri uri) {
        return MEDIA_DOCUMENTS_PATH.equals(uri.getAuthority());

    }
    private Boolean isPhotoContentUri(Uri uri) {
        return PHOTO_CONTENTS_PATH.equals(uri.getAuthority());

    }



    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        //String column = "_data" REMOVED IN FAVOR OF NULL FOR ALL
        //String projection = arrayOf(column) REMOVED IN FAVOR OF PROJECTION FOR ALL
        try {
            cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e("PathUtils", "Error getting uri for cursor to read file: " + e.getMessage());
        } finally {
            assert cursor != null;
            cursor.close();
        }
        return null;

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public  String getFullPathFromContentUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String filePath="";
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }//non-primary e.g sd card
                else {
                    if (Build.VERSION.SDK_INT > 20) {
                        //getExternalMediaDirs() added in API 21
                        File[] extenal = context.getExternalMediaDirs();
                        for (File f : extenal) {
                            filePath = f.getAbsolutePath();
                            if (filePath.contains(type)) {
                                int endIndex = filePath.indexOf("Android");
                                filePath = filePath.substring(0, endIndex) + split[1];
                            }
                        }
                    }else{
                        filePath = "/storage/" + type + "/" + split[1];
                    }
                    return filePath;
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                String fileName = getDataColumn(context,  uri,null, null);
                String uriToReturn = null;
                if (fileName != null) {

                    uriToReturn = Uri.withAppendedPath(
                            Uri.parse(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()), fileName
                    ).toString();
                }
                return uriToReturn;
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                Cursor cursor = null;
                final String column = "_data";
                final String[] projection = {
                        column
                };

                try {
                    cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                            null);
                    if (cursor != null && cursor.moveToFirst()) {
                        final int column_index = cursor.getColumnIndexOrThrow(column);
                        return cursor.getString(column_index);
                    }
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
                return null;
            }

        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        else if (isPublicDocument(uri)){
            String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(
                    Uri.parse(PUBLIC_DOWNLOAD_PATH), Long.parseLong(id));
            String[] projection = {MediaStore.Images.Media.DATA};
            @SuppressLint("Recycle") Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
        }

        return null;
    }

}