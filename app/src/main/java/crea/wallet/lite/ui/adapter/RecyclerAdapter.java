package crea.wallet.lite.ui.adapter;

import android.app.Activity;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ander on 18/10/16.
 */
public abstract class RecyclerAdapter<VH extends RecyclerView.ViewHolder, Item> extends RecyclerView.Adapter<VH>{

    private static final String TAG = "RecyclerAdapter";

    public interface OnItemClickListener<Item> {
        void OnItemClick(View v, int position, Item item);
        boolean OnItemLongClick(View v, int position, Item item);
    }

    protected Activity activity;
    protected List<Item> itemList = new ArrayList<>();
    private OnItemClickListener<Item> onItemClickListener;

    public RecyclerAdapter(Activity activity) {
        this.activity = activity;
        this.itemList = new ArrayList<>();
    }

    protected abstract List<Item> getItemList();

    public Item getItem(int position) {
        return itemList.get(position);
    }

    public OnItemClickListener<Item> getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener<Item> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void notifyDataChanged() {
        this.itemList = getItemList();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    protected String getString(@StringRes int res, Object... args) {
        return activity.getString(res, args);
    }

    @Override
    public final void onBindViewHolder(VH holder, final int position) {
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
        holder.itemView.setLongClickable(true);
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClickListener.OnItemClick(view, position, getItem(position));
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return onItemClickListener.OnItemLongClick(view, position, getItem(position));
                }
            });
        }

        onBindHolder(holder, position, getItem(position));
    }

    public abstract void onBindHolder(VH holder, int position, Item item);

    protected String getString(@StringRes int res) {
        return activity.getString(res);
    }

    protected LayoutInflater getLayoutInflater() {
        return activity.getLayoutInflater();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public void replace(Collection<Item> items) {
        this.itemList.clear();
        this.itemList.addAll(items);
        notifyDataChanged();
    }
}
