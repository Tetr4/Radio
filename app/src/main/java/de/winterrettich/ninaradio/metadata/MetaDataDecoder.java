package de.winterrettich.ninaradio.metadata;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see <a href="</a>http://uniqueculture.net/2010/11/stream-metadata-plain-java">Get Stream Metadata with Plain Java for Your Android App</a>
 * @see <a href="</a>http://www.smackfu.com/stuff/programming/shoutcast.html">Shoutcast Metadata Protocol</a>
 *
 */
public class MetaDataDecoder {
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;
    private static Pattern sOffsetPattern = Pattern.compile("\\r\\n(icy-metaint):\\s*(\\d+)\\r\\n");
    private static Pattern sParsePattern = Pattern.compile("^([a-zA-Z]+)='([^']*)'$");

    @NonNull
    public static Map<String, String> retrieveMetadata(URL streamUrl) throws IOException, MissingMetadataException {

        // Connect to url
        URLConnection con = streamUrl.openConnection();
        con.setConnectTimeout(CONNECTION_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);
        con.setRequestProperty("icy-metadata", "1");
        con.setRequestProperty("connection", "close");
        con.connect();

        int icyMetadataOffset = 0;
        int position = 0;
        Map<String, String> metadata = new HashMap<>();
        Map<String, List<String>> headers = con.getHeaderFields();
        InputStream stream = con.getInputStream();

        // Parse headers (and icy metadata offset)
        if (headers.containsKey("icy-metaint")) {
            // Headers are sent via HTTP

            // store as comma separated values
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                String values = TextUtils.join(", ", header.getValue());
                // only set values
                if (!values.isEmpty()) {
                    metadata.put(header.getKey(), values);
                }
            }

            // get offset
            icyMetadataOffset = Integer.parseInt(headers.get("icy-metaint").get(0));
        } else {
            // Headers are sent within a stream

            // parse header
            StringBuilder strHeaders = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                position++;
                strHeaders.append((char) c);
                if (strHeaders.length() > 5 && (strHeaders.substring((strHeaders.length() - 4), strHeaders.length()).equals("\r\n\r\n"))) {
                    // end of headers
                    break;
                }
            }

            // TODO store all metadata

            // get offset
            Matcher m = sOffsetPattern.matcher(strHeaders.toString());
            if (m.find()) {
                icyMetadataOffset = Integer.parseInt(m.group(2));
            }

        }

        // In case no metadata was sent
        if (icyMetadataOffset == 0) {
            stream.close();
            throw new MissingMetadataException();
        }

        // Read metadata
        int c;
        int icyMetadataLength = 4080; // 4080 is the max length
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // Stream position should be either at the beginning or right after headers
        while ((c = stream.read()) != -1) {
            position++;

            if (position == icyMetadataOffset + 1) {
                // Current char is length of the metadata
                icyMetadataLength = c * 16;
            } else if (position > (icyMetadataOffset + icyMetadataLength)) {
                // after metadata, stop reading
                break;
            } else if (position > icyMetadataOffset + 1) {
                // inside of metadata

                // because length is multiple of 16 the resulting unused space is padded with '\0'
                if (c != '\0') {
                    buffer.write((char) c);
                }
            }
        }
        stream.close();

        // convert bytes to string (utf-8)
        String icyMetadataString = buffer.toString();

        Map<String, String> icyMetadata = parseIcyMetadata(icyMetadataString);
        metadata.putAll(icyMetadata);

        return metadata;
    }

    private static Map<String, String> parseIcyMetadata(String icyMetadataString) {
        Map<String, String> icyMetadata = new HashMap<>();
        String[] entries = icyMetadataString.split(";");
        Matcher m;
        for (String entry : entries) {
            // e.g. StreamTitle='MARK FORSTER - BAUCH UND KOPF | Antenne Niedersachsen ';
            m = sParsePattern.matcher(entry);
            if (m.find()) {
                String key = m.group(1);
                String value = m.group(2);
                // only set values
                if (!value.isEmpty()) {
                    icyMetadata.put(key, value);
                }
            }
        }

        return icyMetadata;
    }
}