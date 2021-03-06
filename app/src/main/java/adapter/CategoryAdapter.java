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
import android.widget.Toast;

import helper.GRPCHelper;
import ir.cafebazaar.booker.booker.R;
import ir.cafebazaar.booker.proto.nano.CategoriesGetReply;
import ir.cafebazaar.booker.proto.nano.CategoriesGetRequest;
import ir.cafebazaar.booker.proto.nano.GetReservationReply;
import ir.cafebazaar.booker.proto.nano.GetReservationRequest;
import ir.cafebazaar.booker.proto.nano.ReservationGrpc;
import ir.cafebazaar.booker.proto.nano.ResourcesGrpc;
import model.Category;
import model.Theme;
import helper.ApiLevelHelper;


import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public static final String DRAWABLE = "drawable";
    private static final String ICON_CATEGORY = "icon_category_";
    private final Resources mResources;
    private final String mPackageName;
    private final LayoutInflater mLayoutInflater;
    private final Activity mActivity;
    private List<Category> mCategories = new ArrayList<Category>(3);

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }

    public CategoryAdapter(Activity activity) {
        mActivity = activity;
        mResources = mActivity.getResources();
        mPackageName = mActivity.getPackageName();
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());
        new UpdateCategoriesTask().execute();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mLayoutInflater
                .inflate(R.layout.item_category, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Category category = mCategories.get(position);
        Theme theme = category.getTheme();
        setCategoryIcon(category, holder.icon);

        holder.title.setText(category.getName());
        holder.title.setTextColor(getColor(theme.getTextPrimaryColor()));
        holder.title.setBackgroundColor(getColor(theme.getPrimaryColor()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onClick(v, position);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return mCategories.get(position).getId().hashCode();
    }

    @Override
    public int getItemCount() {
        return mCategories.size();
    }

    public Category getItem(int position) {
        return mCategories.get(position);
    }

    private class UpdateCategoriesTask extends AsyncTask<Void, Void, Boolean> {
        protected Boolean doInBackground(Void... v) {
            Log.d(CategoryAdapter.class.getSimpleName(), "UpdateCategoriesTask :: doInBackground :: 1");

            try {
                ResourcesGrpc.ResourcesBlockingStub grpcService = GRPCHelper.getInstance().getResourcesGrpc();

                CategoriesGetRequest req = new CategoriesGetRequest();
                req.requestProperties = GRPCHelper.newRPWithDeviceInfo();
                CategoriesGetReply reply = grpcService.getCategories(req);

                mCategories.clear();
                for (ir.cafebazaar.booker.proto.nano.Category c : reply.categories) {
                    mCategories.add(new Category(c.name, c.name, Theme.booker));
                }

                return true;
            } catch (Exception e) {
                Log.e(CategoryAdapter.class.getSimpleName(), "Exception while getting grpcService", e);
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

    private void setCategoryIcon(Category category, ImageView icon) {
        final int categoryImageResource = mResources.getIdentifier(
                ICON_CATEGORY + category.getId(), DRAWABLE, mPackageName);
    }

    /**
     * Loads an icon that indicates that a category has already been solved.
     *
     * @param category The solved category to display.
     * @param categoryImageResource The category's identifying image.
     * @return The icon indicating that the category has been solved.
     */
    private Drawable loadSolvedIcon(Category category, int categoryImageResource) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            return loadSolvedIconLollipop(category, categoryImageResource);
        }
        return loadSolvedIconPreLollipop(category, categoryImageResource);
    }

    @NonNull
    private LayerDrawable loadSolvedIconLollipop(Category category, int categoryImageResource) {
        final Drawable categoryIcon = loadTintedCategoryDrawable(category, categoryImageResource);
        Drawable[] layers = new Drawable[]{categoryIcon}; // ordering is back to front
        return new LayerDrawable(layers);
    }

    private Drawable loadSolvedIconPreLollipop(Category category, int categoryImageResource) {
        return loadTintedCategoryDrawable(category, categoryImageResource);
    }

    /**
     * Loads and tints a drawable.
     *
     * @param category The category providing the tint color
     * @param categoryImageResource The image resource to tint
     * @return The tinted resource
     */
    private Drawable loadTintedCategoryDrawable(Category category, int categoryImageResource) {
        final Drawable categoryIcon = ContextCompat
                .getDrawable(mActivity, categoryImageResource).mutate();
        return wrapAndTint(categoryIcon, category.getTheme().getPrimaryColor());
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
