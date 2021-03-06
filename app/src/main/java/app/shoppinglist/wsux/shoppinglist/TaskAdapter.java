package app.shoppinglist.wsux.shoppinglist;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import app.shoppinglist.wsux.shoppinglist.firebase.BaseCollectionItem;
import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopTask;

import static app.shoppinglist.wsux.shoppinglist.ShopTaskListDiffCallback.BUNDLE_ARG_DESCRIPTION;
import static app.shoppinglist.wsux.shoppinglist.ShopTaskListDiffCallback.BUNDLE_ARG_STATE;
import static app.shoppinglist.wsux.shoppinglist.ShopTaskListDiffCallback.BUNDLE_ARG_TITLE;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder>
        implements BaseCollectionItem.OnChildChangeListener {
    private static final String TAG = "TASK_ADAPTER";

    private static final int BACKGROUND_NORMAL = 0xffffffff;
    private static final int BACKGROUND_CHECKED = 0xffdddddd;

    private ShopList currentShopList;
    private ArrayList<ShopTask> shopTasks;
    private FireBaseManager fireBaseManager;

    public TaskAdapter(FireBaseManager fireBaseManager) {
        shopTasks = new ArrayList<>();
        this.fireBaseManager = fireBaseManager;
    }

    public void setList(ShopList shopList) {
        if (shopList == currentShopList) {
            return;
        }
        currentShopList = shopList;
    }

    public void resetDataset() {
        ArrayList<ShopTask> oldVersionList = new ArrayList<>(shopTasks);
        ArrayList<ShopTask> newVersionList = getOrderedShopTasks();

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(
                new ShopTaskListDiffCallback(oldVersionList, newVersionList));
        result.dispatchUpdatesTo(TaskAdapter.this);
        shopTasks.clear();
        shopTasks.addAll(newVersionList);
    }

    private ArrayList<ShopTask> getOrderedShopTasks() {
        if (currentShopList == null) {
            return new ArrayList<>();
        }

        ArrayList<ShopTask> listOfLists = new ArrayList<>(currentShopList.getTasks().values());
        Collections.sort(listOfLists);
        return listOfLists;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_list_item, parent, false);
        TaskViewHolder viewHolder = new TaskViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            handleNotEmptyPayload(position, payloads);
        }
    }

    private void handleNotEmptyPayload(int position, @NonNull List<Object> payloads){
        Bundle taskData = (Bundle) payloads.get(0);
        ShopTask shopTask = shopTasks.get(position);
        shopTask.setTitle(taskData.getString(BUNDLE_ARG_TITLE));
        shopTask.setDescription(taskData.getString(BUNDLE_ARG_DESCRIPTION));
        shopTask.setState(taskData.getInt(BUNDLE_ARG_STATE) == 1);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        holder.updateView(position);
    }

    @Override
    public void onChildChange() {
        resetDataset();
    }

    @Override
    public int getItemCount() {
        return shopTasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder
            implements BaseCollectionItem.OnChangeListener,
            CompoundButton.OnCheckedChangeListener, BaseCollectionItem.OnMediaDownload,
            View.OnLongClickListener, View.OnClickListener {

        private int numberOfTimePressed;
        private LinearLayout itemView;
        private TextView taskNameTv;
        private TextView taskNoteTv;
        private CheckBox statusCb;
        private View statusContainer;
        private ImageView thumbnailIv;
        private ShopTask task;

        private TaskViewHolder(LinearLayout itemView) {
            super(itemView);
            this.itemView = itemView;
            initFieldFindViewById(itemView);
            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
            statusContainer.setOnClickListener(this);
        }

        private void initFieldFindViewById(LinearLayout itemView) {
            taskNameTv = itemView.findViewById(R.id.task_name_tv);
            taskNoteTv = itemView.findViewById(R.id.task_note_tv);
            statusCb = itemView.findViewById(R.id.task_status);
            thumbnailIv = itemView.findViewById(R.id.task_thumbnail);
            statusContainer = itemView.findViewById(R.id.task_status_container);
        }

        private void updateView(int position) {

            task = shopTasks.get(position);
            this.numberOfTimePressed = 0;

            if (task == null) {
                return;
            }

            task.setOnChangeListener(this);
            task.setOnMediaDownload(this);
        }

        @Override
        public void onChange() {

            if (task == null) {
                return;
            }

            if (itemView != null) {
                itemView.setBackgroundColor(task.isDone() ? BACKGROUND_CHECKED : BACKGROUND_NORMAL);
            }

            if (taskNameTv != null) {
                taskNameTv.setText(task.getTitle());
            }

            if (taskNoteTv != null) {
                taskNoteTv.setText(task.getDescription());
            }

            if (statusCb != null) {
                statusCb.setOnCheckedChangeListener(null);
                statusCb.setChecked(task.isDone());
                statusCb.setOnCheckedChangeListener(this);
            }

            onMediaDownload();
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            if (task != null) {
                task.setState(checked);
            }
        }

        @Override

        public void onMediaDownload() {

            Bitmap thumbnail = null;
            if (thumbnailIv != null && task.hasPicture()) {
                thumbnail = task.getPicture();
            }

            if (thumbnail == null) {
                thumbnailIv.setImageResource(R.mipmap.ic_launcher);
            } else {
                thumbnailIv.setImageBitmap(thumbnail);
            }
        }

        public boolean onLongClick(View view) {
            new TaskEditDialog(view.getContext(), task, fireBaseManager).show();
            return false;
        }

        @Override
        public void onClick(View view) {
            if (view == itemView) {
                onSingleClickOnView(view.getContext());
            } else {
                onCheckBoxAreaClick();
            }
        }

        private void onSingleClickOnView(Context context) {
            numberOfTimePressed++;

            if (numberOfTimePressed % 3 == 0) {
                Toast.makeText(context, R.string.long_press_to_edit, Toast.LENGTH_SHORT).show();
            }
        }

        private void onCheckBoxAreaClick() {
            statusCb.setChecked(!statusCb.isChecked());
        }
    }
}