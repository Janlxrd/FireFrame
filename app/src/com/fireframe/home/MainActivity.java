package com.fireframe.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewTreeObserver;

// FireFrame as an app: the Home screen in a window of its own. See HomeScreen for everything else.
public class MainActivity extends Activity {
    HomeScreen screen;
    // Whether FireFrame's own window is the one in front. HomeWatcher's boot guard reads it.
    static volatile boolean inFront;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        screen = new HomeScreen(this);
        screen.applyExtras(getIntent());
        setContentView(screen);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        screen.applyExtras(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        inFront = true;
        HomeService.pausedUntil = 0;
        HomeWatcher.start(this);
        screen.onShown(false);
        // Once this has drawn, the overlay covering Amazon's launcher can step aside. Not sooner, or
        // Amazon's launcher shows through for a moment.
        screen.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                screen.getViewTreeObserver().removeOnPreDrawListener(this);
                screen.postDelayed(HomeService::appShown, 150);
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        inFront = false;
        super.onPause();
    }

    // Fire TV starts on Amazon's home screen; this opens FireFrame as soon as the TV has booted, or FireFrame has updated.
    // On Fire OS 6, HomeWatcher takes it from here: it covers Amazon's home screen at once and keeps
    // bringing FireFrame back for the first seconds, since Amazon's launcher can still open after it.
    public static class Boot extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            // Before the user's storage is unlocked FireFrame can't read its settings: BOOT_COMPLETED follows.
            if (android.os.Build.VERSION.SDK_INT >= 24
                    && !((android.os.UserManager) c.getSystemService(Context.USER_SERVICE)).isUserUnlocked()) return;
            if (HomeWatcher.boot(c)) return;
            c.startActivity(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
