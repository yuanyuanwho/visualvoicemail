package au.com.wallaceit.voicemail.helper;


import java.io.File;

import android.content.Context;
import android.media.MediaScannerConnection;


public class MediaScannerNotifier {
    public static void notify(Context context, File file) {
        String[] paths = { file.getAbsolutePath() };
        MediaScannerConnection.scanFile(context, paths, null, null);
    }
}
