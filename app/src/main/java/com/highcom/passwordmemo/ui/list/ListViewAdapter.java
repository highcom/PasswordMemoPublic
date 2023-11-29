package com.highcom.passwordmemo.ui.list;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.highcom.passwordmemo.R;
import com.highcom.passwordmemo.util.TextSizeUtil;
import com.highcom.passwordmemo.util.login.LoginDataManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListViewAdapter extends RecyclerView.Adapter<ListViewAdapter.ViewHolder> implements Filterable {

    // private Context context;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;

    private static final float ROW_LAYOUT_HEIGHT_SMALL = 40;
    private static final float ROW_LAYOUT_HEIGHT_MEDIUM = 45;
    private static final float ROW_LAYOUT_HEIGHT_LARGE = 50;
    private static final float ROW_LAYOUT_HEIGHT_EXTRA_LARGE = 55;

    private LayoutInflater inflater;
    private List<? extends Map<String, ?>> listData;
    private List<? extends Map<String, ?>> orig;
    private LoginDataManager loginDataManager;
    private AdapterListener adapterListener;

    private Map<Integer, Float> layoutHeightMap;

    private float textSize = 15;
    private boolean editEnable = false;

    public interface AdapterListener {
        void onAdapterClicked(View view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout rowLinearLayout;
        public Long  id;
        public ImageButton imageButton;
        public TextView title;
        public String account;
        public String password;
        public String url;
        public Long groupId;
        public String memo;
        public TextView date;
        public ImageButton rearrangebtn;
        public TextView memoView;

        public ViewHolder(final View itemView) {
            super(itemView);
            // フッターの場合には何も設定しない
            if (itemView.getId() == R.id.row_footer) {
                return;
            }
            rowLinearLayout = (LinearLayout) itemView.findViewById(R.id.rowLinearLayout);
            imageButton = (ImageButton) itemView.findViewById(R.id.round_key_icon);
            title = (TextView) itemView.findViewById(R.id.title);
            date = (TextView) itemView.findViewById(R.id.date);
            rearrangebtn = (ImageButton) itemView.findViewById(R.id.rearrangebutton);
            memoView = (TextView) itemView.findViewById(R.id.memoView);

            if (editEnable) {
                rearrangebtn.setVisibility(View.VISIBLE);
            } else {
                rearrangebtn.setVisibility(View.GONE);
            }
        }
    }

    public ListViewAdapter(Context context, List<? extends Map<String, ?>> data, LoginDataManager manager, AdapterListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listData = data;
        this.loginDataManager = manager;
        this.adapterListener = listener;
        this.layoutHeightMap = new HashMap<Integer, Float>(){
            {
                put(TextSizeUtil.TEXT_SIZE_SMALL, convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_SMALL));
                put(TextSizeUtil.TEXT_SIZE_MEDIUM, convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_MEDIUM));
                put(TextSizeUtil.TEXT_SIZE_LARGE, convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_LARGE));
                put(TextSizeUtil.TEXT_SIZE_EXTRA_LARGE, convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_EXTRA_LARGE));
            }
        };
    }

    private float convertFromDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float size) {
        textSize = size;
    }

    public void setEditEnable(boolean enable) {
        editEnable = enable;
    }

    public boolean getEditEnable() {
        return editEnable;
    }

    @Override
    public int getItemCount() {
        if (listData != null) {
            return listData.size() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= listData.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            return new ViewHolder(inflater.inflate(R.layout.row, parent, false));
        } else if (viewType == TYPE_FOOTER) {
            return new ViewHolder(inflater.inflate(R.layout.row_footer, parent, false));
        } else {
            return  null;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // フッターの場合にはデータをバインドしない
        if (position >= listData.size()) return;

        Float layoutHeight = layoutHeightMap.get((int)textSize);
        if (layoutHeight != null) {
            ViewGroup.LayoutParams params = holder.rowLinearLayout.getLayoutParams();
            params.height = (int)layoutHeight.floatValue();
            holder.rowLinearLayout.setLayoutParams(params);
        }

        String id = ((HashMap<?, ?>) listData.get(position)).get("id").toString();
        String title = ((HashMap<?, ?>) listData.get(position)).get("title").toString();
        String account = ((HashMap<?, ?>) listData.get(position)).get("account").toString();
        String password = ((HashMap<?, ?>) listData.get(position)).get("password").toString();
        String url = ((HashMap<?, ?>) listData.get(position)).get("url").toString();
        String groupId = ((HashMap<?, ?>) listData.get(position)).get("group_id").toString();
        String memo = ((HashMap<?, ?>) listData.get(position)).get("memo").toString();
        String date = ((HashMap<?, ?>) listData.get(position)).get("inputdate").toString();
        holder.id = Long.valueOf(id);
        holder.title.setText(title);
        holder.title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        holder.account = account;
        holder.password = password;
        holder.url = url;
        holder.groupId = Long.valueOf(groupId);
        holder.memo = memo;
        holder.date.setText(date);
        holder.date.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize-3);
        // メモ表示が有効でメモが入力されている場合は表示する
        if (loginDataManager.isMemoVisibleSwitchEnable() && !memo.equals("")) {
            holder.memoView.setVisibility(View.VISIBLE);
            holder.memoView.setText(memo);
        } else {
            holder.memoView.setVisibility(View.GONE);
            holder.memoView.setText("");
        }
        holder.memoView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize-3);
        holder.itemView.setTag(holder);
        // アイテムクリック時ののイベントを追加
        holder.imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View parentView = (View) view.getParent();
                parentView.callOnClick();
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterListener.onAdapterClicked(view);
            }
        });

    }

    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults oReturn = new FilterResults();
                final ArrayList<Map<String, ?>> results = new ArrayList<Map<String, ?>>();
                if (orig == null)
                    orig = listData;
                if (constraint != null) {
                    if (orig != null && orig.size() > 0) {
                        for (final Map<String, ?> g : orig) {
                            if (g.get("title").toString().toLowerCase().contains(constraint.toString()))
                                results.add(g);
                        }
                    }
                    oReturn.values = results;
                } else {
                    oReturn.values = orig;
                }
                return oReturn;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                listData = (ArrayList<Map<String, String>>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
