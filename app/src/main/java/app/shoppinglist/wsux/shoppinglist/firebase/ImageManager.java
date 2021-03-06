package app.shoppinglist.wsux.shoppinglist.firebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ImageManager {

    private static final String FIREBASE_CACHE_DIR = "firebase";
    private static final String COLLABORATOR_FILE_PATH = "nodpi_user_picture_cache_%s";
    private static final String TASK_FILE_PATH = "nodpi_item_cache_%s_%s_%s";

    private FireBaseManager manager;

    ImageManager(FireBaseManager manager) {
        this.manager = manager;
    }

    private File getFireBaseCache() {
        File firebaseCache = new File(manager.getAppContext().getCacheDir(), FIREBASE_CACHE_DIR);

        if (!firebaseCache.exists()) {
            firebaseCache.mkdir();
        }

        return firebaseCache;
    }

    public Bitmap getPicture(BaseCollectionItem item) {
        File file = getPictureFile(item);

        if (file == null || !file.exists()) {
            return null;
        }

        BitmapFactory.Options options = createBitmapFactoryOptions();
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private BitmapFactory.Options createBitmapFactoryOptions() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        options.inDensity = DisplayMetrics.DENSITY_DEFAULT;

        return options;
    }

    void downloadPicture(BaseCollectionItem item, String pictureUrl) {
        new DownloadPicture(item, pictureUrl).execute("");
    }

    private File getPictureFile(BaseCollectionItem item) {
        if (item instanceof Collaborator) {
            return getPictureFile((Collaborator) item);
        }

        if (item instanceof UserInfo) {
            return getPictureFile((UserInfo) item);
        }

        if (item instanceof ShopTask) {
            return getPictureFile((ShopTask) item);
        }

        return null;

    }

    private File getPictureFile(Collaborator collaborator) {
        return new File(getFireBaseCache(), String.format(COLLABORATOR_FILE_PATH, collaborator.getUserId()));
    }

    private File getPictureFile(UserInfo userInfo) {
        return new File(getFireBaseCache(), String.format(COLLABORATOR_FILE_PATH, userInfo.getUserId()));
    }

    private File getPictureFile(ShopTask shopTask) {

        if (!shopTask.hasPicture()) {
            return null;
        }

        return new File(getFireBaseCache(),
                String.format(
                        TASK_FILE_PATH,
                        shopTask.getInList().getListId(),
                        shopTask.getTaskId(),
                        shopTask.getPictureUrl().hashCode()
                )
        );
    }

    class DownloadPicture extends AsyncTask<String, Void, String> {

        private final String RES_OK = "OK";
        private final String RES_FAIL = null;

        private BaseCollectionItem item;
        private String pictureUrl;
        private File file;

        DownloadPicture(BaseCollectionItem item, String pictureUrl) {
            super();
            this.item = item;
            this.pictureUrl = pictureUrl;

            file = getPictureFile(item);
        }

        /*
         * Note that this function's purpose is to download a file
         * Splitting it to sub-functions will just make it less readable
         */
        private void downloadFile() throws IOException {
            URL url = new URL(pictureUrl);
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            FileOutputStream fileOutput = new FileOutputStream(file);
            byte[] buffer = new byte[1024];

            int bufferLength = 0;
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
            }

            fileOutput.close();
            inputStream.close();
        }

        @Override
        protected String doInBackground(String... params) {

            if (file.exists()) {
                return RES_FAIL;
            }

            try {
                downloadFile();
            } catch (IOException e) {
                return RES_FAIL;
            }

            return RES_OK;
        }

        @Override
        protected void onPostExecute(String result) {
            item.reportMediaDownloaded();
        }
    }

}
