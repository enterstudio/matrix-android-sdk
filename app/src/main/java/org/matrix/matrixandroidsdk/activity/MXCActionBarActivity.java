/*
 * Copyright 2014 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.matrixandroidsdk.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.matrix.androidsdk.MXSession;
import org.matrix.matrixandroidsdk.ConsoleApplication;
import org.matrix.matrixandroidsdk.Matrix;
import org.matrix.matrixandroidsdk.R;
import org.matrix.matrixandroidsdk.adapters.DrawerAdapter;
import org.matrix.matrixandroidsdk.services.EventStreamService;

/**
 * extends ActionBarActivity to manage the rageshake
 */
public class MXCActionBarActivity extends ActionBarActivity {

    // add left sliding menu
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    protected ActionBarDrawerToggle mDrawerToggle;

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);

        // create a "lollipop like " animation
        // not sure it is the save animation curve
        // appcompat does not support (it does nothing)
        //
        // ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(...
        // ActivityCompat.startActivity(activity, new Intent(activity, DetailActivity.class),  options.toBundle());

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            this.overridePendingTransition(R.anim.anim_slide_in_bottom, R.anim.anim_slide_nothing);
        } else {
            // the animation is enabled in the theme
        }
    }

    @Override
    public void finish() {
        super.finish();

        // create a "lollipop like " animation
        // not sure it is the save animation curve
        //
        // ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(...
        // ActivityCompat.startActivity(activity, new Intent(activity, DetailActivity.class),  options.toBundle());
       if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            this.overridePendingTransition(R.anim.anim_slide_nothing, R.anim.anim_slide_out_bottom);
        } else {
            // the animation is enabled in the theme
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU) && (null == getSupportActionBar())) {
            // This is to fix a bug in the v7 support lib. If there is no options menu and you hit MENU, it will crash with a
            // NPE @ android.support.v7.app.ActionBarImplICS.getThemedContext(ActionBarImplICS.java:274)
            // This can safely be removed if we add in menu options on this screen
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ConsoleApplication.setCurrentActivity(null);

        ((ConsoleApplication)getApplication()).startActivityTransitionTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConsoleApplication.setCurrentActivity(this);

        // refresh the push rules when debackgrounding the application
        if (((ConsoleApplication)getApplication()).isInBackground) {
            Matrix matrixInstance =  Matrix.getInstance(getApplicationContext());

            // sanity check
            if (null != matrixInstance) {
                final MXSession session = matrixInstance.getDefaultSession();

                if ((null != session) && (null != session.getDataHandler())) {
                    session.getDataHandler().refreshPushRules();
                }
            }

            EventStreamService.cancelNotificationsForRoomId(null);
        }

        ((ConsoleApplication)getApplication()).stopActivityTransitionTimer();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if ((null != mDrawerToggle) && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == android.R.id.home) {
            // pop the activity to avoid creating a new instance of the parent activity
            this.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Define a sliding menu
     * @param iconResourceIds the icons resource Ids
     * @param textResourceIds the text resources Ids
     */
    protected void addSlidingMenu(Integer[] iconResourceIds, Integer[] textResourceIds) {
        // sanity checks
        if ((null == iconResourceIds) || (null == textResourceIds) || (iconResourceIds.length != textResourceIds.length)) {
            return;
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // check if the dedicated resource exists
        if ((null != mDrawerLayout) && (null != mDrawerList)) {

            // Set the adapter for the list view
            DrawerAdapter adapter = new DrawerAdapter(this, R.layout.adapter_drawer_header, R.layout.adapter_drawer_item);

            for (int index = 0; index < iconResourceIds.length; index++) {
                adapter.add(iconResourceIds[index], getString(textResourceIds[index]));
            }

            mDrawerList.setAdapter(adapter);

            // Set the list's click listener
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                    R.string.action_open,  /* "open drawer" description */
                    R.string.action_close  /* "close drawer" description */
            ) {

                public void onDrawerClosed(View view) {
                }

                public void onDrawerOpened(View drawerView) {
                }
            };

            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);

            // display the home and title button
            if (null != getSupportActionBar()) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (null != mDrawerToggle) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (null != mDrawerToggle) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Run the dedicated sliding menu action
     * @param position selected menu entry
     */
    protected void selectDrawItem(int position) {
        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectDrawItem(position);
        }
    }
}
