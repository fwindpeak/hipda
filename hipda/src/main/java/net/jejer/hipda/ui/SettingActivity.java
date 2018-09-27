package net.jejer.hipda.ui;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;

import net.jejer.emsky.R;
import net.jejer.hipda.ui.setting.AboutFragment;
import net.jejer.hipda.ui.setting.BlacklistFragment;
import net.jejer.hipda.ui.setting.SettingMainFragment;
import net.jejer.hipda.ui.setting.SettingNestedFragment;

/**
 * Created by GreenSkinMonster on 2017-06-16.
 */

public class SettingActivity extends SwipeBaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_post);
        mRootView = findViewById(R.id.main_activity_root_view);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar_layout);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle arguments = getIntent().getExtras();

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentById(R.id.main_frame_container) == null) {
            if (arguments == null) {
                SettingMainFragment fragment = new SettingMainFragment();
                fragmentManager.beginTransaction()
                        .add(R.id.main_frame_container, fragment).commit();
            } else if (arguments.containsKey(AboutFragment.TAG_KEY)) {
                AboutFragment fragment = new AboutFragment();
                fragment.setArguments(arguments);
                fragmentManager.beginTransaction()
                        .add(R.id.main_frame_container, fragment).commit();
            } else if (arguments.containsKey(BlacklistFragment.TAG_KEY)) {
                BlacklistFragment fragment = new BlacklistFragment();
                fragment.setArguments(arguments);
                fragmentManager.beginTransaction()
                        .add(R.id.main_frame_container, fragment).commit();
            } else {
                SettingNestedFragment fragment = new SettingNestedFragment();
                fragment.setArguments(arguments);
                fragmentManager.beginTransaction()
                        .add(R.id.main_frame_container, fragment).commit();
            }
        }
    }

}
