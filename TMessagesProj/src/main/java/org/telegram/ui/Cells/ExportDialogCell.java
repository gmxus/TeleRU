package org.telegram.ui.Cells;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.teleru.ExportController;
import org.teleru.utils.StringUtils;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.teleru.R;

@SuppressLint("RtlHardcoded")
public class ExportDialogCell extends LinearLayout
{
    private TextView titleView;
    private TextView dateView;
    private TextView statusView;
    private TextView optionsView;
    private boolean needDivider = false;
    private ExportController.ExportDialog exportDialog;

    public ExportController.ExportDialog getExportDialog()
    {
        return exportDialog;
    }

    public ExportDialogCell(Context context)
    {
        super(context);
        setOrientation(VERTICAL);
        setBaselineAligned(false);
        setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 2, 2, 2, 2));

        titleView = new TextView(context);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setLines(1);
        titleView.setMaxLines(1);
        titleView.setSingleLine(true);
        titleView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        titleView.setEllipsize(TextUtils.TruncateAt.END);

        addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                LocaleController.isRTL ? 64 : 17, 5, LocaleController.isRTL ? 17 : 64, 0));

        dateView = new TextView(context);
        dateView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        dateView.setLines(1);
        dateView.setMaxLines(1);
        dateView.setSingleLine(true);
        dateView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        dateView.setEllipsize(TextUtils.TruncateAt.END);

        addView(dateView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                LocaleController.isRTL ? 64 : 17, 5, LocaleController.isRTL ? 17 : 64, 0));

        optionsView = new TextView(context);
        optionsView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        optionsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        optionsView.setLines(1);
        optionsView.setMaxLines(1);
        optionsView.setSingleLine(true);
        optionsView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        optionsView.setEllipsize(TextUtils.TruncateAt.END);

        addView(optionsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                LocaleController.isRTL ? 64 : 17, 5, LocaleController.isRTL ? 17 : 64, 0));

        statusView = new TextView(context);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        statusView.setLines(1);
        statusView.setMaxLines(1);
        statusView.setSingleLine(true);
        statusView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        statusView.setEllipsize(TextUtils.TruncateAt.END);

        addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                LocaleController.isRTL ? 64 : 17, 5, LocaleController.isRTL ? 17 : 64, 0));
    }

    public void setExportDialogItem(ExportController.ExportDialog exportDialog)
    {
        this.exportDialog = exportDialog;
        String title = exportDialog.getTitle();
        TLRPC.User user = MessagesController.getInstance().getUser(exportDialog.getUserId());
        if (user != null)
            title = String.format("%s %s", user.first_name, user.last_name != null ? user.last_name : StringUtils.empty);

        TLRPC.Chat chat = MessagesController.getInstance().getChat(exportDialog.getChatId());
        if (chat != null)
            title = chat.title;

        String status = StringUtils.empty;
        switch (exportDialog.getState())
        {
            case Waiting:
                status = LocaleController.getString("ExportChatWaiting", R.string.ExportChatWaiting);
                break;
            case Exporting:
                status = LocaleController.getString("ExportChatExporting", R.string.ExportChatExporting);
                statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                break;
            case Completed:
                status = LocaleController.getString("ExportChatCompleted", R.string.ExportChatCompleted);
                statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText));
                break;
            case Failed:
                status = LocaleController.getString("ExportChatFailed", R.string.ExportChatFailed);
                statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText));
                break;

        }

        String optionsText = StringUtils.empty;
        if (exportDialog.isIncludePhotos())
            optionsText += LocaleController.getString("ExportChatIncludePhotos", R.string.ExportChatIncludePhotos);

        if (exportDialog.isIncludeStickers())
        {
            if (!StringUtils.isNullOrEmpty(optionsText))
                optionsText += ", ";

            optionsText += LocaleController.getString("ExportChatIncludeStickers", R.string.ExportChatIncludeStickers);
        }

        if (exportDialog.isIncludeVoiceMessages())
        {
            if (!StringUtils.isNullOrEmpty(optionsText))
                optionsText += ", ";

            optionsText += LocaleController.getString("ExportChatIncludeVoiceMessages", R.string.ExportChatIncludeVoiceMessages);
        }

        if (exportDialog.isIncludeFiles())
        {
            if (!StringUtils.isNullOrEmpty(optionsText))
                optionsText += ", ";

            optionsText += LocaleController.getString("ExportChatIncludeFiles", R.string.ExportChatIncludeFiles);
        }

        if (StringUtils.isNullOrEmpty(optionsText))
            optionsView.setVisibility(GONE);

        titleView.setText(title);
        dateView.setText(LocaleController.formatDateCallLog(exportDialog.getDate() / 1000));
        statusView.setText(status);
        optionsView.setText(optionsText);
    }

    public void setCurrentExportDialogItem(ExportController.ExportDialog currentExportDialog)
    {
        if (currentExportDialog != null && currentExportDialog.dialogId == exportDialog.dialogId)
            setExportDialogItem(currentExportDialog);
    }

    public void drawBorder(boolean b)
    {
        needDivider = b;
        setWillNotDraw(!needDivider);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(optionsView.getVisibility() == VISIBLE ? 110 : 85) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (needDivider)
            canvas.drawLine(getPaddingLeft(), getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, Theme.dividerPaint);
    }
}
