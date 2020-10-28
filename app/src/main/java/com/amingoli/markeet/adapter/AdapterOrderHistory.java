package com.amingoli.markeet.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amingoli.markeet.R;
import com.amingoli.markeet.data.SharedPref;
import com.amingoli.markeet.model.Order;
import com.amingoli.markeet.utils.Tools;
import com.balysv.materialripple.MaterialRippleLayout;

import java.util.ArrayList;
import java.util.List;


public class AdapterOrderHistory extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context ctx;
    private List<Order> items = new ArrayList<>();

    private OnItemClickListener onItemClickListener;
    private SharedPref sharedPref;

    public interface OnItemClickListener {
        void onItemClick(View view, Order obj);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView code;
        public TextView date;
        public TextView price;
        public TextView status;
        public MaterialRippleLayout lyt_parent;
        public LinearLayout lay_background;

        public ViewHolder(View v) {
            super(v);
            code = (TextView) v.findViewById(R.id.code);
            date = (TextView) v.findViewById(R.id.date);
            price = (TextView) v.findViewById(R.id.price);
            status = (TextView) v.findViewById(R.id.status);
            lyt_parent = (MaterialRippleLayout) v.findViewById(R.id.lyt_parent);
            lay_background = (LinearLayout) v.findViewById(R.id.lay_background);
        }
    }

    public AdapterOrderHistory(Context ctx, List<Order> items) {
        this.ctx = ctx;
        this.items = items;
        sharedPref = new SharedPref(ctx);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
        vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            ViewHolder vItem = (ViewHolder) holder;
            final Order c = items.get(position);

            String status = "";
            if (c.status!=null && c.status.equals("PROCESSED")){
                status = ctx.getString(R.string.processed);
                vItem.lay_background.setBackgroundColor(Color.parseColor("#43066700"));
            }else if (c.status!=null && c.status.equals("CANCEL")){
                status = ctx.getString(R.string.cancel);
                vItem.lay_background.setBackgroundColor(Color.parseColor("#43FF0000"));
            }else {
//                status = ctx.getString(R.string.waiting);
            }
            vItem.code.setText(c.code);
            vItem.price.setText(c.total_fees);
            vItem.status.setText(status);
            vItem.date.setText(Tools.getFormattedDateSimple(c.created_at));
            vItem.lyt_parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(v, c);
                    }
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<Order> getItem() {
        return items;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setItems(List<Order> items) {
        this.items = items;
        notifyDataSetChanged();
    }


}