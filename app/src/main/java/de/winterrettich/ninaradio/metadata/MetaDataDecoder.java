package de.winterrettich.ninaradio.metadata;

import android.support.annotation.NonNull;

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
 */
public class MetaDataDecoder {

    protected URL streamUrl;
    private static Pattern sOffsetPattern = Pattern.compile("\\r\\n(icy-metaint):\\s*(.*)\\r\\n");
    private static Pattern sParsePattern = Pattern.compile("^([a-zA-Z]+)='([^']*)'$");

    public MetaDataDecoder(URL streamUrl) {
        setStreamUrl(streamUrl);
    }

    @NonNull
    public Map<String, String> retrieveMetadata() throws IOException, MissingDataException {
        URLConnection con = streamUrl.openConnection();
        con.setRequestProperty("Icy-MetaData", "1");
        con.setRequestProperty("Connection", "close");
        con.connect();

        int metaDataOffset = 0;
        Map<String, List<String>> headers = con.getHeaderFields();
        InputStream stream = con.getInputStream();

        if (headers.containsKey("icy-metaint")) {
            // Headers are sent via HTTP
            metaDataOffset = Integer.parseInt(headers.get("icy-metaint").get(0));
        } else {
            // Headers are sent within a stream
            StringBuilder strHeaders = new StringBuilder();
            char c;
            while ((c = (char) stream.read()) != -1) {
                strHeaders.append(c);
                if (strHeaders.length() > 5 && (strHeaders.substring((strHeaders.length() - 4), strHeaders.length()).equals("\r\n\r\n"))) {
                    // end of headers
                    break;
                }
            }

            // Match headers to get metadata offset within a stream
            Matcher m = sOffsetPattern.matcher(strHeaders.toString());
            if (m.find()) {
                metaDataOffset = Integer.parseInt(m.group(2));
            }
        }

        // In case no data was sent
        if (metaDataOffset == 0) {
            // TODO try with?
            stream.close();
            throw new MissingDataException();
        }

        // Read metadata
        int b;
        int count = 0;
        int metaDataLength = 4080; // 4080 is the max length
        boolean inData;
        StringBuilder metaData = new StringBuilder();
        // Stream position should be either at the beginning or right after headers
        while ((b = stream.read()) != -1) {
            count++;

            // Length of the metadata
            if (count == metaDataOffset + 1) {
                metaDataLength = b * 16;
            }

            inData = (count > metaDataOffset + 1 && count < (metaDataOffset + metaDataLength));

            if (inData) {
                if (b != 0) {
                    metaData.append((char) b);
                }
            }
            if (count > (metaDataOffset + metaDataLength)) {
                break;
            }

        }

        // Close
        stream.close();

        return parseMetadata(metaData.toString());
    }

    public void setStreamUrl(URL streamUrl) {
        this.streamUrl = streamUrl;
    }

    private Map<String, String> parseMetadata(String metaString) {
        Map<String, String> metadata = new HashMap<>();
        String[] metaParts = metaString.split(";");
        Matcher m;
        for (String metaPart : metaParts) {
            m = sParsePattern.matcher(metaPart);
            if (m.find()) {
                metadata.put(m.group(1), m.group(2));
            }
        }

        return metadata;
    }
}