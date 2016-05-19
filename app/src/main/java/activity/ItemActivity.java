package activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import fragment.ItemSelectionFragment;
import helper.GRPCHelper;
import ir.cafebazaar.booker.booker.R;
import ir.cafebazaar.booker.proto.nano.CategoriesGetReply;
import ir.cafebazaar.booker.proto.nano.CategoriesGetRequest;
import ir.cafebazaar.booker.proto.nano.GetReservationReply;
import ir.cafebazaar.booker.proto.nano.GetReservationRequest;
import ir.cafebazaar.booker.proto.nano.PostReservationReply;
import ir.cafebazaar.booker.proto.nano.PostReservationRequest;
import ir.cafebazaar.booker.proto.nano.ReservationGrpc;
import ir.cafebazaar.booker.proto.nano.ReservationInstance;
import ir.cafebazaar.booker.proto.nano.ResourcesGrpc;
import model.Category;
import model.Item;
import model.Theme;
import widget.AvatarView;

public class ItemActivity extends AppCompatActivity {

    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_USER = "user";

    public Item item;

    public String name;
    public String photoURI;

    private TextView textView;
    private EditText editTextFrom, editTextTo;

    public static void start(Activity activity, Item item, String name, String photoUri, ActivityOptionsCompat options) {
        Intent starter = getStartIntent(activity, item, name, photoUri);
        ActivityCompat.startActivity(activity, starter, options.toBundle());
    }

    public static void start(Context context, Item item, String name, String photoUri) {
        Intent starter = getStartIntent(context, item, name, photoUri);
        context.startActivity(starter);
    }

    @NonNull
    public static Intent getStartIntent(Context context, Item item, String name, String photoUri) {
        Intent starter = new Intent(context, ItemActivity.class);

        starter.putExtra(EXTRA_CATEGORY, item);
        ArrayList<String> userParams = new ArrayList<String>();
        userParams.add(name);
        userParams.add(photoUri);
        starter.putStringArrayListExtra(EXTRA_USER, userParams);

        return starter;
    }

    private class UpdateReservationTask extends AsyncTask<GetReservationRequest, Void, String> {
        protected String doInBackground(GetReservationRequest... reqs) {
            Log.d(ItemActivity.class.getSimpleName(), "UpdateCategoriesTask :: doInBackground :: 1");
            if (reqs.length == 0) {
                return null;
            }

            GetReservationRequest req = reqs[0];

            try {
                ReservationGrpc.ReservationBlockingStub grpcService = GRPCHelper.getInstance().getReservationGrpc();

                GetReservationReply reply = grpcService.getReservation(req);

                if (reply.reservation == null) {
                    return "Currently not reserved.";
                }
                return "Reserved from " + reply.reservation.startTimestamp + " to " + reply.reservation.endTimestamp + " by " + reply.reservation.userID;
            } catch (Exception e) {
                Log.e(ItemActivity.class.getSimpleName(), "Exception while getting grpcService", e);
                e.printStackTrace();
                return "Oops";
            }
        }

        protected void onPostExecute(String result) {
            textView.setText(result);
        }
    }

    private class PostReservationTask extends AsyncTask<PostReservationRequest, Void, String> {
        protected String doInBackground(PostReservationRequest... reqs) {
            Log.d(ItemActivity.class.getSimpleName(), "PostReservationTask :: doInBackground :: 1");
            if (reqs.length == 0) {
                return null;
            }

            PostReservationRequest req = reqs[0];

            try {
                ReservationGrpc.ReservationBlockingStub grpcService = GRPCHelper.getInstance().getReservationGrpc();

                req.requestProperties = GRPCHelper.newRPWithDeviceInfo();
                PostReservationReply reply = grpcService.postReservation(req);

                if (reply.reservation == null) {
                    return "Currently not reserved.";
                }
                return "Reserved from " + reply.reservation.startTimestamp + " to " + reply.reservation.endTimestamp + " by " + reply.reservation.userID;
            } catch (Exception e) {
                Log.e(ItemActivity.class.getSimpleName(), "Exception while getting grpcService", e);
                e.printStackTrace();
                return "Oops";
            }
        }

        protected void onPostExecute(String result) {
            Toast.makeText(ItemActivity.this.getApplicationContext(), result, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        textView = (TextView) findViewById(R.id.textViewStatus);
        item = getIntent().getParcelableExtra(EXTRA_CATEGORY);
        ArrayList<String> user = getIntent().getStringArrayListExtra(EXTRA_USER);
        photoURI = user.get(1);
        name = user.get(0);

        editTextFrom = (EditText) findViewById(R.id.editTextFrom);
        editTextTo = (EditText) findViewById(R.id.editTextTo);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostReservationRequest req = new PostReservationRequest();
                req.requestProperties = GRPCHelper.newRPWithDeviceInfo();
                req.objectURI = item.getId();
                req.reservation = new ReservationInstance();
                req.reservation.userID = name;
                req.reservation.startTimestamp = Integer.parseInt("" + editTextFrom.getText());
                req.reservation.endTimestamp = Integer.parseInt("" + editTextTo.getText());
                new PostReservationTask().execute(req);
            }
        });

        setUpToolbar();
        supportPostponeEnterTransition();

        GetReservationRequest req = new GetReservationRequest();
        req.requestProperties = GRPCHelper.newRPWithDeviceInfo();
        req.timestamp = 500;
        req.objectURI = item.getId();
        new UpdateReservationTask().execute(req);
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
