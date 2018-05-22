package org.teleru;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Base64;
import android.widget.Toast;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.teleru.utils.FileUtils;
import org.teleru.utils.StringUtils;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class ExportController implements NotificationCenter.NotificationCenterDelegate
{
    private static final int READ_MESSAGE_COUNT = 100;

    public static final String TAG = ExportController.class.getName();

    public enum ExportDialogState
    {
        Waiting(-1),
        Exporting(1),
        Completed(2),
        Failed(3);

        public final int code;

        ExportDialogState(int code)
        {
            this.code = code;
        }

        public static ExportDialogState find(int code)
        {
            for (ExportDialogState state : ExportDialogState.values())
            {
                if (state.code == code)
                    return state;
            }

            return Exporting;
        }
    }

    public class ExportDialog
    {
        public final String key;
        public final long dialogId;
        private String dialogType;
        private String title;
        private int userId;
        private int chatId;
        private boolean includePhotos = false;
        private boolean includeStickers = false;
        private boolean includeVoiceMessages = false;
        private boolean includeFiles = false;
        private long date = 0;
        private File exportPath = null;
        private int lastMessageId = 0;
        private int lastRequestId;
        private boolean isUsersSaved = false;
        private boolean isChatsSaved = false;
        private boolean isExportMessagesCompleted = false;
        private boolean isExportFilesCompleted = false;
        private ExportDialogState state;
        private int errorCode = -1;
        private String errorMessage = null;
        private OutputStreamWriter messagesWriter = null;

        public String getTitle()
        {
            return title;
        }

        public int getUserId()
        {
            return userId;
        }

        public int getChatId()
        {
            return chatId;
        }

        public boolean isIncludePhotos()
        {
            return includePhotos;
        }

        public boolean isIncludeStickers()
        {
            return includeStickers;
        }

        public boolean isIncludeVoiceMessages()
        {
            return includeVoiceMessages;
        }

        public boolean isIncludeFiles()
        {
            return includeFiles;
        }

        public long getDate()
        {
            return date;
        }

        public String getExportPath()
        {
            try
            {
                String appName = ApplicationLoader.applicationContext.getString(R.string.AppName);
                File basePath = new File(Environment.getExternalStorageDirectory(), appName);
                File baseExportPath = new File(basePath, "ExportChats");
                return new File(baseExportPath, key).getPath();
            }
            catch (Exception ignored) {}
            return null;
        }

        public ExportDialogState getState()
        {
            return state;
        }

        public int getErrorCode()
        {
            return errorCode;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }


        private ExportDialog(String key, long dialogId)
        {
            this.key = key;
            this.dialogId = dialogId;
        }

        private void export()
        {
            if (!isRunning || ExportDialogState.Waiting != state)
                return;

            try
            {
                String appName = ApplicationLoader.applicationContext.getString(R.string.AppName);
                File basePath = new File(Environment.getExternalStorageDirectory(), appName);
                File baseExportPath = new File(basePath, "ExportChats");
                exportPath = new File(baseExportPath, key);

                if ((!exportPath.exists() && !exportPath.mkdirs()) || !exportPath.canWrite())
                    throw new Exception();

                File savePath = new File(exportPath, "messages.json");
                boolean exists = savePath.exists();
                messagesWriter = new OutputStreamWriter(new FileOutputStream(savePath, exists), "UTF-8");
                if (!exists)
                    messagesWriter.write("{");
            }
            catch (Exception ignored)
            {
                if (!isRunning)
                    return;

                onError(-1, ignored.getMessage());
                return;
            }

            state = ExportDialogState.Exporting;
            if (!isExportMessagesCompleted)
            {
                exportMessages(()->
                {
                    if (!isExportFilesCompleted)
                    {
                        exportFiles(this::onCompleted);
                        return;
                    }

                    onCompleted();
                });
                return;
            }

            onCompleted();
        }

        private void exportMessages(Runnable listener)
        {
            int lower_part = (int) dialogId;
            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = MessagesController.getInputPeer(lower_part);
            req.limit = READ_MESSAGE_COUNT;
            req.offset_id = lastMessageId;
            req.offset_date = 0;
            req.add_offset = 0;
            lastRequestId = ConnectionsManager.getInstance().sendRequest(req, (response, error) ->
                    ApplicationLoader.applicationHandler.post(() ->
                    {
                        if (!isRunning)
                            return;

                        if (error != null || response == null)
                        {
                            onError(error != null ? error.code: -1, error != null ? error.text : "no data received!");
                            return;
                        }

                        final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                        if (res.messages.size() > READ_MESSAGE_COUNT)
                            res.messages.remove(0);

                        if (!isUsersSaved && res.users.size() > 0)
                        {
                            try
                            {
                                JSONObject users = new JSONObject();
                                for (TLRPC.User user : res.users)
                                {
                                    JSONObject userInfo = new JSONObject();
                                //    userInfo.put("photo_small", TLUtils.photoToBase64(user.photo.photo_small));
                                 //   userInfo.put("photo_large", TLUtils.photoToBase64(user.photo.photo_big));
                                    userInfo.put("id", user.id);
                                    userInfo.put("username", user.username);
                                    userInfo.put("self", user.self);
                                    userInfo.put("bot", user.bot);
                                    userInfo.put("deleted", user.deleted);
                                    userInfo.put("access_hash", user.access_hash);
                                    userInfo.put("inactive", user.inactive);
                                    userInfo.put("flags", user.flags);
                                    userInfo.put("first_name", user.first_name);
                                    userInfo.put("last_name", user.last_name);
                                    userInfo.put("contact", user.contact);
                                    userInfo.put("mutual_contact", user.mutual_contact);
                                    userInfo.put("inactive", user.inactive);
                                    users.put(Integer.toString(user.id), userInfo);
                                }

                                File savePath = new File(exportPath, "users.json");
                                if (savePath.exists() && !savePath.delete())
                                    throw new Exception();

                                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(savePath), "UTF-8");
                                writer.write(users.toString());
                                writer.flush();
                                writer.close();
                                isUsersSaved = true;
                            }
                            catch (Exception ignored)
                            {
                                onError(-1, ignored.toString());
                                return;
                            }
                        }

                        if (!isChatsSaved && res.chats.size() > 0)
                        {
                            try
                            {
                                JSONObject chats = new JSONObject();
                                for (TLRPC.Chat chat : res.chats)
                                {
                                    JSONObject chatInfo = new JSONObject();
                               //     chatInfo.put("photo_small", TLUtils.photoToBase64(chat.photo.photo_small));
                                //    chatInfo.put("photo_large", TLUtils.photoToBase64(chat.photo.photo_big));
                                    chatInfo.put("id", chat.id);
                                    chatInfo.put("title", chat.title);
                                    chatInfo.put("username", chat.username);
                                    chatInfo.put("address", chat.address);
                                    chatInfo.put("access_hash", chat.access_hash);
                                    chatInfo.put("creator", chat.creator);
                                    chatInfo.put("admin", chat.admin);
                                    chatInfo.put("admins_enabled", chat.admins_enabled);
                                    chatInfo.put("broadcast", chat.broadcast);
                                    chatInfo.put("checked_in", chat.checked_in);
                                    chatInfo.put("date", chat.date);
                                    chatInfo.put("megagroup", chat.megagroup);
                                    chatInfo.put("moderator", chat.moderator);
                                    chatInfo.put("version", chat.version);
                                    chats.put(Integer.toString(chat.id), chatInfo);
                                }

                                File savePath = new File(exportPath, "chats.json");
                                if (savePath.exists() && !savePath.delete())
                                    throw new Exception();

                                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(savePath), "UTF-8");
                                writer.write(chats.toString());
                                writer.flush();
                                writer.close();
                                isChatsSaved = true;
                            }
                            catch (Exception ignored)
                            {
                                onError(-1, ignored.getMessage());
                                return;
                            }
                        }

                        if (res.messages.size() > 0)
                        {
                            try
                            {
                                for (TLRPC.Message message : res.messages)
                                {
                                     String content = !StringUtils.isNullOrEmpty(message.message) ?
                                             Base64.encodeToString(message.message.getBytes(Charset.forName("UTF-8")),
                                                     Base64.NO_WRAP) : StringUtils.empty;

                                    JSONObject messageInfo = new JSONObject();
                                    messageInfo.put("id", message.id);
                                    messageInfo.put("dialog_id", message.dialog_id);
                                    messageInfo.put("from_id", message.from_id);
                                    messageInfo.put("fwd_msg_id", message.fwd_msg_id);
                                    messageInfo.put("reply_to_msg_id", message.reply_to_msg_id);
                                    messageInfo.put("reply_to_random_id", message.reply_to_random_id);
                                    messageInfo.put("random_id", message.random_id);
                                    messageInfo.put("via_bot_id", message.via_bot_id);
                                    messageInfo.put("message", content);
                                    messageInfo.put("date", message.date);
                                    messageInfo.put("edit_date", message.edit_date);
                                    messageInfo.put("flags", message.flags);
                                    messageInfo.put("layer", message.layer);
                                    messageInfo.put("mentioned", message.mentioned);
                                    messageInfo.put("out", message.out);
                                    messageInfo.put("post", message.post);
                                    messageInfo.put("post_author", message.post_author);
                                    messagesWriter.write(String.format("\"%s\":%s,", message.id, messageInfo.toString()));
                                    lastMessageId = message.id;
                                }

                                messagesWriter.flush();
                                ExportController.getInstance().updateExportChatLastMessageId(this, lastMessageId);
                            }
                            catch (Exception ignored)
                            {
                                onError(-1, ignored.getMessage());
                                return;
                            }

                            exportMessages(listener);
                        }
                        else
                        {
                            try
                            {
                                messagesWriter.write("}");
                                messagesWriter.flush();
                                messagesWriter.close();
                                messagesWriter = null;
                            }
                            catch (Exception ignored)
                            {
                                onError(-1, ignored.getMessage());
                                return;
                            }

                            isExportMessagesCompleted = true;
                            ExportController.getInstance().setExportChatMessageCompleted(this);
                            listener.run();
                        }
                    }));
            ConnectionsManager.getInstance().bindRequestToGuid(lastRequestId, classGuid);
        }

        private void exportFiles(Runnable listener)
        {
            ExportController.getInstance().setExportChatFilesCompleted(this);
            listener.run();
        }

        private void onCompleted()
        {
            if (!isRunning)
                return;

            try
            {
                File htmlFile = new File(exportPath, "index.html");
                if (htmlFile.exists() && !htmlFile.delete())
                    throw new Exception();

                JSONObject info = new JSONObject();
                info.put("title", title);
                info.put("userId", userId);
                info.put("chatId", chatId);
                info.put("dialogId", dialogId);
                info.put("dialogType", dialogType);
                info.put("includePhotos", includePhotos);
                info.put("includeStickers", includeStickers);
                info.put("includeVoiceMessages", includeVoiceMessages);
                info.put("includeFiles", includeFiles);
                info.put("date", date);

                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(htmlFile, false), Charset.forName("UTF-8"));
                writer.write("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><script>");
                writer.write(String.format("var info=%s;", info.toString()));
                writer.write("var users=");

                byte[] buffer = new byte[2048];
                int read;

                File usersFile = new File(exportPath, "users.json");
                if (usersFile.exists())
                {
                    if (usersFile.length() > 0)
                    {
                        InputStream usersReader = new FileInputStream(usersFile);
                        while ((read = usersReader.read(buffer, 0, buffer.length))  > 0)
                            writer.write(new String(buffer, 0, read, "UTF-8"));

                        usersReader.close();
                    }

                    if (!usersFile.delete())
                        throw new Exception();
                }
                else
                    writer.write("{}");

                writer.write(";");
                writer.write("var chats=");

                File chatsFile = new File(exportPath, "chats.json");
                if (chatsFile.exists())
                {
                    if (chatsFile.length() > 0)
                    {
                        InputStream chatsReader = new FileInputStream(chatsFile);
                        while ((read = chatsReader.read(buffer, 0, buffer.length))  > 0)
                            writer.write(new String(buffer, 0, read, "UTF-8"));

                        chatsReader.close();
                    }

                    if (!chatsFile.delete())
                        throw new Exception();
                }
                else
                    writer.write("{}");

                writer.write(";");
                writer.write("var messages='");

                File messagesFile = new File(exportPath, "messages.json");
                if (messagesFile.exists())
                {
                    if (messagesFile.length() > 0)
                    {
                        InputStream messagesReader = new FileInputStream(messagesFile);
                        while ((read = messagesReader.read(buffer, 0, buffer.length))  > 0)
                            writer.write(new String(buffer, 0, read, "UTF-8"));

                        messagesReader.close();
                    }

                    if (!messagesFile.delete())
                        throw new Exception();
                }
                else
                    writer.write("{}");

                writer.write("';");
                writer.write("</script>");
                writer.write("<style>body{background-color:#e9e9e9;font-family:Tahoma,Arial,sans-serif;font-size:14px}.message-box{background-color:white;margin:5px auto;padding:10px;border-radius:5px;width:auto;max-width:800px;border:1px solid #e5e5e5;box-shadow:1px 1px 1px #cbcbcb;color:black}.message-box.self{text-align:right;background-color:#efffde;color:black}.message-box .message-avatar img{width:40px;height:40px;margin:0;padding:0;border-radius:100px;vertical-align:middle;display:block;float:left}.message-box.self .message-avatar img{float:right}.message-box .message-avatar p{font-size:16px;margin:0 5px 5px;padding:0;vertical-align:middle;float:left}.message-box .message-avatar p span{font-size:12px;color:#535353}.message-box.self .message-avatar p{float:right}</style>");
                writer.write("</head>");
                writer.write("<body>");
                writer.write("<div id=\"main\"></div><script>var months=['January','February','March','April','May','June','July','August','September','October','November','December'];function b64DecodeUnicode(str){return decodeURIComponent(Array.prototype.map.call(atob(str),function(c){return'%'+('00'+c.charCodeAt(0).toString(16)).slice(-2)}).join(''));} function formatDate(date){return date.getFullYear()+'/'+(months[date.getMonth()])+'/'+date.getDate()+' ' +date.getHours()+':'+date.getMinutes()+':'+date.getSeconds();} var rootElement=document.getElementById(\"main\");var messagesData=JSON.parse(messages.substring(0,messages.length-2)+\"}\");var messagesKeys=Object.keys(messagesData);if(messagesKeys.length>0){for(var i=0;i<messagesKeys.length;i++){var key=messagesKeys[i];var msg=messagesData[key];var user=users.hasOwnProperty(msg.from_id)?users[msg.from_id]:null;var text=msg.message;var date=formatDate(new Date(msg.date*1000));if(text.length>0) text=b64DecodeUnicode(text);rootElement.insertAdjacentHTML('beforeend','<div class=\"message-box'+((user!=null&&user.self)?' self':'')+'\">'+'<div class=\"message-avatar\"><img src=\"photos/avatar_'+(user!=null?user.id:\"deleted\")+'.jpg\"/><p>'+(user!=null?(user.first_name+' '+(user.last_name!=undefined?user.last_name:'')):'deleted')+'<br/><span>'+date+'</span></p></div><div style=\"clear: both\"></div>'+'<div class=\"message-text\">'+text+'</div></div>');}}else{rootElement.innerHTML=\"<center><h1 style='color: white;'>No Chats!</h1></center>\";}</script>");
                writer.write("</body></html>");
                writer.flush();
                writer.close();
            }
            catch (Exception ignored)
            {
                onError(-1, ignored.getMessage());
                return;
            }

            onExportCompleted(this);
        }

        private void onError(int errorCode, String errorMessage)
        {
            clean();
            onExportFailed(this, errorCode, errorMessage);
        }

        private void remove()
        {
            clean();
            ConnectionsManager.getInstance().cancelRequest(lastRequestId, true);
        }

        private void clean()
        {
            dispose();
            delete();
        }

        private void dispose()
        {
            try
            {
                if (messagesWriter != null)
                {
                    messagesWriter.flush();
                    messagesWriter.close();
                }
            }
            catch (Exception ignored) {}
        }

        private void delete()
        {
            try
            {
                File basePath = new File(Environment.getExternalStorageDirectory(), ApplicationLoader.applicationContext.getString(R.string.AppName));
                File baseExportPath = new File(basePath, "ExportChats");
                File exportPath = new File(baseExportPath, Long.toString(dialogId));
                if (exportPath.exists())
                    FileUtils.deleteRecursive(exportPath, exportPath);
            }
            catch (Exception ignored) {}
        }
    }

    private static ExportController instance = new ExportController();

    public  static ExportController getInstance()
    {
        return instance;
    }

    private List<String> exportDialogAllList = new ArrayList<>();
    private List<String> exportDialogNeedToExportList = new ArrayList<>();
    private ExportDialog currentExportDialog = null;
    private int classGuid = -1;
    private SharedPreferences storage;
    private boolean isRunning = false;

    public ExportDialog getCurrentExportDialog()
    {
        return currentExportDialog;
    }


    private ExportController()
    {
        storage = ApplicationLoader.applicationContext.getSharedPreferences(ExportController.class.getName(), Context.MODE_PRIVATE);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didUpdatedConnectionState);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.appDidLogout);
    }

    public void init()
    {
        if (isRunning)
            return;

        isRunning = true;
        try
        {
            Map<String, ?> items = storage.getAll();
            if (items != null && items.size() > 0)
            {
                for (Map.Entry<String, ?> entry : items.entrySet())
                {
                    String key = entry.getKey();
                    ExportDialog exportDialog = getExportDialog(key);
                    assert exportDialog != null;
                    if (exportDialog.state == ExportDialogState.Waiting || exportDialog.state == ExportDialogState.Exporting)
                        exportDialogNeedToExportList.add(key);

                    exportDialogAllList.add(key);
                }
            }
        }
        catch (Exception ex)
        {
            storage.edit().clear().apply();
            exportDialogNeedToExportList.clear();
            exportDialogAllList.clear();
            removeAllExportDialogsFiles();
        }

        classGuid = ConnectionsManager.getInstance().generateClassGuid();
        ApplicationLoader.applicationHandler.post(exportMessages);
    }

    @Override
    public void didReceivedNotification(int id, Object... args)
    {
        if (!isRunning)
            return;

        if (id == NotificationCenter.didUpdatedConnectionState)
        {
            if (ConnectionsManager.getInstance().getConnectionState() == ConnectionsManager.ConnectionStateConnected)
                ApplicationLoader.applicationHandler.post(exportMessages);
        }
        else if (id == NotificationCenter.appDidLogout)
        {
            if (currentExportDialog != null)
            {
                currentExportDialog.remove();
                currentExportDialog = null;
            }

            exportDialogNeedToExportList.clear();
            exportDialogAllList.clear();
            storage.edit().clear().apply();
            removeAllExportDialogsFiles();
            Observers.push(Observers.NotifyId.ExportDialogsListChanged);
        }
    }

    private Runnable exportMessages = () ->
    {
        if (!isRunning || (currentExportDialog != null && currentExportDialog.state == ExportDialogState.Exporting))
            return;

        if (currentExportDialog != null && !(currentExportDialog.state == ExportDialogState.Waiting || currentExportDialog.state == ExportDialogState.Exporting))
        {
            currentExportDialog.dispose();
            currentExportDialog = null;
        }

        if (currentExportDialog == null && exportDialogNeedToExportList.size() > 0)
            currentExportDialog = getExportDialog(exportDialogNeedToExportList.get(0));

        if (currentExportDialog != null)
        {
            currentExportDialog.export();
            Observers.push(Observers.NotifyId.ExportDialogsCurrentItemChanged, currentExportDialog.dialogId);
        }
    };

    public int getExportDialogsCount()
    {
        return exportDialogAllList.size();
    }

    public ExportDialog getExportDialog(int index)
    {
        return getExportDialog(exportDialogAllList.get(index));
    }

    private ExportDialog getExportDialog(String key)
    {
        if (!isRunning)
            return null;

        try
        {
            if (currentExportDialog != null && currentExportDialog.key.equals(key))
                return currentExportDialog;

            JSONObject item = new JSONObject(storage.getString(key, null));
            ExportDialogState state = ExportDialogState.find(item.optInt("state", -1));
            if (ExportDialogState.Exporting == state)
                state = ExportDialogState.Waiting;

            ExportDialog exportDialog = new ExportDialog(key, item.getLong("dialogId"));
            exportDialog.dialogType = item.optString("dialogType", StringUtils.empty);
            exportDialog.title = item.optString("title", StringUtils.empty);
            exportDialog.userId = item.optInt("userId", -1);
            exportDialog.chatId = item.optInt("chatId", -1);
            exportDialog.includePhotos = item.optBoolean("includePhotos");
            exportDialog.includeStickers = item.optBoolean("includeStickers");
            exportDialog.includeVoiceMessages = item.optBoolean("includeVoiceMessages");
            exportDialog.includeFiles = item.getBoolean("includeFiles");
            exportDialog.isExportMessagesCompleted = item.optBoolean("isExportMessagesCompleted", false);
            exportDialog.isExportFilesCompleted = item.optBoolean("isExportFilesCompleted", false);
            exportDialog.state = state;
            exportDialog.lastMessageId = item.optInt("lastMessageId", 0);
            exportDialog.date = item.optLong("date", 0);
            exportDialog.errorCode = item.optInt("errorCode", -1);
            exportDialog.errorMessage = item.optString("errorMessage", StringUtils.empty);

            return exportDialog;
        }
        catch (Exception ignored) {}
        return null;
    }

    public void showExportDialog(Context context, int userId, int chatId, long dialog_id)
    {
        if (!isRunning)
            return;

        boolean[] includePhotos = new boolean[] { false };
        boolean[] includeStickers = new boolean[] { false };
        boolean[] includeVoiceMessages = new boolean[] { false };
        boolean[] includeFiles = new boolean[] { false };
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("ExportChatOptions", R.string.ExportChatOptions));
        /*
        * builder.setMultiChoiceItems(new CharSequence[]
                {
                        LocaleController.getString("ExportChatIncludePhotos", R.string.ExportChatIncludePhotos),
                        LocaleController.getString("ExportChatIncludeStickers", R.string.ExportChatIncludeStickers),
                        LocaleController.getString("ExportChatIncludeVoiceMessages", R.string.ExportChatIncludeVoiceMessages),
                        LocaleController.getString("ExportChatIncludeFiles", R.string.ExportChatIncludeFiles)
                }, null, (dialog, which, isChecked) ->
        {
            switch (which)
            {
                case 0:
                    includePhotos[0] = isChecked;
                    break;
                case 1:
                    includeStickers[0] = isChecked;
                    break;
                case 2:
                    includeVoiceMessages[0] = isChecked;
                    break;
                case 3:
                    includeFiles[0] = isChecked;
                    break;
            }
        });
        * */
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) ->
        {
            dialog.dismiss();
            String title = Long.toString(dialog_id);
            String type = "chat";

            TLRPC.User user = MessagesController.getInstance().getUser(userId);
            if (user != null)
                title = String.format("%s %s", user.first_name, user.last_name != null ? user.last_name : StringUtils.empty);

            TLRPC.Chat chat = MessagesController.getInstance().getChat(chatId);
            if (chat != null)
            {
                title = chat.title;
                type = ChatObject.isChannel(chat) ? (chat.megagroup ? "megaGroup" : "channel") : "group";
            }

            export(title, userId, chatId, dialog_id, type, includePhotos[0],
                    includeStickers[0], includeVoiceMessages[0], includeFiles[0]);

            Toast.makeText(context, LocaleController.getString("ExportChatAddedToQueueSuccessfully",
                    R.string.ExportChatAddedToQueueSuccessfully),
                    Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> dialog.dismiss());
        builder.setCancelable(true);
        builder.show();
    }

    private void export(String title, int userId, int chatId, long dialog_id, String dialogType, boolean includePhotos,
                        boolean includeStickers, boolean includeVoiceMessages, boolean includeFiles)
    {
        ApplicationLoader.applicationHandler.post(() ->
        {
            if (!isRunning)
                return;

            try
            {
                long now = System.currentTimeMillis();
                String key = Long.toString(now);
                JSONObject item = new JSONObject();
                item.put("title", title);
                item.put("userId", userId);
                item.put("chatId", chatId);
                item.put("dialogId", dialog_id);
                item.put("dialogType", dialogType);
                item.put("includePhotos", includePhotos);
                item.put("includeStickers", includeStickers);
                item.put("includeVoiceMessages", includeVoiceMessages);
                item.put("includeFiles", includeFiles);
                item.put("date", now);
                storage.edit().putString(key, item.toString()).apply();
                exportDialogAllList.add(key);
                exportDialogNeedToExportList.add(key);
                Observers.push(Observers.NotifyId.ExportDialogsItemAdded, key);
                ApplicationLoader.applicationHandler.post(exportMessages);
            }
            catch (Exception ignored) {}
        });
    }

    public void removeExport(int index)
    {
        if (!isRunning)
            return;

        removeExport(exportDialogAllList.get(index));
    }

    public void removeExport(String key)
    {
        if (!isRunning)
            return;

        ApplicationLoader.applicationHandler.post(() ->
        {
            if (!isRunning)
                return;

            if (exportDialogAllList.contains(key))
            {
                ExportDialog item = getExportDialog(key);
                exportDialogAllList.remove(key);
                storage.edit().remove(key).apply();
                if (currentExportDialog != null && key.equals(currentExportDialog.key))
                    currentExportDialog.remove();

                if (item != null)
                {
                    try
                    {
                        File deletePath = new File(item.getExportPath());
                        FileUtils.deleteRecursive(deletePath, null);
                    }
                    catch (Exception ignored) {}
                }

                Observers.push(Observers.NotifyId.ExportDialogsItemRemoved, key);
            }
        });
    }

    private void updateExportChatLastMessageId(ExportDialog exportDialog, int lastMessageId)
    {
        try
        {
            exportDialog.lastMessageId = lastMessageId;
            JSONObject item = new JSONObject(storage.getString(exportDialog.key, null));
            item.put("lastMessageId", lastMessageId);
            storage.edit().putString(exportDialog.key, item.toString()).apply();
        }
        catch (Exception ignored) {}
    }

    private void setExportChatMessageCompleted(ExportDialog exportDialog)
    {
        try
        {
            exportDialog.isExportMessagesCompleted = true;
            JSONObject item = new JSONObject(storage.getString(exportDialog.key, null));
            item.put("isExportMessagesCompleted", true);
            storage.edit().putString(exportDialog.key, item.toString()).apply();
        }
        catch (Exception ignored) {}
    }

    private void setExportChatFilesCompleted(ExportDialog exportDialog)
    {
        try
        {
            exportDialog.isExportFilesCompleted = true;
            String key = Long.toString(exportDialog.dialogId);
            JSONObject item = new JSONObject(storage.getString(key, null));
            item.put("isExportFilesCompleted", true);
            storage.edit().putString(key, item.toString()).apply();
        }
        catch (Exception ignored) {}
    }

    private void onExportCompleted(ExportDialog exportDialog)
    {
        if (!isRunning)
            return;

        try
        {
            exportDialog.state = ExportDialogState.Completed;
            JSONObject item = new JSONObject(storage.getString(exportDialog.key, null));
            item.put("state", exportDialog.state.code);
            storage.edit().putString(exportDialog.key, item.toString()).apply();
            exportDialogNeedToExportList.remove(exportDialog.key);
            Observers.push(Observers.NotifyId.ExportDialogsItemCompleted, exportDialog.key);
            ApplicationLoader.applicationHandler.post(exportMessages);
        }
        catch (Exception ignored) {}
    }

    private void onExportFailed(ExportDialog exportDialog, int errorCode, String errorMessage)
    {
        if (!isRunning)
            return;

        try
        {
            exportDialog.state = ExportDialogState.Failed;
            exportDialog.errorCode = errorCode;
            exportDialog.errorMessage = errorMessage;
            JSONObject item = new JSONObject(storage.getString(exportDialog.key, null));
            item.put("state", exportDialog.state.code);
            item.put("errorCode", errorCode);
            item.put("errorMessage", errorMessage);
            storage.edit().putString(exportDialog.key, item.toString()).apply();
            exportDialogNeedToExportList.remove(exportDialog.key);
            Observers.push(Observers.NotifyId.ExportDialogsItemFailed, exportDialog.key);
        }
        catch (Exception ignored) {}
    }

    private void removeAllExportDialogsFiles()
    {
        try
        {
            String appName = ApplicationLoader.applicationContext.getString(R.string.AppName);
            File basePath = new File(Environment.getExternalStorageDirectory(), appName);
            File baseExportPath = new File(basePath, "ExportChats");
            FileUtils.deleteRecursive(baseExportPath, null);
        }
        catch (Exception ignored) {}
    }

    public void cleanup()
    {
        if (!isRunning)
            return;

        isRunning = false;
        ApplicationLoader.applicationHandler.removeCallbacks(exportMessages);
        ConnectionsManager.getInstance().cancelRequestsForGuid(classGuid);
        exportDialogAllList.clear();
        exportDialogNeedToExportList.clear();
        if (currentExportDialog != null)
        {
            currentExportDialog.dispose();
            currentExportDialog = null;
        }
    }
}