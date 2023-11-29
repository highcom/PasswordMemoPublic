package com.highcom.passwordmemo.ui.list;

import android.content.Context;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.highcom.passwordmemo.R;
import com.highcom.passwordmemo.util.TextSizeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;

    private static final float ROW_LAYOUT_HEIGHT_SMALL = 40;
    private static final float ROW_LAYOUT_HEIGHT_MEDIUM = 45;
    private static final float ROW_LAYOUT_HEIGHT_LARGE = 50;
    private static final float ROW_LAYOUT_HEIGHT_EXTRA_LARGE = 55;

    private LayoutInflater inflater;
    private List<? extends Map<String, ?>> listData;

    private Map<Integer, Float> layoutHeightMap;

    private float textSize = 15;
    private boolean editEnable = false;

    private GroupAdapterListener adapterListener;
    public interface GroupAdapterListener {
        void onGroupNameClicked(View view, Long groupId);
        void onGroupNameOutOfFocused(View view, Map<String, String> data);
    }

    public class GroupViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout rowLinearLayout;
        public Long  groupId;
        public int  groupOrder;
        public ImageButton groupImage;
        public EditText groupName;
        private String orgGroupName = "";
        public ImageButton groupRearrangeButton;

        public GroupViewHolder(final View itemView) {
            super(itemView);
            // フッターの場合には何も設定しない
            if (itemView.getId() == R.id.row_footer) {
                return;
            }
            rowLinearLayout = (LinearLayout) itemView.findViewById(R.id.rowGroupLayout);
            groupImage = (ImageButton) itemView.findViewById(R.id.folder_icon);
            groupName = (EditText) itemView.findViewById(R.id.groupName);
            groupImage.setOnClickListener(view -> {
                if (!editEnable) {
                    orgGroupName = groupName.getText().toString();
                    adapterListener.onGroupNameClicked(view, groupId);
                }
            });
            groupName.setOnClickListener(view -> {
                orgGroupName = groupName.getText().toString();
                adapterListener.onGroupNameClicked(view, groupId);
            });
            groupName.setOnFocusChangeListener((view, b) -> {
                if (b) {
                    orgGroupName = groupName.getText().toString();
                    adapterListener.onGroupNameClicked(view, groupId);
                } else {
                    // グループ名を空欄で登録することは出来ないので元の名前にする
                    if (groupName.getText().toString().equals("")) {
                        groupName.setText(orgGroupName);
                    }
                    Map<String, String> data = new HashMap<String, String>();
                    data.put("group_id", Long.valueOf(groupId).toString());
                    data.put("group_order", Integer.valueOf(groupOrder).toString());
                    data.put("name", groupName.getText().toString());
                    // フォーカスが外れた時に内容が変更されていたら更新する
                    adapterListener.onGroupNameOutOfFocused(view, data);
                }
            });
            // キーボードのエンターが押下された場合
            groupName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        textView.setFocusable(false);
                        textView.setFocusableInTouchMode(false);
                        textView.requestFocus();
                    }
                    return false;
                }
            });

            groupRearrangeButton = itemView.findViewById(R.id.groupRearrangeButton);
        }
    }

    public GroupListAdapter(Context context, List<? extends Map<String, ?>> data, GroupAdapterListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listData = data;
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
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            return new GroupViewHolder(inflater.inflate(R.layout.row_group, parent, false));
        } else if (viewType == TYPE_FOOTER) {
            return new GroupViewHolder(inflater.inflate(R.layout.row_footer, parent, false));
        } else {
            return  null;
        }
    }

    @Override
    public void onBindViewHolder(GroupViewHolder holder, final int position) {
        // フッターの場合にはデータをバインドしない
        if (position >= listData.size()) return;

        Float layoutHeight = layoutHeightMap.get((int)textSize);
        if (layoutHeight != null) {
            ViewGroup.LayoutParams params = holder.rowLinearLayout.getLayoutParams();
            params.height = (int)layoutHeight.floatValue();
            holder.rowLinearLayout.setLayoutParams(params);
        }

        String groupId = ((HashMap<?, ?>) listData.get(position)).get("group_id").toString();
        String groupOrder = ((HashMap<?, ?>) listData.get(position)).get("group_order").toString();
        String groupName = ((HashMap<?, ?>) listData.get(position)).get("name").toString();
        holder.groupId = Long.valueOf(groupId);
        holder.groupOrder = Integer.valueOf(groupOrder);
        holder.groupName.setText(groupName);
        holder.groupName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        holder.itemView.setTag(holder);
        // グループ名が空欄があった場合は新規追加時なのでフォーカスする
        if (groupName.equals("")) {
            adapterListener.onGroupNameClicked(holder.itemView.findViewById(R.id.groupName), holder.groupId);
        }
        // 編集モードでも1番目の「すべて」は並べ替えさせない
        if (editEnable && holder.groupId != null && holder.groupId != 1L) {
            holder.groupRearrangeButton.setVisibility(View.VISIBLE);
        } else {
            holder.groupRearrangeButton.setVisibility(View.GONE);
        }
    }
}
