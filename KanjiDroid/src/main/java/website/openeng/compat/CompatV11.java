
package website.openeng.compat;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;

import website.openeng.anki.KanjiActivity;

import java.text.Normalizer;

import timber.log.Timber;

/** Implementation of {@link Compat} for SDK level 11 (Honeycomb) */
@TargetApi(11)
public class CompatV11 extends CompatV9 implements Compat {
    @Override
    public void setAlpha(View view, float alpha) {
        view.setAlpha(alpha);
    }

    /**
     * Restart the activity and discard old backstack, creating it new from the heirarchy in the manifest
     */
    public void restartActivityInvalidateBackstack(KanjiActivity activity) {
        Timber.i("KanjiActivity -- restartActivityInvalidateBackstack()");
        Intent intent = new Intent();
        intent.setClass(activity, activity.getClass());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
        stackBuilder.addNextIntentWithParentStack(intent);
        stackBuilder.startActivities(new Bundle());
        activity.finishWithoutAnimation();
    }

}
