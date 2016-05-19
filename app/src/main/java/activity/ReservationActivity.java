package activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import fragment.CategorySelectionFragment;
import fragment.ItemSelectionFragment;
import ir.cafebazaar.booker.booker.R;
import model.Category;
import widget.AvatarView;

public class ReservationActivity extends AppCompatActivity {

    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_USER = "user";

    public Category category;

    public String name;
    public String photoURI;

    public static void start(Activity activity, Category category, String name, String photoUri, ActivityOptionsCompat options) {
        Intent starter = getStartIntent(activity, category, name, photoUri);
        ActivityCompat.startActivity(activity, starter, options.toBundle());
    }

    public static void start(Context context, Category category, String name, String photoUri) {
        Intent starter = getStartIntent(context, category, name, photoUri);
        context.startActivity(starter);
    }

    @NonNull
    public static Intent getStartIntent(Context context, Category category, String name, String photoUri) {
        Intent starter = new Intent(context, ReservationActivity.class);

        starter.putExtra(EXTRA_CATEGORY, category);
        ArrayList<String> userParams = new ArrayList<String>();
        userParams.add(name);
        userParams.add(photoUri);
        starter.putStringArrayListExtra(EXTRA_USER, userParams);

        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        ArrayList<String> user = getIntent().getStringArrayListExtra(EXTRA_USER);
        photoURI = user.get(1);
        name = user.get(0);

        category = getIntent().getParcelableExtra(EXTRA_CATEGORY);

        setUpToolbar();
        if (savedInstanceState == null) {
            attachCategoryGridFragment();
        } else {
            setProgressBarVisibility(false);
        }
        supportPostponeEnterTransition();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void attachCategoryGridFragment() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
        if (!(fragment instanceof ItemSelectionFragment)) {
            fragment = ItemSelectionFragment.newInstance();
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.category_container, fragment)
                .commit();
        setProgressBarVisibility(false);
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


}
