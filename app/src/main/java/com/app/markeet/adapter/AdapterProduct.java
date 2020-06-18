package com.app.markeet.adapter;

import android.content.Context;
import android.graphics.Paint;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.markeet.R;
import com.app.markeet.data.Constant;
import com.app.markeet.data.DatabaseHandler;
import com.app.markeet.data.SharedPref;
import com.app.markeet.model.Cart;
import com.app.markeet.model.Product;
import com.app.markeet.utils.FaNum;
import com.app.markeet.utils.Tools;
import com.balysv.materialripple.MaterialRippleLayout;

import java.util.ArrayList;
import java.util.List;

public class AdapterProduct extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static String TAG = "adminamin";

    private void refreshCartButton(long product_id, MaterialRippleLayout ly, TextView tv,ImageView ic,DatabaseHandler db) {
        if (db.getCart(product_id) != null) {
            ly.setBackgroundColor(ctx.getResources().getColor(R.color.colorRemoveCart));
            tv.setText(R.string.bt_remove_cart);
            ic.setImageResource(R.drawable.ic_remove);
        } else {
            ly.setBackgroundColor(ctx.getResources().getColor(R.color.colorAddCart));
            tv.setText(R.string.bt_add_cart);
            ic.setImageResource(R.drawable.ic_add);
        }
    }
//    ---------------

    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;

    private List<Product> items = new ArrayList<>();

    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;

    private Context ctx;
    private OnItemClickListener mOnItemClickListener;
    private SharedPref sharedPref;

    public interface OnItemClickListener {
        void onItemClick(View view, Product obj, int position);
        void updateBadge();
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterProduct(Context context, RecyclerView view, List<Product> items) {
        this.items = items;
        ctx = context;
        sharedPref = new SharedPref(ctx);
        lastItemViewDetector(view);
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView name;
        public TextView price;
        private TextView price_strike;
        private RelativeLayout lay_image;
        public ImageView image;
        public RelativeLayout lyt_parent;
        private MaterialRippleLayout lyt_add_cart;
        private TextView tv_add_cart;
        private ImageView ic_add_cart;
        private DatabaseHandler db ;

        public OriginalViewHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.name);
            price = (TextView) v.findViewById(R.id.price);
            price_strike = (TextView) v.findViewById(R.id.price_strike);
            lay_image = (RelativeLayout) v.findViewById(R.id.lay_image);
            image = (ImageView) v.findViewById(R.id.image);
            lyt_parent = (RelativeLayout) v.findViewById(R.id.lyt_parent);
            lyt_add_cart = (MaterialRippleLayout) v.findViewById(R.id.lyt_add_cart);
            tv_add_cart = (TextView) v.findViewById(R.id.tv_add_cart);
            ic_add_cart = (ImageView) v.findViewById(R.id.ic_add_cart);
            db = new DatabaseHandler(ctx);
        }
    }


    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = (ProgressBar) v.findViewById(R.id.progress_loading);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            vh = new OriginalViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Product p = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;
            vItem.name.setText(FaNum.convert(p.name));

            // handle discount view
            if (p.price_discount > 0) {
                vItem.price.setText(Tools.getFormattedPrice(p.price_discount, ctx));
                vItem.price_strike.setText(Tools.getFormattedPrice(p.price, ctx));
                vItem.price_strike.setPaintFlags(vItem.price_strike.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                vItem.price_strike.setVisibility(View.VISIBLE);
            } else {
                vItem.price.setText(Tools.getFormattedPrice(p.price, ctx));
                vItem.price_strike.setVisibility(View.GONE);
            }
            Tools.displayImageOriginal(ctx, vItem.image, Constant.getURLimgProduct(p.image));
            vItem.lay_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(view, p, position);
                    }
                }
            });
            // Add To Cart Method
            refreshCartButton(p.id,vItem.lyt_add_cart,vItem.tv_add_cart,vItem.ic_add_cart,vItem.db);  //313
            vItem.lyt_add_cart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.updateBadge();
                    if (vItem.db.getCart(p.id) != null) {
                        vItem.db.deleteActiveCart(p.id);
//                        Toast.makeText(ctx, R.string.remove_cart, Toast.LENGTH_SHORT).show();
                    } else {
                        // check stock product
                        if (p.stock == 0 || p.status.equalsIgnoreCase("OUT OF STOCK")) {
                            Toast.makeText(ctx, R.string.msg_out_of_stock, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (p.status.equalsIgnoreCase("SUSPEND")) {
                            Toast.makeText(ctx, R.string.msg_suspend, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Double selected_price = p.price_discount > 0 ? p.price_discount : p.price;
                        Cart cart = new Cart(p.id, p.name, p.image, 1, p.stock, selected_price, System.currentTimeMillis());
                        vItem.db.saveCart(cart);
//                        Toast.makeText(ctx, R.string.add_cart, Toast.LENGTH_SHORT).show();
                    }
                    refreshCartButton(p.id,vItem.lyt_add_cart,vItem.tv_add_cart,vItem.ic_add_cart,vItem.db);
                }
            });
        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return this.items.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }

    public void insertData(List<Product> items) {
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.items.add(null);
            notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void resetListData() {
        this.items = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            final GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    int lastPos = layoutManager.findLastVisibleItemPosition();
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) return;
                    boolean bottom = lastPos >= getItemCount() - Constant.PRODUCT_PER_REQUEST;
                    if (!loading && bottom && onLoadMoreListener != null) {
                        int current_page = getItemCount() / Constant.PRODUCT_PER_REQUEST;
                        onLoadMoreListener.onLoadMore(current_page);
                        loading = true;
                    }
                }
            });

            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int type = getItemViewType(position);
                    int spanCount = layoutManager.getSpanCount();
                    return type == VIEW_PROG ? spanCount : 1;
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }

}