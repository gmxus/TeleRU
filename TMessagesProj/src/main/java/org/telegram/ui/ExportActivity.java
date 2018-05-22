package org.telegram.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.teleru.ExportController;
import org.teleru.Observers;
import org.teleru.utils.FileUtils;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.ExportDialogCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.teleru.R;

public class ExportActivity extends BaseFragment implements Observers.IObserver
{
    private ListAdapter listAdapter;
    private RecyclerListView listView;


    @Override
    public boolean onFragmentCreate()
    {
        Observers.addObservers(this,
                Observers.NotifyId.ExportDialogsListChanged,
                Observers.NotifyId.ExportDialogsCurrentItemChanged,
                Observers.NotifyId.ExportDialogsItemAdded,
                Observers.NotifyId.ExportDialogsItemRemoved,
                Observers.NotifyId.ExportDialogsItemCompleted,
                Observers.NotifyId.ExportDialogsItemFailed);

        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context)
    {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ExportChat", R.string.ExportChat));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick()
        {
            @Override
            public void onItemClick(int id)
            {
                if (id == -1)
                    finishFragment();
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout)fragmentView;

        listAdapter = new ListAdapter(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position) ->
        {
            ExportDialogCell cell = (ExportDialogCell) view;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            CharSequence[] items;
            if (cell.getExportDialog().getState() == ExportController.ExportDialogState.Completed)
            {
                items = new CharSequence[]
                        {
                                LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads),
                                LocaleController.getString("DeleteAndStop", R.string.DeleteAndStop)
                        };
            }
            else
            {
                items = new CharSequence[]
                        {
                                LocaleController.getString("DeleteAndStop", R.string.DeleteAndStop)
                        };
            }

            builder.setItems(items, (dialog, which) ->
            {
                dialog.dismiss();
                if (cell.getExportDialog().getState() == ExportController.ExportDialogState.Completed)
                {
                    if (which == 0)
                    {
                        ProgressDialog progressDialog = new ProgressDialog(context);
                        String date = LocaleController.formatDateCallLog(cell.getExportDialog().getDate() / 1000).replace("/", "-").replace(":", "-");
                        String filename = FileUtils.getFixedFilename(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(),
                                String.format("%s %s", cell.getExportDialog().getTitle(), date), "zip");

                        FileUtils.Zipper zipper = new FileUtils.Zipper(cell.getExportDialog().getExportPath(), filename,
                                new FileUtils.Zipper.IZipperListener()
                                {
                                    @Override
                                    public void onProgress(FileUtils.Zipper zipper, int percent)
                                    {
                                        if (progressDialog.isShowing())
                                            progressDialog.setProgress(percent);
                                    }

                                    @Override
                                    public void onComplete(FileUtils.Zipper zipper, String filename)
                                    {
                                        if (progressDialog.isShowing())
                                            progressDialog.dismiss();

                                        String message = LocaleController.getString("SavedTo", R.string.SavedTo);
                                        Toast.makeText(context, String.format(message, cell.getExportDialog().getTitle(), filename), Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(FileUtils.Zipper zipper, Exception ex)
                                    {
                                        if (progressDialog.isShowing())
                                            progressDialog.dismiss();

                                        Toast.makeText(context, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred), Toast.LENGTH_LONG).show();
                                    }
                                });

                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.setIndeterminate(true);
                        progressDialog.setProgress(0);
                        progressDialog.setMax(100);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setCancelable(false);
                        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                                LocaleController.getString("Cancel", R.string.Cancel),
                                (dialog1, which1) ->
                                {
                                    zipper.cancel();
                                    progressDialog.dismiss();
                                });

                        progressDialog.show();
                        zipper.compress();
                    }
                    else if (which == 1)
                        ExportController.getInstance().removeExport(cell.getExportDialog().key);
                }
                else if (which == 0)
                    ExportController.getInstance().removeExport(cell.getExportDialog().key);
            });

            builder.setCancelable(true);
            builder.show();
        });

        LinearLayout emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        TextView emptyTextView1 = new TextView(context);
        emptyTextView1.setText(LocaleController.getString("ExportChatEmptyViewText", R.string.ExportChatEmptyViewText));
        emptyTextView1.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
        emptyTextView1.setGravity(Gravity.CENTER);
        emptyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyView.addView(emptyTextView1, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        listView.setEmptyView(emptyView);

        return fragmentView;
    }

    @Override
    public void onFragmentDestroy()
    {
        Observers.removeObservers(this,
                Observers.NotifyId.ExportDialogsListChanged,
                Observers.NotifyId.ExportDialogsCurrentItemChanged,
                Observers.NotifyId.ExportDialogsItemAdded,
                Observers.NotifyId.ExportDialogsItemRemoved,
                Observers.NotifyId.ExportDialogsItemCompleted,
                Observers.NotifyId.ExportDialogsItemFailed);

        super.onFragmentDestroy();
    }

    @Override
    public void onNotifyReceive(Observers.NotifyId id, Object... args)
    {
        listAdapter.notifyDataSetChanged();
        /*switch (id)
        {
            case ExportDialogsListChanged:
                break;
            case ExportDialogsCurrentItemChanged:
                break;
            case ExportDialogsItemAdded:
                break;
            case ExportDialogsItemRemoved:
                break;
            case ExportDialogsItemCompleted:
                break;
            case ExportDialogsItemFailed:
                break;
        }
        * */
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter
    {
        private Context context;

        public ListAdapter(Context context)
        {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder)
        {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = new ExportDialogCell(context);
            view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
        {
            if (holder.getItemViewType() == 0)
            {
                ExportDialogCell cell = (ExportDialogCell) holder.itemView;
                cell.setExportDialogItem(ExportController.getInstance().getExportDialog(position));
                cell.setCurrentExportDialogItem(ExportController.getInstance().getCurrentExportDialog());
                cell.drawBorder(getItemCount() != position + 1);
            }
        }

        @Override
        public int getItemCount()
        {
            return ExportController.getInstance().getExportDialogsCount();
        }

        @Override
        public int getItemViewType(int position)
        {
            return 0;
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions()
    {
        return new ThemeDescription[]
        {
            new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{ExportDialogCell.class}, null, null, null, Theme.key_windowBackgroundWhite),
            new ThemeDescription(listView, 0, new Class[]{ExportDialogCell.class}, new String[]{"titleView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
            new ThemeDescription(listView, 0, new Class[]{ExportDialogCell.class}, new String[]{"dateView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
            new ThemeDescription(listView, 0, new Class[]{ExportDialogCell.class}, new String[]{"statusView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
            new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray),
            new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault),
            new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault),
            new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon),
            new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
            new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector),
            new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),
        };
    }
}
