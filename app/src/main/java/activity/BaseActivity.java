package activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

import fragment.CategorySelectionFragment;
import ir.cafebazaar.booker.booker.R;

import widget.AvatarView;


public class BaseActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>  {


    public String name;
    public String photoURI;

    public static void start(Activity activity, ActivityOptionsCompat options) {
        Intent starter = getStartIntent(activity);
        ActivityCompat.startActivity(activity, starter, options.toBundle());
    }

    public static void start(Context context) {
        Intent starter = getStartIntent(context);
        context.startActivity(starter);
    }

    @NonNull
    static Intent getStartIntent(Context context) {
        Intent starter = new Intent(context, BaseActivity.class);

        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_base);

        getLoaderManager().initLoader(0, null, this);


        photoURI = "";

        setUpToolbar();
        if (savedInstanceState == null) {
            attachCategoryGridFragment();
        } else {
            setProgressBarVisibility(View.GONE);
        }
        supportPostponeEnterTransition();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_player);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (!photoURI.equals(""))
        {
            final AvatarView avatarView = (AvatarView) toolbar.findViewById(R.id.avatar);
            avatarView.setAvatar(Uri.parse(photoURI));
        }

        //noinspection PrivateResource
        ((TextView) toolbar.findViewById(R.id.title)).setText(name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh: {
                // Todo
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    private void attachCategoryGridFragment() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
        if (!(fragment instanceof CategorySelectionFragment)) {
            fragment = CategorySelectionFragment.newInstance();
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.category_container, fragment)
                .commit();
        setProgressBarVisibility(View.GONE);
    }

    private void setProgressBarVisibility(int visibility) {
        findViewById(R.id.progress).setVisibility(visibility);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arguments) {
        final String SELECTION = "((" +
                ContactsContract.Data.DISPLAY_NAME + " NOTNULL) AND (" +
                ContactsContract.Data.DISPLAY_NAME + " != '' ))";

        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(
                        ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                ProfileQuery.PROJECTION,

                // Select only email addresses.
                SELECTION,
                null,

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        cursor.moveToFirst();
        name = cursor.getString(ProfileQuery.DISPLAY_NAME);
        photoURI= cursor.getString(ProfileQuery.PHOTO_THUMBNAIL_URI);
        setUpToolbar();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.Data.PHOTO_THUMBNAIL_URI ,
                ContactsContract.Contacts.Data.IS_PRIMARY,
        };

        int DISPLAY_NAME = 0;
        int PHOTO_THUMBNAIL_URI = 1;
        int IS_PRIMARY = 2;
    }
}
