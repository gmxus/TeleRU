package org.teleru;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Base64;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.teleru.utils.AESEncryption;
import org.teleru.utils.Base64Utils;
import org.teleru.utils.RsaEncryption;
import org.teleru.utils.StringUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public class NetworkManager
{
    public static final String TAG = NetworkManager.class.getName();

    public enum RequestMethod
    {
        Get("GET"),
        Post("POST");

        public final String name;


        RequestMethod(String name)
        {
            this.name = name;
        }
    }

    public static class RequestException extends Exception
    {
        public final int code;


        RequestException(int code, String message)
        {
            super(String.format(Locale.US, "[%d]: %s", code, message));
            this.code = code;
        }
    }

    public interface RequestCallback
    {
        void onResponse(RequestException error, JSONObject result);
    }

    public static String sendRequestSync(String address, String params, String method, int timeout) throws RequestException
    {
        try
        {
            String result;
            URL obj = new URL(address);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setConnectTimeout(timeout);
            con.setRequestMethod(method);
            con.setRequestProperty("Connection", "Keep-Alive");

            byte[] data = !TextUtils.isEmpty(params) ? StringUtils.getUTFBytes(params) : new byte[0];
            con.setRequestProperty("Content-Length", Integer.toString(data.length));
            if (data.length > 0)
            {
                OutputStream outputStream = con.getOutputStream();
                outputStream.write(data);
                outputStream.flush();
                outputStream.close();
            }

            int responseCode = con.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK)
            {
                InputStream inputStream = con.getInputStream();
                ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[2048];
                int read;

                while ((read = inputStream.read(buffer, 0, buffer.length)) > 0)
                    byteArrayInputStream.write(buffer, 0, read);

                byteArrayInputStream.flush();
                result = new String(byteArrayInputStream.toByteArray(), "utf-8");
                byteArrayInputStream.close();
                inputStream.close();
            }
            else
                throw new RequestException(responseCode, "InvalidResponse");

            con.disconnect();
            return result;
        }
        catch (RequestException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new RequestException(-1, ex.getMessage());
        }
    }

    private static final NetworkManager instance = new NetworkManager();

    public static NetworkManager getInstance()
    {
        return instance;
    }

    private ConnectivityManager connectivityManager;
    private HandlerThread handlerThread;
    private Handler handler;
    private AESEncryption aesEncryption;
    private boolean isConnectedToNetwork = false;
    private boolean isAvailable = false;
    private boolean isHandShaking = false;
    private boolean isRunning = false;

    public boolean isConnected()
    {
        return isConnectedToNetwork && isAvailable;
    }

    private NetworkManager()
    {
        connectivityManager = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void init()
    {
        if (isRunning)
            return;

        isRunning = true;
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        ApplicationLoader.applicationContext.registerReceiver(netActivityReceiver, intentFilter);
        ApplicationLoader.applicationHandler.post(checkNetwork);
    }

    private BroadcastReceiver netActivityReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            ApplicationLoader.applicationHandler.post(checkNetwork);
        }
    };

    private Runnable checkNetwork = () ->
    {
        if (!isRunning)
            return;

        boolean lastConnectedToNetwork = isConnectedToNetwork;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        isConnectedToNetwork = networkInfo != null && (networkInfo.isConnectedOrConnecting());
        if (lastConnectedToNetwork != isConnectedToNetwork)
        {
            Observers.pushAsync(Observers.NotifyId.ConnectionChanged);
            handShake();
        }
    };

    private void handShake()
    {
        if (!isConnectedToNetwork || isAvailable || isHandShaking)
            return;

        isHandShaking = true;
        handler.post(() ->
        {
            try
            {
                AESEncryption aes = new AESEncryption();
                String response = sendRequestSync(BuildParams.getServiceAddress(),
                        String.format(Locale.US, "beginAuth=&tourId=%s", ApplicationController.getInstance().getId()),
                        RequestMethod.Post.name, BuildParams.DEFAULT_CONNECTION_TIME_OUT);

                JSONObject data = new JSONObject(StringUtils.getUTFString(Base64Utils.decode(response)));
                RsaEncryption.RsaPublicEncryption rsa = new RsaEncryption.RsaPublicEncryption(RsaEncryption.convertPublicKeyFromJson(data));
                data = new JSONObject();
                data.put("key", Base64.encodeToString(aes.getKey(), Base64.NO_WRAP | Base64.URL_SAFE));
                data.put("iv", Base64.encodeToString(aes.getIv(), Base64.NO_WRAP | Base64.URL_SAFE));
                byte[] encoded = rsa.encrypt(StringUtils.getUTFBytes(data.toString()));

                sendRequestSync(BuildParams.getServiceAddress(),
                        String.format("endAuth=%s&tourId=%s", Base64Utils.encode(encoded), ApplicationController.getInstance().getId()),
                        RequestMethod.Post.name, BuildParams.DEFAULT_CONNECTION_TIME_OUT);

                aesEncryption = aes;
                isAvailable = true;
                isHandShaking = false;
                Observers.pushAsync(Observers.NotifyId.ConnectionChanged);
            }
            catch (Exception ignored)
            {
                isHandShaking = false;
                handler.postDelayed(this::handShake, 2000);
            }
        });
    }

    public void sendRequest(JSONObject params, RequestCallback callback)
    {
        sendRequest(params, BuildParams.DEFAULT_CONNECTION_TIME_OUT, callback);
    }

    public void sendRequest(final JSONObject params, final int timeout, final RequestCallback callback)
    {
        if (!isRunning || !isAvailable)
        {
            callback.onResponse(new RequestException(-1, StringUtils.empty), null);
            return;
        }

        handler.post(() ->
        {
            try
            {
                JSONObject result = sendRequestSync(params, timeout);
                ApplicationLoader.applicationHandler.post(() -> callback.onResponse(null, result));
            }
            catch (RequestException exception)
            {
                ApplicationLoader.applicationHandler.post(() -> callback.onResponse(exception, null));
            }
        });
    }

    public JSONObject sendRequestSync(final JSONObject params, final int timeout) throws RequestException
    {
        if (!isRunning || !isAvailable)
            throw new RequestException(-1, StringUtils.empty);

        JSONObject result;
        RequestException exception;

        try
        {
            byte[] encoded = aesEncryption.encrypt(StringUtils.getUTFBytes(params.toString()));
            String response = sendRequestSync(BuildParams.getServiceAddress(),
                    String.format("request=%s&tourId=%s",
                            Base64.encodeToString(encoded, Base64.NO_WRAP | Base64.URL_SAFE),
                            ApplicationController.getInstance().getId()),
                    RequestMethod.Post.name, timeout);
            byte[] decoded = aesEncryption.decrypt(Base64.decode(response, Base64.NO_WRAP | Base64.URL_SAFE));
            result = new JSONObject(StringUtils.getUTFString(decoded));
            exception = null;
        }
        catch (Exception ex)
        {
            result = null;
            if (ex instanceof RequestException)
            {
                RequestException requestException = (RequestException)ex;
                exception = requestException;
                if (requestException.code == 401 || requestException.code == 500)
                {
                    isAvailable = false;
                    aesEncryption = null;
                    Observers.pushAsync(Observers.NotifyId.ConnectionChanged);
                    handShake();
                }
            }
            else
                exception = new RequestException(-1, StringUtils.empty);
        }

        if (exception != null)
            throw exception;

        return result;
    }

    public void cleanup()
    {
        if (!isRunning)
            return;

        isRunning = false;
        isAvailable = false;
        isHandShaking = true;
        aesEncryption = null;
        handlerThread.quit();
        handlerThread.interrupt();
        ApplicationLoader.applicationContext.unregisterReceiver(netActivityReceiver);
    }
}
