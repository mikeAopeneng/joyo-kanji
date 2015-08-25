package website.openeng.kandroidpkg.dialogs;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import website.openeng.kandroidpkg.BackupManager;
import website.openeng.kandroidpkg.CollectionHelper;
import website.openeng.kandroidpkg.DeckPicker;
import website.openeng.kandroidpkg.R;

public class DeckPickerBackupNoSpaceLeftDialog extends DialogFragment {
    public static DeckPickerBackupNoSpaceLeftDialog newInstance() {
        DeckPickerBackupNoSpaceLeftDialog f = new DeckPickerBackupNoSpaceLeftDialog();
        return f;        
    }
    
    @Override
    public MaterialDialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        long space = BackupManager.getFreeDiscSpace(CollectionHelper.getCollectionPath(getActivity()));
        return new MaterialDialog.Builder(getActivity())
                .title(res.getString(R.string.sd_card_almost_full_title))
                .content(res.getString(R.string.sd_space_warning, space/1024/1024))
                .positiveText(res.getString(R.string.dialog_ok))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        ((DeckPicker) getActivity()).finishWithoutAnimation();
                    }
                })
                .cancelable(true)
                .cancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ((DeckPicker) getActivity()).finishWithoutAnimation();
                    }
                })
                .show();
    }
}