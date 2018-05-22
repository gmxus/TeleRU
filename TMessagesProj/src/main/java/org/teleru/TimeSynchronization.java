package org.teleru;

import org.json.JSONObject;
import org.teleru.utils.StringUtils;

@SuppressWarnings({"WeakerAccess", "unused"})
public class TimeSynchronization
{
    public static class Addresses
    {
        // http://www.convert-unix-time.com/api?timestamp=now&timezone=utc&returnType=json
        public static byte[] UNIX_TIME_CONVERTER = new byte[] { 104, 116, 116, 112, 58, 47, 47, 119, 119, 119, 46, 99, 111, 110, 118, 101, 114, 116, 45, 117, 110, 105, 120, 45, 116, 105, 109, 101, 46, 99, 111, 109, 47, 97, 112, 105, 63, 116, 105, 109, 101, 115, 116, 97, 109, 112, 61, 110, 111, 119, 38, 116, 105, 109, 101, 122, 111, 110, 101, 61, 117, 116, 99, 38, 114, 101, 116, 117, 114, 110, 84, 121, 112, 101, 61, 106, 115, 111, 110 };

        // http://ntp-a1.nict.go.jp/cgi-bin/json
        public static byte[] JAPAN_TIME_SERVICE = new byte[] { 104, 116, 116, 112, 58, 47, 47, 110, 116, 112, 45, 97, 49, 46, 110, 105, 99, 116, 46, 103, 111, 46, 106, 112, 47, 99, 103, 105, 45, 98, 105, 110, 47, 106, 115, 111, 110 };
    }

    public static class Parameters
    {
        public static byte[] TIMESTAMP = new byte[] { 116, 105, 109, 101, 115, 116, 97, 109, 112 }; // timestamp
        public static byte[] MILLIS_UTC = new byte[] { 109, 105, 108, 108, 105, 115, 85, 116, 99 }; // millisUtc
    }

    public static abstract class TimeService
    {
        public final String address;


        public TimeService(String address)
        {
            this.address = address;
        }

        public abstract long parseUTCTime(String data) throws Exception;
    }

    public static class UnixTimeConvertTimeService extends TimeService
    {
        public UnixTimeConvertTimeService()
        {
            super(StringUtils.getUTFString(Addresses.UNIX_TIME_CONVERTER));
        }

        @Override
        public long parseUTCTime(String data) throws Exception
        {
            long utcTime = new JSONObject(data).getLong(StringUtils.getUTFString(Parameters.TIMESTAMP));
            return utcTime * 1000;
        }
    }

    public static class NICTTimeService extends TimeService
    {
        public NICTTimeService()
        {
            super(StringUtils.getUTFString(Addresses.JAPAN_TIME_SERVICE));
        }

        @Override
        public long parseUTCTime(String data) throws Exception
        {
            double st = new JSONObject(data).getDouble("st");
            return ((long)(st * 1000));
        }
    }

    public static class CustomTimeService extends TimeService
    {
        public CustomTimeService(String address)
        {
            super(address);
        }

        @Override
        public long parseUTCTime(String data) throws Exception
        {
            return new JSONObject(data).getLong(StringUtils.getUTFString(Parameters.MILLIS_UTC));
        }
    }
}