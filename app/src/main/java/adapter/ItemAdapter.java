/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import helper.ApiLevelHelper;
import helper.GRPCHelper;
import ir.cafebazaar.booker.booker.R;
import ir.cafebazaar.booker.proto.nano.CategoriesGetReply;
import ir.cafebazaar.booker.proto.nano.CategoriesGetRequest;
import ir.cafebazaar.booker.proto.nano.CategoryItemsGetReply;
import ir.cafebazaar.booker.proto.nano.CategoryItemsGetRequest;
import ir.cafebazaar.booker.proto.nano.ResourcesGrpc;
import model.Category;
import model.Item;
import model.Theme;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    public static final String DRAWABLE = "drawable";
    private static final String ICON_CATEGORY = "icon_category_";
    private final Resources mResources;
    private final String mPackageName;
    private final LayoutInflater mLayoutInflater;
    private final Activity mActivity;
    private final String mCategoryName;
    private List<Item> mItems = new ArrayList<Item>(3);

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }

    public ItemAdapter(Activity activity, String categoryName) {
        mActivity = activity;
        mResources = mActivity.getResources();
        mPackageName = mActivity.getPackageName();
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());
        new UpdateItemsTask().execute();
        mCategoryName = categoryName;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mLayoutInflater
                .inflate(R.layout.item_category, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Item item = mItems.get(position);
        Theme theme = item.getTheme();

        holder.title.setText(item.getName());
        holder.title.setTextColor(getColor(theme.getTextPrimaryColor()));
        holder.title.setBackgroundColor(getColor(theme.getPrimaryColor()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onClick(v, position);
            }
        });
    }

    public Item getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getId().hashCode();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private class UpdateItemsTask extends AsyncTask<Void, Void, Boolean> {
        protected Boolean doInBackground(Void... v) {
            Log.d(ItemAdapter.class.getSimpleName(), "UpdateCategoriesTask :: doInBackground :: 1");

            try {
                Log.d(ItemAdapter.class.getSimpleName(), "Update categories task :: before getResourcesGrpc");
                ResourcesGrpc.ResourcesBlockingStub grpcService = GRPCHelper.getInstance().getResourcesGrpc();

                CategoryItemsGetRequest req = new CategoryItemsGetRequest();
                req.requestProperties = GRPCHelper.newRPWithDeviceInfo();
                req.name = mCategoryName;
                Log.d(ItemAdapter.class.getSimpleName(), "Update categories task :: before getCategoryItems");
                CategoryItemsGetReply reply = grpcService.getCategoryItems(req);

                mItems.clear();
                for (ir.cafebazaar.booker.proto.nano.Item i : reply.items) {
                    mItems.add(new Item(i.name, i.name, Theme.green));
                }
                Log.d(ItemAdapter.class.getSimpleName(), "Update categories task :: serverVersion=" + reply.replyProperties.serverVersion);
                return true;
            } catch (Exception e) {
                Log.e(ItemAdapter.class.getSimpleName(), "Exception while getting grpcService", e);
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                notifyDataSetChanged();
            }
        }
    }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    /**
     * Loads an icon that indicates that a category has already been solved.
     *
     * @param item The solved category to display.
     * @param categoryImageResource The category's identifying image.
     * @return The icon indicating that the category has been solved.
     */
    private Drawable loadSolvedIcon(Item item, int categoryImageResource) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            return loadSolvedIconLollipop(item, categoryImageResource);
        }
        return loadSolvedIconPreLollipop(item, categoryImageResource);
    }

    @NonNull
    private LayerDrawable loadSolvedIconLollipop(Item item, int categoryImageResource) {
        final Drawable categoryIcon = loadTintedCategoryDrawable(item, categoryImageResource);
        Drawable[] layers = new Drawable[]{categoryIcon}; // ordering is back to front
        return new LayerDrawable(layers);
    }

    private Drawable loadSolvedIconPreLollipop(Item item, int categoryImageResource) {
        return loadTintedCategoryDrawable(item, categoryImageResource);
    }

    /**
     * Loads and tints a drawable.
     *
     * @param item The category providing the tint color
     * @param categoryImageResource The image resource to tint
     * @return The tinted resource
     */
    private Drawable loadTintedCategoryDrawable(Item item, int categoryImageResource) {
        final Drawable categoryIcon = ContextCompat
                .getDrawable(mActivity, categoryImageResource).mutate();
        return wrapAndTint(categoryIcon, item.getTheme().getPrimaryColor());
    }

    private Drawable wrapAndTint(Drawable done, @ColorRes int color) {
        Drawable compatDrawable = DrawableCompat.wrap(done);
        //DrawableCompat.setTint(compatDrawable, getColor(color));
        return compatDrawable;
    }

    /**
     * Convenience method for color loading.
     *
     * @param colorRes The resource id of the color to load.
     * @return The loaded color.
     */
    private int getColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(mActivity, colorRes);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView icon;
        final TextView title;

        public ViewHolder(View container) {
            super(container);
            icon = (ImageView) container.findViewById(R.id.category_icon);
            title = (TextView) container.findViewById(R.id.category_title);
        }
    }
}
