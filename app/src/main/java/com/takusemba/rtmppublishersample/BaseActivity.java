package com.takusemba.rtmppublishersample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import io.reactivex.disposables.CompositeDisposable;
import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

/**
 * <p>
 * Desc：Activity父类
 */
public abstract class BaseActivity extends SwipeBackActivity {

    public CompositeDisposable mDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisposable = new CompositeDisposable();
    }

    public void gotoActivity(Class<? extends Activity> activityClass) {
        startActivity(new Intent(this, activityClass));
    }


    @Override
    protected void onDestroy() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable.clear();
        }

        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();

        //finish回收软键盘
        View dsf = this.getCurrentFocus();
        if (dsf != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(dsf.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
