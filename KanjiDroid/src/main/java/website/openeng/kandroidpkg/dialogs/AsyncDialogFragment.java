package website.openeng.kandroidpkg.dialogs;
import android.content.res.Resources;
import android.os.Message;
import android.support.v4.app.DialogFragment;

import website.openeng.kandroidpkg.KanjiDroidApp;

public abstract class AsyncDialogFragment extends DialogFragment {
    /* provide methods for text to show in notification bar when the DialogFragment
       can't be shown due to the host activity being in stopped state.
       This can happen when the DialogFragment is shown from 
       the onPostExecute() method of an AsyncTask */
    
    public abstract String getNotificationMessage();
    public abstract String getNotificationTitle();

    public Message getDialogHandlerMessage() {
        return null;
    }

    protected Resources res() {
        return KanjiDroidApp.getAppResources();
    }
} 